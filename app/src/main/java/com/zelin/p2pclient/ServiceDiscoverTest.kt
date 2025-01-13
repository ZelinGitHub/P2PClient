package com.zelin.p2pclient

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest
import android.util.Log
import androidx.core.app.ActivityCompat


object ServiceDiscoverTest {
    private val mBuddies: MutableMap<String, String> = HashMap()

    private const val SERVER_PORT = 3288
    private const val TAG = "peerClient/ServiceDiscoverTest"


    private var mWifiP2pManager: WifiP2pManager? = null
    private var mWifiP2pChannel: WifiP2pManager.Channel? = null


    fun init(context: Context) {
        mWifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        mWifiP2pChannel = mWifiP2pManager?.initialize(context, context.mainLooper, null)
    }


    //开始本地服务注册
    private fun startServiceRegistration(context: Context) {


        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (mWifiP2pChannel != null) {
            val record: MutableMap<String, String> = HashMap()
            record["listenport"] = SERVER_PORT.toString()
            record["buddyname"] = "John Doe" + (Math.random() * 1000).toInt()
            record["available"] = "visible"
            val serviceInfo: WifiP2pDnsSdServiceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record)
            mWifiP2pManager?.addLocalService(mWifiP2pChannel, serviceInfo, object : ActionListener {
                override fun onSuccess() {
                }

                override fun onFailure(arg0: Int) {
                }
            })
        }

    }

    private fun createAndAddP2pServiceRequest() {
        if (mWifiP2pChannel != null) {
            val serviceRequest: WifiP2pServiceRequest = WifiP2pDnsSdServiceRequest.newInstance()
            mWifiP2pManager?.addServiceRequest(mWifiP2pChannel,
                serviceRequest,
                object : ActionListener {
                    override fun onSuccess() {
                        // Success!
                    }

                    override fun onFailure(code: Int) {
                        // Command failed.  Check forP2P_UNSUPPORTED, ERROR, or BUSY
                    }
                })
        }
    }

    private fun startServiceDiscovery(context: Context) {
        if (mWifiP2pChannel != null) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            mWifiP2pManager?.discoverServices(mWifiP2pChannel, object : ActionListener {
                override fun onSuccess() {
                    Log.i(TAG, "onSuccess()")
                }

                //如果一切顺利，恭喜你大功告成。
                // 如果遇到问题，前面异步调用的参数WifiP2pManager.ActionListener参数，它提供了指示成功或失败的回调方法。
                // 把调试断点设置在onFailure()方法中来诊断问题。
                // 这个方法提供了错误代码，以下是可能发生的错误：
                // P2P_UNSUPPORTED运行app的设备上不支持Wi-Fi P2P
                //BUSY 系统忙于处理请求
                //ERROR 由于内部错误导致操作失败
                override fun onFailure(reason: Int) {
                    Log.e(TAG, "onFailure() reason: $reason")
                    if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                        Log.e(TAG, "onFailure() reason is WifiP2pManager.P2P_UNSUPPORTED")
                    }
                }
            })
        }
    }


    private fun initBonjourService() {
        val dnsSdTxtRecordListener: DnsSdTxtRecordListener =
            DnsSdTxtRecordListener { fullDomainName, txtRecordMap, srcDevice ->
                Log.i(
                    TAG,
                    "onDnsSdTxtRecordAvailable() fullDomainName: $fullDomainName, txtRecordMap: $txtRecordMap, srcDevice: $srcDevice"
                )
                if (srcDevice != null && txtRecordMap != null) {
                    mBuddies[srcDevice.deviceAddress] = txtRecordMap.get("name") ?: ""
                }
            }
        val dnsSdServiceResponseListener: DnsSdServiceResponseListener =
            DnsSdServiceResponseListener { instanceName, registrationType, srcDevice ->
                Log.i(
                    TAG,
                    "onDnsSdServiceAvailable() instanceName: $instanceName, registrationType: $registrationType, srcDevice: $srcDevice"
                )
                if (srcDevice != null) {
                    srcDevice.deviceName = if (mBuddies
                            .containsKey(srcDevice.deviceAddress)
                    ) mBuddies[srcDevice.deviceAddress] else srcDevice.deviceName
                }
            }

        if (mWifiP2pChannel != null) {
            //监听设备连接状态
            mWifiP2pManager?.setDnsSdResponseListeners(
                mWifiP2pChannel,
                dnsSdServiceResponseListener,
                dnsSdTxtRecordListener
            )

        }
    }
}