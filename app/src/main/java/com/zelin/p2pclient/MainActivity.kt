package com.zelin.p2pclient

import android.graphics.Bitmap
import android.net.wifi.p2p.WifiP2pManager.EXTRA_DISCOVERY_STATE
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.zelin.p2pclient.FileManager.getImageFilePath
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
    private var iv: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")
        setContentView(R.layout.activity_main)
        iv = findViewById(R.id.iv)
        btn_send_data = findViewById(R.id.btn_send_data)

        loadBitmap()
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
        registerListeners()
        P2pClientManager.init(this);
        P2pClientManager.beginPeersDiscovery(this)
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause()")
        P2pClientManager.mIsP2pConnected = false
        P2pClientManager.mRemoteDeviceHostAddress = null
        P2pClientManager.deInit();
        unRegisterListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy()")

    }


    private fun registerListeners() {
        btn_send_data?.setOnClickListener {
            Log.i(TAG, "onClick() click btn_send_data")
            P2pClientManager.mRemoteDeviceHostAddress?.apply {
                SocketManager.connectAndSendData(
                    this@MainActivity,
                    this
                )
            } ?: { Log.e(TAG, "RemoteDeviceHostAddress is null!") }
        }

        P2pBroadcastReceiver.registerP2pReceiver(this)
        P2pBroadcastReceiver.mOnWIFI_P2P_PEERS_CHANGED_ACTION = PEERS_CHANGED_ACTION@{
            Log.i(TAG, "mOnWIFI_P2P_PEERS_CHANGED_ACTION peers数据改变")
            if (P2pClientManager.mIsP2pConnected) {
                Log.e(TAG, "mOnWIFI_P2P_PEERS_CHANGED_ACTION 设备已连接，不会再查询peers并连接设备")
                return@PEERS_CHANGED_ACTION
            }
            //我们直接写死远程设备的MAC地址，不使用查询结果
            P2pClientManager.beginPeersRequest(this)
            P2pClientManager.connectDevice(this, P2pClientManager.DEVICE_ADDRESS_MAC_REDMI_K40S)
        }
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
                    P2pClientManager.beginPeersDiscovery(this)

                } else if (it.getInt(EXTRA_DISCOVERY_STATE) == WIFI_P2P_DISCOVERY_STARTED) {
                    Log.i(TAG, "mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION 设备发现开始")
                }
            }
        }
        P2pBroadcastReceiver.mOnWIFI_P2P_CONNECTION_CHANGED_ACTION = ConnectionChanged@{
            Log.i(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION 连接状态改变")
            //            val wifiP2pInfo: WifiP2pInfo? =
            //                intent.getParcelableExtra<WifiP2pInfo>(EXTRA_WIFI_P2P_INFO)
            //            Log.i(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION 连接状态改变 wifiP2pInfo: $wifiP2pInfo")
            //            if(wifiP2pInfo==null){
            //                P2pClientManager.mIsConnected = false
            //                return@ConnectionChanged
            //            }
            //            if (!wifiP2pInfo.groupFormed) {
            //                P2pClientManager.mIsConnected = false
            //                Log.e(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION wifiP2pInfo.groupFormed is false")
            //                return@ConnectionChanged
            //            }
//            if (!P2pClientManager.mIsConnected) {
//                Log.e(TAG, "mOnWIFI_P2P_CONNECTION_CHANGED_ACTION mIsConnected is false")
//                return@ConnectionChanged
//            }
//            //请求Group信息
//            P2pClientManager.beginRequestConnectionInfo(this)
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
            REQUEST_CODE_PEERS_REQUEST -> {
                P2pClientManager.beginPeersRequest(this)
            }

            REQUEST_CODE_PEERS_DISCOVERY -> {
                P2pClientManager.beginPeersDiscovery(this)
            }

            REQUEST_CODE_REQUEST_CONNECT_DEVICE -> {
                P2pClientManager.connectDevice(this, P2pClientManager.DEVICE_ADDRESS_MAC_REDMI_K40S)
            }

            REQUEST_CODE_REQUEST_CONNECTION_INFO -> {
                P2pClientManager.beginRequestConnectionInfo(this)
            }

            REQUEST_CODE_LOAD_BITMAP -> {
                loadBitmap()
            }

            REQUEST_CODE_CONNECT_SEND_DATA -> {
                P2pClientManager.mRemoteDeviceHostAddress?.apply {
                    SocketManager.connectAndSendData(
                        this@MainActivity,
                        this
                    )
                } ?: { Log.e(TAG, "RemoteDeviceHostAddress is null!") }
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.e(TAG, "onPermissionsDenied() 用户拒绝了权限 requestCode: $requestCode")
        finish()
    }


    companion object {
        private const val TAG = "peerClient/MainActivity"
    }
}
