package com.zelin.p2pclient

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.util.Log
import android.widget.Toast
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_PEERS_DISCOVERY
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_PEERS_REQUEST
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_REQUEST_CONNECTION_INFO
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_REQUEST_CONNECT_DEVICE

object P2pClientManager {

    private var mIsRequestingConnectionInfo = false
    private var mWifiP2pManager: WifiP2pManager? = null
    private var mWifiP2pChannel: WifiP2pManager.Channel? = null
    private const val TAG = "peerClient/P2pClientManager"

    //郑泽霖的redmi k40s的WIFI mac地址
    const val DEVICE_ADDRESS_MAC_REDMI_K40S = "42:68:bd:cb:55:6d"

    //对外
    var mRemoteDeviceHostAddress: String? = null
    var mIsP2pConnected: Boolean = false


    fun init(context: Context) {
        Log.i(TAG, "init()")
        mWifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        mWifiP2pChannel = mWifiP2pManager?.initialize(context, context.mainLooper, null)
    }

    fun deInit() {
        Log.i(TAG, "deInit()")
        mWifiP2pManager = null
        mWifiP2pChannel = null
    }

    //开启设备发现
    //服务端和客户端都需要开启设备发现
    @SuppressLint("MissingPermission")
    fun beginPeersDiscovery(context: Activity) {
        Log.i(TAG, "beginPeersDiscovery()")
        if (
            !PermissionManager.isHavePermissions(context, REQUEST_CODE_PEERS_DISCOVERY)
        ) {
            return
        }
        Log.i(TAG, "beginPeersDiscovery() call WifiP2pManager.discoverPeers()")
        Toast.makeText(context, "开始设备发现", Toast.LENGTH_SHORT)
            .show()
        if (mWifiP2pChannel != null) {
            mWifiP2pManager?.discoverPeers(mWifiP2pChannel, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun beginPeersRequest(context: Activity) {
        Log.i(TAG, "beginPeersRequest()")
        if (
            !PermissionManager.isHavePermissions(context, REQUEST_CODE_PEERS_REQUEST)
        ) {
            return
        }
        mWifiP2pChannel?.let {
            mWifiP2pManager?.requestPeers(
                it
            ) { peers ->
                Log.i(TAG, "requestPeers() peers: $peers")
            }
        }
    }

    //客户端连接设备，服务端不去连接设备
    //实际上谁连谁都是可以的
    //远程设备
    //wifiP2pDevice:
    // Device: Redmi K40S
    //deviceAddress: 02:00:00:00:00:00
    //primary type: 10-0050F204-5
    //secondary type: null
    //wps: 0
    //grpcapab: 0
    //devcapab: 0
    //status: 3
    //wfdInfo: null
    //vendorElements: null
    //************************************************************
    //设置界面显示的mac地址：
    //f8:ab:82:d9:3a:1e
    //设置界面显示的蓝牙地址：
    //f8:ab:82:d8:01:9e
    //************************************************************
    //p2p扫描到的远程设备信息
    //Device: Redmi K40S
    //deviceAddress: 42:68:bd:cb:55:6d
    //primary type: 10-0050F204-5
    //secondary type: null
    //wps: 392
    //grpcapab: 43
    //devcapab: 37
    //status: 3
    //wfdInfo: WFD enabled: trueWFD DeviceInfo: 0
    //WFD CtrlPort: 0
    //WFD MaxThroughput: 0
    //WFD R2 DeviceInfo: -1
    //vendorElements: null
    @SuppressLint("MissingPermission")
    fun connectDevice(context: Activity, deviceAddress: String) {
        Log.i(TAG, "connectDevice()")
        if (
            !PermissionManager.isHavePermissions(context, REQUEST_CODE_REQUEST_CONNECT_DEVICE)
        ) {
            return
        }
        val config = WifiP2pConfig()
        //目标设备的MAC地址
        config.deviceAddress = deviceAddress;
        mWifiP2pChannel?.let {
            mWifiP2pManager?.connect(it, config, object : ActionListener {
                override fun onSuccess() {
                    Log.i(TAG, "connectDevice() onSuccess() 连接成功")
                    Toast.makeText(context, "连接成功", Toast.LENGTH_SHORT).show()
                    mIsP2pConnected = true
                    //请求Group信息
                    beginRequestConnectionInfo(context)
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "connectDevice() onFailure() 连接失败 reason: $reason")
                    mIsP2pConnected = false
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun beginRequestConnectionInfo(activity: Activity) {
        Log.i(TAG, "beginRequestConnectionInfo() ")
        if(mIsRequestingConnectionInfo){
            return
        }
        mIsRequestingConnectionInfo=true
        if(mRemoteDeviceHostAddress!=null){
            return;
        }
        // TODO:
        if (
            !PermissionManager.isHavePermissions(activity, REQUEST_CODE_REQUEST_CONNECTION_INFO)
        ) {
            mIsRequestingConnectionInfo=false
            return
        }
        Toast.makeText(activity, "开始查询Group信息", Toast.LENGTH_SHORT).show()
        mWifiP2pChannel?.let {
            mWifiP2pManager?.requestConnectionInfo(it) { wifiP2pInfo: WifiP2pInfo? ->
                //beginRequestConnectionInfo() wifiP2pInfo：groupFormed: true isGroupOwner: false groupOwnerAddress: /192.168.49.1
                Log.i(TAG, "beginRequestConnectionInfo() wifiP2pInfo：$wifiP2pInfo")
                if (wifiP2pInfo == null) {
                    Log.e(TAG, "beginRequestConnectionInfo() wifiP2pInfo is null! ")
                    mIsP2pConnected = false
                    mIsRequestingConnectionInfo=false
                    return@requestConnectionInfo
                }
                if (!wifiP2pInfo.groupFormed) {
                    Log.e(TAG, "beginRequestConnectionInfo() wifiP2pInfo.groupFormed is false! ")
                    mIsP2pConnected = false
                    mIsRequestingConnectionInfo=false
                    return@requestConnectionInfo
                }
                val address: String? = wifiP2pInfo.groupOwnerAddress.hostAddress
                Log.i(TAG, "beginRequestConnectionInfo() groupOwnerAddress：${wifiP2pInfo.groupOwnerAddress}")

                if(address==null){
                    mIsP2pConnected = false
                    mIsRequestingConnectionInfo=false
                    return@requestConnectionInfo
                }
                //beginRequestConnectionInfo() 服务端IP地址：192.168.49.1
                Log.i(TAG, "beginRequestConnectionInfo() 服务端IP地址：$address")
                mRemoteDeviceHostAddress = address
                mIsRequestingConnectionInfo=false
                Toast.makeText(activity, "wifiP2p连接成功，服务器地址：$address", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}