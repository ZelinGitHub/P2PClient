package com.zelin.p2pclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log

class P2pBroadcastReceiver(
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "onReceive()")
        val action = intent?.action
        when (action) {
            //一次设备发现开始或结束
            //设备发现开始，会收到这个广播。
            //设备发现结束，也会收到这个广播。
            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                Log.i(TAG, "onReceive() WIFI_P2P_DISCOVERY_CHANGED_ACTION")
                mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION?.invoke(intent.extras)
            }
            //可以试着得到Group连接信息，从而得到服务端的IP地址
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Log.i(TAG, "onReceive() WIFI_P2P_CONNECTION_CHANGED_ACTION")
                mOnWIFI_P2P_CONNECTION_CHANGED_ACTION?.invoke(intent.extras)
            }
            //发现设备，可以得到设备列表
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.i(TAG, "onReceive() WIFI_P2P_PEERS_CHANGED_ACTION")
                mOnWIFI_P2P_PEERS_CHANGED_ACTION?.invoke(intent.extras)
            }
        }
    }

    companion object {
        private const val TAG = "peerClient/WiFiDirectBroadcastReceiver"
        var mOnWIFI_P2P_PEERS_CHANGED_ACTION: ((bundle: Bundle?) -> Unit)? = null
        var mOnWIFI_P2P_DISCOVERY_CHANGED_ACTION: ((bundle: Bundle?) -> Unit)? = null
        var mOnWIFI_P2P_CONNECTION_CHANGED_ACTION: ((bundle: Bundle?) -> Unit)? = null

        private var mReceiver: BroadcastReceiver? = P2pBroadcastReceiver()

        fun registerP2pReceiver(context: Context) {
            val mIntentFilter = IntentFilter()
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            context.registerReceiver(mReceiver, mIntentFilter);
        }


        fun unRegisterP2pReceiver(context: Context) {
            context.unregisterReceiver(mReceiver);
        }
    }
}