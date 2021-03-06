package com.example.zipfileinmemoryjni

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.Closeable
import java.io.FileInputStream
import java.io.InputStream

fun Closeable?.closeSilently() {
    if (this != null) try {
        this.close()
    } catch (e: Exception) {
    }
}

fun InputStream.readBytesWithSize(size: Long): ByteArray? {
    return when {
        size < 0L -> this.readBytes()
        size == 0L -> ByteArray(0)
        size > Int.MAX_VALUE -> null
        else -> {
            val result = ByteArray(size.toInt())
            readBytesIntoByteArray(result)
            result
        }
    }
}

fun InputStream.skipForcibly(size: Long): Long {
    if (size <= 0L)
        return 0L
    var bytesToSkip = size
    while (bytesToSkip > 0)
        bytesToSkip -= skip(bytesToSkip)
    return size
}

fun InputStream.readBytesIntoByteArray(byteArray: ByteArray, bytesToRead: Int = byteArray.size) {
    var offset = 0
    while (true) {
        val read = this.read(byteArray, offset, bytesToRead - offset)
        if (read == -1)
            break
        offset += read
        if (offset >= bytesToRead)
            break
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
                    bytesCount += inputStream.skip(available.toLong())
                }
                if (bytesCount > 0L)
                    return bytesCount
            }
        }
        return -1L
    }
}
