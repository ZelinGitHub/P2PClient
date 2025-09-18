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

    private var mWifiP2pManager: WifiP2pManager? = null
    private var mWifiP2pChannel: WifiP2pManager.Channel? = null
    private const val TAG = "peerClient/P2pClientManager"

    //郑泽霖的redmi k40s的WIFI mac地址
    const val DEVICE_ADDRESS_MAC_REDMI_K40S = "42:68:bd:cb:55:6d"
//    const val DEVICE_ADDRESS_MAC_REDMI_K40S = "02:00:00:00:00:00"

    //服务端ip地址
    var mRemoteDeviceHostAddress: String? = null
    //p2p是否已经连接
    var mIsP2pConnected: Boolean = false
    //避免重复请求group连接信息
     var mIsRequestingConnectionInfo = false


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
            Log.e(TAG, "beginPeersDiscovery() isHavePermissions false")
            return
        }
        Log.i(TAG, "beginPeersDiscovery() call WifiP2pManager.discoverPeers()")
        if (mWifiP2pChannel != null) {
            //设备发现
            mWifiP2pManager?.discoverPeers(mWifiP2pChannel, null)
        }
    }

    //查询远程设备列表
    //实际上我们没有用到这个列表
    @SuppressLint("MissingPermission")
    fun beginPeersRequest(context: Activity) {
        Log.i(TAG, "beginPeersRequest()")
        if (
            !PermissionManager.isHavePermissions(context, REQUEST_CODE_PEERS_REQUEST)
        ) {
            Log.e(TAG, "beginPeersRequest() isHavePermissions false")
            return
        }
        mWifiP2pChannel?.let {
            //调用requestPeers()方法，获取远程发现设备的列表
            mWifiP2pManager?.requestPeers(
                it
            ) {
                //参数是WifiP2pDeviceList对象，包含WifiP2pDevice对象的列表
                    peers ->
                Log.i(TAG, "requestPeers() peers: $peers")
            }
        }
    }

    //客户端连接设备，服务端不去连接设备。实际上谁连谁都是可以的
    //远程设备
    @SuppressLint("MissingPermission")
    fun connectDevice(context: Activity, deviceAddress: String) {
        Log.i(TAG, "connectDevice()")
        if (
            !PermissionManager.isHavePermissions(context, REQUEST_CODE_REQUEST_CONNECT_DEVICE)
        ) {
            Log.e(TAG, "connectDevice() isHavePermissions false")
            return
        }
        val config = WifiP2pConfig()
        //目标设备的MAC地址
        config.deviceAddress = deviceAddress;
        Log.i(TAG, "connectDevice() call WifiP2pManager.connect() deviceAddress: ${config.deviceAddress}")
        mWifiP2pChannel?.let {
            //连接设备
            mWifiP2pManager?.connect(it, config, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun beginRequestConnectionInfo(activity: Activity) {
        Log.i(TAG, "beginRequestConnectionInfo() ")
        //防止重复请求连接信息
        if (mIsRequestingConnectionInfo) {
            return
        }
        mIsRequestingConnectionInfo = true
        if (mRemoteDeviceHostAddress != null) {
            return
        }
        if (
            //没有权限，请求权限
            !PermissionManager.isHavePermissions(activity, REQUEST_CODE_REQUEST_CONNECTION_INFO)
        ) {
            //
            mIsRequestingConnectionInfo = false
            return
        }
        Toast.makeText(activity, "开始查询Group信息", Toast.LENGTH_SHORT).show()
        mWifiP2pChannel?.let {
            mWifiP2pManager?.requestConnectionInfo(it) { wifiP2pInfo: WifiP2pInfo? ->
                //beginRequestConnectionInfo() wifiP2pInfo：groupFormed: true isGroupOwner: false groupOwnerAddress: /192.168.49.1
                Log.i(TAG, "beginRequestConnectionInfo() wifiP2pInfo：$wifiP2pInfo")
                if (wifiP2pInfo == null) {
                    Log.e(TAG, "beginRequestConnectionInfo() wifiP2pInfo is null! ")
                    //
                    mIsP2pConnected = false
                    //
                    mIsRequestingConnectionInfo = false
                    return@requestConnectionInfo
                }
                if (!wifiP2pInfo.groupFormed) {
                    Log.e(TAG, "beginRequestConnectionInfo() wifiP2pInfo.groupFormed is false! ")
                    //
                    mIsP2pConnected = false
                    //
                    mIsRequestingConnectionInfo = false
                    return@requestConnectionInfo
                }
                val address: String? = wifiP2pInfo.groupOwnerAddress.hostAddress
                Log.i(
                    TAG,
                    "beginRequestConnectionInfo() groupOwnerAddress：${wifiP2pInfo.groupOwnerAddress}"
                )

                if (address == null) {
                    //
                    mIsP2pConnected = false
                    //
                    mIsRequestingConnectionInfo = false
                    return@requestConnectionInfo
                }
                //beginRequestConnectionInfo() 服务端IP地址：192.168.49.1
                Log.i(TAG, "beginRequestConnectionInfo() 服务端IP地址：$address")
                //远程设备的ip地址
                //GroupOwner的ip地址
                mRemoteDeviceHostAddress = address
                Toast.makeText(activity, "wifiP2p连接成功，服务器地址：$address", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}