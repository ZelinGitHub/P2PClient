package com.zelin.p2pclient

import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {
    //p2p管理
    private var mWifiP2pManager: WifiP2pManager? = null

    //p2p渠道。manager initialize之后，会得到这个渠道。
    private var mWifiP2pChannel: WifiP2pManager.Channel? = null

    //p2p设备
    private var mWifiP2pSrcDevice: WifiP2pDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = View(this)
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        setContentView(view)
        initP2p()
        discoverPeers()
        observeDevicesConnectStatus()
        connectDevice()
        createGroup()
        removeGroup()
    }


    private fun initP2p() {
        mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mWifiP2pChannel =
            mWifiP2pManager?.initialize(
                this, Looper.getMainLooper()
            ) { Log.e(TAG, "onChannelDisconnected()") }
    }

    //监听设备连接状态
    private fun observeDevicesConnectStatus() {
        if (mWifiP2pChannel != null) {
            //监听设备连接状态
            mWifiP2pManager?.setDnsSdResponseListeners(mWifiP2pChannel,

                // 发现可用的设备
                { instanceName, registrationType, srcDevice ->
                    Log.i(
                        TAG,
                        "onDnsSdServiceAvailable() instanceName: $instanceName, registrationType: $registrationType, srcDevice: $srcDevice"
                    )
                    mWifiP2pSrcDevice = srcDevice
                }
            )

            { fullDomainName, txtRecordMap, srcDevice ->
                Log.i(
                    TAG,
                    "onDnsSdTxtRecordAvailable() fullDomainName: $fullDomainName, txtRecordMap: $txtRecordMap, srcDevice: $srcDevice"
                )
            }
        }
    }

    //需要有一个srcDevice
    private fun connectDevice() {
        val config = WifiP2pConfig()
        if (mWifiP2pSrcDevice != null) {
            config.deviceAddress = mWifiP2pSrcDevice?.deviceAddress;
            mWifiP2pManager?.connect(mWifiP2pChannel, config, object : ActionListener {
                override fun onSuccess() {
                    // 连接成功
                    Log.i(TAG, "onSuccess()")
                }

                override fun onFailure(reason: Int) {
                    // 连接失败
                    Log.i(TAG, "onFailure() reason: $reason")
                }
            })
        }
    }


    //******************************************************************
    private fun discoverPeers() {
        if (mWifiP2pChannel != null) {
            mWifiP2pManager?.discoverPeers(mWifiP2pChannel, object : ActionListener {
                override fun onSuccess() {
                    Log.i(TAG, "discoverPeers() onSuccess()")
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "discoverPeers() onFailure()")
                }

            })
        }

    }


    private fun createGroup() {
        mWifiP2pManager?.createGroup(mWifiP2pChannel, object : ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "onSuccess()")
            }

            override fun onFailure(reason: Int) {
                Log.i(TAG, "onFailure() reason: $reason")
            }
        })

    }

    private fun removeGroup() {

        mWifiP2pManager?.removeGroup(mWifiP2pChannel, object : ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "onSuccess()")
            }

            override fun onFailure(reason: Int) {
                Log.i(TAG, "onFailure() reason: $reason")
            }
        })
    }

    companion object {
        private const val TAG = "P2PClient/MainActivity"
    }
}
