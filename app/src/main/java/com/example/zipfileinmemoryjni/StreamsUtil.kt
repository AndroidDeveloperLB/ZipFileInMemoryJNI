package com.example.zipfileinmemoryjni

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.Closeable
import java.io.FileInputStream
import java.io.InputStream

fun Closeable?.tryClose() {
    if (this != null) try {
        this.close()
    } catch (e: Exception) {
    }
}

fun InputStream.readBytesWithSize(size: Long): ByteArray? {
    return when {
        size < 0L -> this.readBytes()
        size > Int.MAX_VALUE -> null
        else -> {
            ByteArray(size.toInt()).also { this.read(it) }
        }
    }
}

object StreamsUtil {
    fun getStreamLengthFromUri(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)
                ?.use {
                    if (!it.moveToNext())
                        return@use
                    val fileSize = it.getLong(it.getColumnIndex(MediaStore.MediaColumns.SIZE))
                    if (fileSize > 0)
                        return fileSize
                }
        FileUtilEx.getFilePathFromUri(context, uri, false)?.use {
            val file = it.file
            val fileSize = file.length()
            if (fileSize > 0)
                return fileSize
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            if (inputStream is FileInputStream)
                return inputStream.channel.size()
            else {
                var bytesCount = 0L
                while (true) {
                    val available = inputStream.available()
                    if (available == 0)
                        break
                    val skip = inputStream.skip(available.toLong())
                    if (skip < 0)
                        break
                    bytesCount += skip
                }
                if (bytesCount > 0L)
                    return bytesCount
            }
        }
        return -1L
    }
}
