package com.example.zipfileinmemoryjni

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

@RequiresApi(Build.VERSION_CODES.N)
abstract class InefficientSeekableInputStreamByteChannel : SeekableByteChannel {
    private var position: Long = 0L
    private var cachedSize: Long = -1L
    private var buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    abstract fun getNewInputStream(): InputStream

    override fun isOpen(): Boolean = true

    override fun position(): Long = position

    override fun position(newPosition: Long): SeekableByteChannel {
//        Log.d("AppLog", "position $newPosition")
        require(newPosition >= 0L) { "Position has to be positive" }
        position = newPosition
        return this
    }

    open fun calculateSize(): Long {
        return getNewInputStream().use { inputStream: InputStream ->
            if (inputStream is FileInputStream)
                return inputStream.channel.size()
            var bytesCount = 0L
            while (true) {
                val available = inputStream.available()
                if (available == 0)
                    break
                bytesCount += inputStream.skipForcibly(available.toLong())
            }
            bytesCount
        }
    }

    final override fun size(): Long {
        if (cachedSize < 0L)
            cachedSize = calculateSize()
//        Log.d("AppLog", "size $cachedSize")
        return cachedSize
    }

    override fun close() {
    }

    override fun read(buf: ByteBuffer): Int {
        var wanted: Int = buf.remaining()
//        Log.d("AppLog", "read wanted:$wanted")
        if (wanted <= 0)
            return wanted
        val possible = (calculateSize() - position).toInt()
        if (possible <= 0)
            return -1
        if (wanted > possible)
            wanted = possible
        if (buffer.size < wanted)
            buffer = ByteArray(wanted)
        getNewInputStream().use { inputStream ->
            inputStream.skipForcibly(position)
            //now we have an inputStream right on the needed position
            inputStream.readBytesIntoByteArray(buffer, wanted)
        }
        buf.put(buffer, 0, wanted)
        position += wanted
        return wanted
    }

    //not needed, because we don't store anything in memory
    override fun truncate(size: Long): SeekableByteChannel = this

    override fun write(src: ByteBuffer?): Int {
        //not needed, we read only
        throw  NotImplementedError()
    }
}
