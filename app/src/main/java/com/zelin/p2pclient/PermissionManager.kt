package com.zelin.p2pclient

import android.Manifest
import android.R
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import pub.devrel.easypermissions.EasyPermissions


object PermissionManager {

    private const val TAG = "peerClient/PermissionManager"

    const val REQUEST_CODE_PEERS_REQUEST = 0
    const val REQUEST_CODE_PEERS_DISCOVERY = 1
    const val REQUEST_CODE_REQUEST_CONNECT_DEVICE = 2
    const val REQUEST_CODE_REQUEST_CONNECTION_INFO = 3
    const val REQUEST_CODE_LOAD_BITMAP = 4
    const val REQUEST_CODE_CONNECT_SEND_DATA = 5

    fun isHavePermissions(activity: Activity, requestCode: Int): Boolean {
        Log.i(TAG, "isHavePermissions()")
        val permissions: MutableList<String> = ArrayList()
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        //先将集合转换为数组
        //然后增加星号，转换数组为可变参数
        if (!EasyPermissions.hasPermissions(activity, *permissions.toTypedArray())) {
            Log.e(TAG, "isHavePermissions() 权限不足")
            EasyPermissions.requestPermissions(
                activity, "需要权限",
                requestCode, *permissions.toTypedArray()
            )
            return false
        } else {
            return true
        }
    }


}