package com.example.zipfileinmemoryjni

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

@RequiresApi(Build.VERSION_CODES.N)
abstract class SeekableInputStreamByteChannel : SeekableByteChannel {
    private var position: Long = 0L
    private var actualPosition: Long = 0L
    private var cachedSize: Long = -1L
    private var inputStream: InputStream? = null

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
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesCount = 0L
            while (true) {
                val read = inputStream.read(buffer)
                if (read < 0)
                    break
                bytesCount += read
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
        inputStream.closeSilently().also { inputStream = null }
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
        var inputStream = this.inputStream
        //skipping to required position
        if (inputStream == null) {
            inputStream = getNewInputStream()
//            Log.d("AppLog", "getNewInputStream")
            inputStream.skip(position)
            this.inputStream = inputStream
        } else {
            if (actualPosition > position) {
                inputStream.close()
                actualPosition = 0L
                inputStream = getNewInputStream()
//                Log.d("AppLog", "getNewInputStream")
                this.inputStream = inputStream
            }
            inputStream.skip(position - actualPosition)
        }
        //now we have an inputStream right on the needed position
        val byteArray = ByteArray(min(DEFAULT_BUFFER_SIZE, wanted))
        var remaining = wanted
        while (remaining > 0L) {
            val bytesRead = inputStream.read(byteArray, 0, min(byteArray.size, remaining))
//            Log.d("AppLog", "read $bytesRead $remaining")
            if (bytesRead < 0)
                break
            buf.put(byteArray, 0, wanted)
            remaining -= bytesRead
        }
        position += wanted
        actualPosition = position
        return wanted
    }

    //not needed, because we don't store anything in memory
    override fun truncate(size: Long): SeekableByteChannel = this

    override fun write(src: ByteBuffer?): Int {
        //not needed, we read only
        throw  NotImplementedError()
    }
}