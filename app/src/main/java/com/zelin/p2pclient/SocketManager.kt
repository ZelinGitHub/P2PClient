package com.zelin.p2pclient

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.zelin.p2pclient.PermissionManager.REQUEST_CODE_CONNECT_SEND_DATA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket


object SocketManager {

    private const val PORT = 7236
    private const val TAG = "peerClient/SocketManager"
    private var mIsSending:Boolean=false

    fun connectAndSendData(context: Activity, hostAddress: String) {
        Log.i(TAG, "connectAndSendData() hostAddress: $hostAddress, mIsSending: $mIsSending")
        if(mIsSending){
            return
        }
        mIsSending=true
        if (
            !PermissionManager.isHavePermissions(context, REQUEST_CODE_CONNECT_SEND_DATA)
        ) {
            mIsSending=false
            return
        }
        Toast.makeText(context, "开始传输数据", Toast.LENGTH_SHORT)
            .show()
        //开启子线程
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var socketOutputStream: OutputStream? = null
                val socket = Socket()
                try {
                    val address = InetSocketAddress(hostAddress, PORT)
                    socket.connect(address)
                    socket.tcpNoDelay = true;
                    socket.sendBufferSize = 16 * 1024 * 1024;//default 512 * 1024;
                    socket.receiveBufferSize = 16 * 1024 * 1024;////default 1024 * 1024;
                    //连接时间、延迟、带宽
                    socket.setPerformancePreferences(1, 8, 1);
                    socket.setSoLinger(true, 1);
                    socketOutputStream = socket.getOutputStream()
                    val fileInputStream = FileInputStream(FileManager.getImageFilePath())
                    var length: Int
                    val buffer = ByteArray(1024)
                    Log.i(TAG, "realConnectAndSendData() 数据开始发送")
                    while ((fileInputStream.read(buffer).also { length = it }) != -1) {
                        Log.i(TAG, "realConnectAndSendData() 数据发送 length: $length")
                        socketOutputStream.write(buffer, 0, length)
                    }
                    socket.shutdownOutput()
                    Log.i(TAG, "realConnectAndSendData() 数据发送完成")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "数据传输完成", Toast.LENGTH_SHORT)
                            .show()
                    }
                } finally {
                    Log.i(TAG, "realConnectAndSendData() finally 回收数据")
                    mIsSending=false
                    socketOutputStream?.close()
                    socket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }
}