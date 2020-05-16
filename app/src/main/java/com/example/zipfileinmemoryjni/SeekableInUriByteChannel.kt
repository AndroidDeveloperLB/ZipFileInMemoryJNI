package com.example.zipfileinmemoryjni

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.zipfileinmemoryjni.SeekableInputStreamByteChannel
import java.io.BufferedInputStream
import java.io.InputStream

@RequiresApi(Build.VERSION_CODES.N)
class SeekableInUriByteChannel(someContext: Context, private val uri: Uri) : SeekableInputStreamByteChannel() {
    private val applicationContext = someContext.applicationContext

    override fun calculateSize(): Long = StreamsUtil.getStreamLengthFromUri(applicationContext, uri)

    override fun getNewInputStream(): InputStream = BufferedInputStream(
            applicationContext.contentResolver.openInputStream(uri)!!)
}
