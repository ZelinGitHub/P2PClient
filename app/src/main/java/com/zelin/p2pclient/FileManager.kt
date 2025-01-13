package com.zelin.p2pclient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import androidx.core.graphics.drawable.toBitmap
import java.io.ByteArrayOutputStream
import java.io.File


object FileManager {

    fun getImageFilePath(): String {
        val filePath = "${Environment.getExternalStorageDirectory()}/Pictures/woman.jpg"
        return filePath
    }

    // /storage/emulated/0/Pictures/woman.jpg
    fun readBitmapFromFile(filePath: String): Bitmap? {
        val imgFile = File(filePath)
        if (imgFile.exists()) {
            // 从文件解码为 Bitmap
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            return bitmap
        }
        return null
    }

    fun convertDrawableToBytes(drawable: Drawable?): ByteArray {
        drawable as BitmapDrawable?
        val bitmap = drawable?.toBitmap()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            byteArrayOutputStream
        ) // 使用PNG格式，质量为100（无损）
        val byteArray = byteArrayOutputStream.toByteArray()
        return byteArray
    }
}