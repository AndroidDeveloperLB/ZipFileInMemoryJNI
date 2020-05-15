package com.example.zipfileinmemoryjni

import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File

class ClosableFileHolder(val file: File, private val parcelFileDescriptor: ParcelFileDescriptor? = null) : Closeable {

    override fun close() {
        parcelFileDescriptor.tryClose()
    }

    protected fun finalize() {
        parcelFileDescriptor.tryClose()
    }
}
