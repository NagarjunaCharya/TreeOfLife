package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.media.MediaPlayer

fun Uri.toBase64(context: Context): String? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, this)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
        val outputStream = ByteArrayOutputStream()
        // Resize down to keep API request fast and within limits
        val scaled = Bitmap.createScaledBitmap(bitmap, 800, (800 * bitmap.height / bitmap.width), true)
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun playBase64Audio(context: Context, base64Audio: String) {
    try {
        val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
        val tempFile = File.createTempFile("tts_audio", ".mp3", context.cacheDir)
        FileOutputStream(tempFile).use { it.write(bytes) }
        
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(tempFile.absolutePath)
            prepare()
            setOnCompletionListener { 
                it.release()
                tempFile.delete()
            }
            start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
