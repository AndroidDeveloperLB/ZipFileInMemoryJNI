package com.example.zipfileinmemoryjni

import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.nio.ByteBuffer
import kotlin.math.min

class ByteBufferChannel(private val buf: ByteBuffer) : SeekableInMemoryByteChannel() {
//    @Synchronized
    override fun read(dst: ByteBuffer): Int {
        if (buf.remaining() == 0) return -1
        val count = min(dst.remaining(), buf.remaining())
        if (count > 0) {
            val tmp = buf.slice()
            tmp.limit(count)
            dst.put(tmp)
            buf.position(buf.position() + count)
        }
        return count
    }

//    @Synchronized
    override fun position(): Long = buf.position().toLong()

//    @Synchronized
    override fun position(newPosition: Long): ByteBufferChannel {
        require(newPosition or Int.MAX_VALUE - newPosition >= 0)
        buf.position(newPosition.toInt())
        return this
    }

//    @Synchronized
    override fun size(): Long = buf.limit().toLong()

    override fun isOpen(): Boolean = true

    override fun close() {}

}
