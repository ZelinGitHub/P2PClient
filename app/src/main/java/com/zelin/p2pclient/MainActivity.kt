package com.zelin.p2pclient

import android.graphics.Bitmap
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager.EXTRA_DISCOVERY_STATE
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_INFO
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.zelin.p2pclient.FileManager.getImageFilePath
import com.zelin.p2pclient.P2pClientManager.mIsP2pConnected
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_CONNECT_SEND_DATA
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_LOAD_BITMAP
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_PEERS_DISCOVERY
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_PEERS_REQUEST
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_REQUEST_CONNECTION_INFO
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_REQUEST_CONNECT_DEVICE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks


class MainActivity : ComponentActivity(), PermissionCallbacks {

    private var btn_send_data: Button? = null
    private var btn_connect_server: Button? = null
    private var iv: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")
        setContentView(R.layout.activity_main)
        iv = findViewById(R.id.iv)
        btn_send_data = findViewById(R.id.btn_send_data)
        btn_connect_server = findViewById(R.id.btn_connect_server)

        loadBitmap()
        //点击进行socket连接，并发送数据
        btn_send_data?.setOnClickListener {
            Log.i(TAG, "onClick() click btn_send_data")
            //如果目标设备的IP地址不为null，进行sock连接，然后传输数据
            connectSocket()
        }

        btn_connect_server?.setOnClickListener {
            //连接远程设备
            connectRemoteDevice()
        }
        registerListeners()
        //初始化WifiP2pManager
        P2pClientManager.init(this);
        //设备发现
        beginPeersDiscovery()
    }

    private fun loadBitmap() {
        if (
            !PermissionManager.isHavePermissions(this, REQUEST_CODE_LOAD_BITMAP)
        ) {
            return
        }
        //开启子线程
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filePath = getImageFilePath()
                val bitmap: Bitmap = FileManager.readBitmapFromFile(filePath) ?: return@launch
                //切换到主线程
                withContext(Dispatchers.Main) {
                    iv?.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume()")

    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy()")
        P2pClientManager.mIsP2pConnected = false
        P2pClientManager.mIsRequestingConnectionInfo = false
        P2pClientManager.mRemoteDeviceHostAddress = null
        P2pClientManager.deInit();
        unRegisterListeners()
    }


    private fun registerListeners() {


        //注册p2p事件广播接收者
        P2pBroadcastReceiver.registerP2pReceiver(this)


        //广播1：
        //设备发现开始，会收到这个广播。
        //设备发现结束，也会收到这个广播。
        P2pBroadcastReceiver.mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION = {
            Log.i(TAG, "mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION 设备发现状态改变")
            it?.let {
                if (it.getInt(EXTRA_DISCOVERY_STATE) == WIFI_P2P_DISCOVERY_STOPPED) {
                    Log.i(TAG, "mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION 设备发现结束")
                    if (P2pClientManager.mIsP2pConnected) {
                        Log.e(
                            TAG,
                            "mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION 设备已连接，不会开启设备发现"
                        )
                        return@let
                    }
                    //设备发现
                    beginPeersDiscovery()

                } else if (it.getInt(EXTRA_DISCOVERY_STATE) == WIFI_P2P_DISCOVERY_STARTED) {
                    Log.i(TAG, "mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION 设备发现开始")
                }
            }
        }

        //广播2：
        //指示可用的远程p2p设备列表发生了变化的广播。这个广播在远程p2p设备们被发现、丢失、更新的时候会收到。
        P2pBroadcastReceiver.mOnWIFI_P2P_PEERS_CHANGED_ACTION = PEERS_CHANGED_ACTION@{
            Log.i(TAG, "mOnWIFI_P2P_PEERS_CHANGED_ACTION peers数据改变")
            if (P2pClientManager.mIsP2pConnected) {
                Log.e(TAG, "mOnWIFI_P2P_PEERS_CHANGED_ACTION 设备已连接，不会再查询peers并连接设备")
                return@PEERS_CHANGED_ACTION
            }
            //这里可以使用EXTRA_P2P_DEVICE_LIST得到p2p设备列表，也可以调用requestPeers方法请求p2p设备列表
            // TODO:  我们直接写死远程设备的MAC地址，不使用查询结果
            beginPeersRequest()
        }

        //广播3：
        //连接状态变化。之前不知道为什么注释掉内容了
        //连接成功之后，请求设备信息，得到服务端IP地址。最后利用这个IP地址进行Socket连接
        P2pBroadcastReceiver.mOnWIFI_P2P_CONNECTION_CHANGED_ACTION = ConnectionChanged@{
            Log.i(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION 连接状态改变")
            val wifiP2pInfo: WifiP2pInfo? =
                intent.getParcelableExtra<WifiP2pInfo>(EXTRA_WIFI_P2P_INFO)
            Log.i(
                TAG,
                "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION 连接状态改变 wifiP2pInfo: $wifiP2pInfo"
            )
            if (wifiP2pInfo == null) {
                //1
                P2pClientManager.mIsP2pConnected = false
                return@ConnectionChanged
            }
            //确定group是否成功构建。Indicates if a p2p group has been successfully formed
            if (!wifiP2pInfo.groupFormed) {
                //2
                P2pClientManager.mIsP2pConnected = false
                Log.e(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION wifiP2pInfo.groupFormed is false")
                return@ConnectionChanged
            }
            if (wifiP2pInfo.groupOwnerAddress.hostAddress == null) {
                //3
                P2pClientManager.mIsP2pConnected = false
                Log.e(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION wifiP2pInfo.groupFormed is false")
                return@ConnectionChanged
            }
            P2pClientManager.mIsP2pConnected = true
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
            //请求Group信息
            beginRequestConnectionInfo()
        }
    }


    private fun unRegisterListeners() {
        P2pBroadcastReceiver.mOnWIFI_P2P_PEERS_CHANGED_ACTION = null
        P2pBroadcastReceiver.mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION = null
        P2pBroadcastReceiver.mOnWIFI_P2P_CONNECTION_CHANGED_ACTION = null
        P2pBroadcastReceiver.unRegisterP2pReceiver(this)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult()")
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.i(TAG, "onPermissionsGranted() requestCode: $requestCode")
        when (requestCode) {

            //有开启p2p设备发现的权限
            REQUEST_CODE_PEERS_DISCOVERY -> {
                //再次尝试开启p2p设备发现
                beginPeersDiscovery()
            }
            //有得到p2p设备列表的权限
            REQUEST_CODE_PEERS_REQUEST -> {
                //再次获取设备列表
                beginPeersRequest()
            }
            //有连接p2p设备的权限
            REQUEST_CODE_REQUEST_CONNECT_DEVICE -> {
                //再次连接p2p设备
                connectRemoteDevice()
            }
            //有获取Group连接信息的权限
            REQUEST_CODE_REQUEST_CONNECTION_INFO -> {
                //再次尝试请求获取Group连接信息
                beginRequestConnectionInfo()
            }

            REQUEST_CODE_LOAD_BITMAP -> {
                extracted()
            }
            REQUEST_CODE_CONNECT_SEND_DATA -> {
                connectSocket()
            }
        }
    }

    private fun connectSocket() {
        P2pClientManager.mRemoteDeviceHostAddress?.apply {
            SocketManager.connectAndSendData(
                this@MainActivity,
                this
            )
        } ?: {
            Log.e(TAG, "RemoteDeviceHostAddress is null!")
            Toast.makeText(this, "RemoteDeviceHostAddress is null!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extracted() {
        loadBitmap()
    }

    private fun beginRequestConnectionInfo() {
        P2pClientManager.beginRequestConnectionInfo(this)
    }

    private fun connectRemoteDevice() {
        P2pClientManager.connectDevice(this, P2pClientManager.DEVICE_ADDRESS_MAC_REDMI_K40S)
    }

    private fun beginPeersRequest() {
        P2pClientManager.beginPeersRequest(this)
    }


    private fun beginPeersDiscovery() {
        P2pClientManager.beginPeersDiscovery(this)
    }



    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.e(TAG, "onPermissionsDenied() 用户拒绝了权限 requestCode: $requestCode")
        finish()
    }


    companion object {
        private const val TAG = "peerClient/MainActivity"
    }
}
