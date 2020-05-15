package com.example.zipfileinmemoryjni

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.text.format.Formatter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileInputStream
import java.text.NumberFormat
import kotlin.concurrent.thread
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        thread {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                //val file = File("/storage/emulated/0/big.xapk")
                //val file = File("/storage/emulated/0/Chrome.apk")
                val file = File(packageInfo.applicationInfo.publicSourceDir)
                val uri = Uri.fromFile(file)
                val fileSizeFromUri = StreamsUtil.getStreamLengthFromUri(this, uri)
                val fileSize = file.length()
                val fileSizeOnDisk = getFileSizeOnDisk(this, file, fileSize)
                Log.d("AppLog", "file size:$fileSize fileSizeOnDisk:$fileSizeOnDisk fileSizeFromUri:$fileSizeFromUri")
                val bytesCountToAllocate = fileSizeOnDisk
                Log.d("AppLog", "will allocate $bytesCountToAllocate bytes")
                printMemStats(this)
                val jniByteArrayHolder = JniByteArrayHolder()
                val byteBuffer = jniByteArrayHolder.allocate(bytesCountToAllocate)
                Log.d("AppLog", "memory after allocation:")
                printMemStats(this)
                val inStream = FileInputStream(file)
                val inBytes = ByteArray(DEFAULT_BUFFER_SIZE)
                while (inStream.available() > 0) {
                    inStream.read(inBytes)
                    byteBuffer.put(inBytes)
                }
                byteBuffer.flip()
                ZipFile(ByteBufferChannel(byteBuffer)).use { zipFile: ZipFile ->
                    Log.d("Applog", "Starting Zip file name dump...")
                    for (entry in zipFile.entries) {
                        val name = entry.name
                        Log.d("Applog", "Zip name: $name")
                    }
                }
                Log.d("AppLog", "memory after parsing")
                printMemStats(this)
                jniByteArrayHolder.freeBuffer(byteBuffer)
                Log.d("AppLog", "memory after freeing")
                printMemStats(this)
            } catch (e: Exception) {
                Log.e("AppLog", "error:$e")
                e.printStackTrace()
            }
        }
    }

    companion object {
        private fun getFileSizeOnDisk(context: Context, file: File?, fileSize: Long, defaultBlockSizeIfNotFound: Long = 4096L): Long {
            var blockSize: Long = 0L
            if (file != null)
                blockSize = StatFs(file.absolutePath).blockSizeLong
            else {
                //we don't have a file, so we guess based on the max of all storages
                val externalCacheDirs: Array<File?> = ContextCompat.getExternalCacheDirs(context)
                externalCacheDirs.asSequence().filterNotNull().forEach { cacheFolder: File ->
                    blockSize = max(blockSize, StatFs(cacheFolder.absolutePath).blockSizeLong)
                }
                if (blockSize == 0L)
                //default in case we couldn't find for some reason
                    blockSize = defaultBlockSizeIfNotFound
            }
            //based on: https://stackoverflow.com/questions/3750590/get-size-of-file-on-disk/3750658#3750658
            return blockSize * ((fileSize + blockSize - 1) / blockSize)
        }

        private fun printMemStats(context: Context) {
            val myFormat: NumberFormat = NumberFormat.getInstance()
            val memoryInfo = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
            val nativeHeapSize = memoryInfo.totalMem
            val nativeHeapFreeSize = memoryInfo.availMem
            val usedMemInBytes = nativeHeapSize - nativeHeapFreeSize
            val usedMemInPercentage = usedMemInBytes * 100 / nativeHeapSize
            Log.d(
                    "AppLog", "total:${Formatter.formatFileSize(context, nativeHeapSize)} " +
                    "free:${Formatter.formatFileSize(context, nativeHeapFreeSize)} - ${myFormat.format(nativeHeapFreeSize)} B " +
                    "used:${Formatter.formatFileSize(context, usedMemInBytes)} - ${myFormat.format(usedMemInBytes)} B ($usedMemInPercentage%)"
            )
        }

    }

}
