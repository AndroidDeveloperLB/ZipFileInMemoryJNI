package com.example.zipfileinmemoryjni

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import java.io.*

object FileUtilEx {
    fun getFilePathFromUri(context: Context, androidUri: Uri, includeUriMappingTechnique: Boolean = true, tryToGetWritePermission: Boolean = false): ClosableFileHolder? {
        var file: File
        androidUri.path?.let {
            file = File(it)
            if (file.exists() && file.canRead())
                return ClosableFileHolder(file)
        }
        if (androidUri.scheme == "file") {
            val jUri = java.net.URI(androidUri.scheme, androidUri.schemeSpecificPart, androidUri.fragment)
            file = File(jUri)
            if (file.exists() && file.canRead())
                return ClosableFileHolder(file)
        }
        getFilePathFromDocumentUri(androidUri)?.let { filePath ->
            file = File(filePath)
            if (file.exists() && file.canRead())
                return ClosableFileHolder(file)
        }
        if (includeUriMappingTechnique)
            return getFileUsingUriMappingTechnique(context, androidUri, tryToGetWritePermission)
        return null
    }

    fun getFileUsingUriMappingTechnique(context: Context, androidUri: Uri, tryToGetWritePermission: Boolean = false): ClosableFileHolder? {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        for (i in 0..1)
            try {
                parcelFileDescriptor = context.contentResolver.openFileDescriptor(androidUri, if (tryToGetWritePermission && i == 0) "w" else "r")
                if (parcelFileDescriptor != null) {
                    val fd: Int = parcelFileDescriptor.fd
                    val linkFileName = "/proc/self/fd/$fd"
                    val file = File(linkFileName)
                    if (file.exists() && file.canRead())
                        return ClosableFileHolder(file, parcelFileDescriptor)
                    parcelFileDescriptor.tryClose()
                }
            } catch (e: Exception) {
                parcelFileDescriptor.tryClose()
                parcelFileDescriptor = null
            }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getFilePathFromDocumentUri(uri: Uri): String? {
        //            https://stackoverflow.com/questions/5657411/android-getting-a-file-uri-from-a-content-uri
        if ("com.android.externalstorage.documents" == uri.authority) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]
            // This is for checking Main Memory
            return if ("primary".equals(type, ignoreCase = true)) {
                if (split.size > 1) {
                    Environment.getExternalStorageDirectory().absolutePath + "/" + split[1] + "/"
                } else {
                    Environment.getExternalStorageDirectory().absolutePath + "/"
                }
                // This is for checking SD Card
            } else {
                "storage" + "/" + docId.replace(":", "/")
            }
        }
        return null
    }

}
