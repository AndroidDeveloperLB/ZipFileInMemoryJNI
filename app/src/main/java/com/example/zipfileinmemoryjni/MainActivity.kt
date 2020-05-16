package com.example.zipfileinmemoryjni

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.FileInputStream
import java.text.NumberFormat
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val numberFormat = NumberFormat.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState != null)
            return
        thread {
            Log.d("AppLog", "started testing")

//            val packageInfo = packageManager.getPackageInfo("com.google.android.configupdater", 0)
//            val file = File(packageInfo.applicationInfo.publicSourceDir)
//            val applicationInfoWithLargestApkFromInstalledApps = getApplicationInfoWithLargestApkFromInstalledApps(this)
//            Log.d("AppLog", "packageName of APK to test:${applicationInfoWithLargestApkFromInstalledApps.packageName}")
//            val file = File(applicationInfoWithLargestApkFromInstalledApps.publicSourceDir)
//            val file = File("/storage/emulated/0/big.xapk")
//                val file = File("/storage/emulated/0/medium.xapk")
//            val file = File("/storage/emulated/0/huge.xapk")
//                val file = File("/storage/emulated/0/Chrome.apk")
//                val file = File("/storage/emulated/0/tiny.zip")
//            val file = File("/storage/emulated/0/base.apk")

//            val uri = Uri.fromFile(file)
//            val fileSize = file.length()
//            Log.d("AppLog", "will parse the file: ${file.absolutePath} fileSize:${numberFormat.format(fileSize)} ")
//            parseUsingAndroidZipFile(file)
//            parseUsingApacheZipFile(file)
//            parseUsingZipInputStream(file)
//            parseUsingApacheZipArchiveInputStream(file)
//            parseUsingApacheZipFileViaByteArray(file, fileSize)
//            parseUsingSeekableInUriByteChannel(uri)
//            parseUsingJniByteArray(file)
            //            val applicationInfo = packageManager.getApplicationInfo("com.google.android.googlequicksearchbox", 0)
//            tryParseZipFile(File(applicationInfo.publicSourceDir))
//                        val applicationInfo = packageManager.getApplicationInfo("com.diune.pictures", 0)
//            tryParseZipFile(File(applicationInfo.publicSourceDir))
//
            val startTime = System.currentTimeMillis()
            var errorsCount = 0
            packageManager.getInstalledApplications(0).forEach { applicationInfo ->
                Log.d("AppLog", "packageName of APK to test:${applicationInfo.packageName}")
                val file = File(applicationInfo.publicSourceDir)
                parseUsingJniByteArray(file).let { succeeded -> if (!succeeded) ++errorsCount }
            }
            Log.d("AppLog", "done parsing in ${System.currentTimeMillis() - startTime}ms errors:$errorsCount")
        }
    }

    private fun parseUsingAndroidZipFile(file: File): Boolean {
        Log.d("AppLog", "testing file using direct path - Android framework ZipFile")
        try {
            val startTime = System.currentTimeMillis()
            java.util.zip.ZipFile(file).use { zipFile: java.util.zip.ZipFile ->
                val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
                for (entry in zipFile.entries()) {
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
                val endTime = System.currentTimeMillis()
                Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
                return true
            }
        } catch (e: Throwable) {
            Log.e("AppLog", "error while trying to parse using  Android framework ZipFile:$e")
            e.printStackTrace()
        }
        return false
    }

    private fun parseUsingApacheZipFile(file: File): Boolean {
        Log.d("AppLog", "testing file using direct path - Apache ZipFile)")
        try {
            val startTime = System.currentTimeMillis()
            ZipFile(file).use { zipFile: ZipFile ->
                val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
                for (entry in zipFile.entries) {
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
                val endTime = System.currentTimeMillis()
                Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
                return true

            }
        } catch (e: Throwable) {
            Log.e("AppLog", "error while trying to parse using Apache ZipFile:$e")
            e.printStackTrace()
        }
        return false
    }

    private fun parseUsingZipInputStream(file: File):Boolean {
        Log.d("AppLog", "testing file using ZipInputStream")
        try {
            val startTime = System.currentTimeMillis()
            ZipInputStream(FileInputStream(file)).use {
                val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
                while (true) {
                    val entry = it.nextEntry ?: break
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
                val endTime = System.currentTimeMillis()
                Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
                return true
            }
        } catch (e: Throwable) {
            Log.e("AppLog", "error while trying to parse using Apache ZipFile:$e")
            e.printStackTrace()
        }
        return false
    }

    private fun parseUsingApacheZipArchiveInputStream(file: File) :Boolean{
        Log.d("AppLog", "testing with ZipArchiveInputStream")
        try {
            val startTime = System.currentTimeMillis()
            ZipArchiveInputStream(FileInputStream(file)).use {
                val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
                while (true) {
                    val entry = it.nextZipEntry ?: break
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
                val endTime = System.currentTimeMillis()
                Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
                return true
            }
        } catch (e: Throwable) {
            Log.e("AppLog", "error while trying to parse using ZipArchiveInputStream:$e")
            e.printStackTrace()
        }
        return false
    }

    private fun parseUsingApacheZipFileViaByteArray(file: File, fileSize: Long):Boolean {
        Log.d("AppLog", "testing file using byte array")
        try {
            val startTime = System.currentTimeMillis()
            val bytes = file.inputStream().use { it.readBytesWithSize(fileSize)!! }
            ZipFile(SeekableInMemoryByteChannel(bytes)).use { zipFile: ZipFile ->
                val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
                for (entry in zipFile.entries) {
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
                val endTime = System.currentTimeMillis()
                Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
                return true
            }
        } catch (e: Throwable) {
            Log.e("AppLog", "error while trying to parse using byte array:$e")
            e.printStackTrace()
        }
        return false
    }

    private fun parseUsingSeekableInUriByteChannel(uri: Uri) :Boolean{
        Log.d("AppLog", "testing using SeekableInUriByteChannel (re-creating inputStream when needed) ")
        try {
            val startTime = System.currentTimeMillis()
            ZipFile(SeekableInUriByteChannel(this, uri)).use { zipFile: ZipFile ->
                val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
                for (entry in zipFile.entries) {
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
                val endTime = System.currentTimeMillis()
                Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
                return true
            }
        } catch (e: Throwable) {
            Log.e("AppLog", "error while trying to parse using SeekableInUriByteChannel:$e")
            e.printStackTrace()
        }
        return false
    }

    private fun parseUsingJniByteArray(file: File): Boolean {
        Log.d("AppLog", "testing file using JNI byte array")
        try {
            val startTime = System.currentTimeMillis()
//                Log.d("AppLog", "file size:${numberFormat.format(fileSize)} fileSizeOnDisk:${numberFormat.format(fileSizeOnDisk)} fileSizeFromUri:${numberFormat.format(fileSizeFromUri)}")
            val bytesCountToAllocate = file.length()
//                Log.d("AppLog", "will allocate ${numberFormat.format(bytesCountToAllocate)} bytes")
//                printMemStats(this)
            val jniByteArrayHolder = JniByteArrayHolder()
            val byteBuffer = jniByteArrayHolder.allocate(bytesCountToAllocate)!!
//                Log.d("AppLog", "memory after allocation:")
//                printMemStats(this)
            FileInputStream(file).use { inStream: FileInputStream ->
                val inBytes = ByteArray(DEFAULT_BUFFER_SIZE)
                while (inStream.available() > 0) {
                    byteBuffer.put(inBytes, 0, inStream.read(inBytes))
                }
            }
            byteBuffer.flip()
            val entriesNamesAndSizes = ArrayList<Pair<String, Long>>()
            ZipFile(ByteBufferChannel(byteBuffer)).use { zipFile: ZipFile ->
                for (entry in zipFile.entries) {
                    val name = entry.name
                    val size = entry.size
                    entriesNamesAndSizes.add(Pair(name, size))
                    Log.v("Applog", "entry name: $name - ${numberFormat.format(size)}")
                }
            }
//                printMemStats(this)
            jniByteArrayHolder.freeBuffer(byteBuffer)
//                Log.d("AppLog", "memory after freeing")
//                printMemStats(this)
            val endTime = System.currentTimeMillis()
            Log.d("AppLog", "got ${entriesNamesAndSizes.size} entries data. time taken: ${endTime - startTime}ms")
            return true
        } catch (e: Exception) {
            Log.e("AppLog", "error while trying to parse using JNI byte array:$e")
            e.printStackTrace()
        }
        return false
    }

    companion object {
        @WorkerThread
        private fun getApplicationInfoWithLargestApkFromInstalledApps(context: Context): ApplicationInfo {
            var result = context.applicationInfo
            var maxSizeFoundSoFar = File(result.publicSourceDir).length()
//            val ignoredPackageNames = hashSetOf(context.packageName, "com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.nga_resources",
//                    "com.google.android.gms", "com.waze")
            val ignoredPackageNames = hashSetOf(context.packageName)
            context.packageManager.getInstalledApplications(0).forEach {
                if (!ignoredPackageNames.contains(it.packageName)) {
                    val file = File(it.publicSourceDir)
                    val fileSize = file.length()
                    if (fileSize > maxSizeFoundSoFar) {
                        result = it
                        maxSizeFoundSoFar = fileSize
                    }
                }
            }
            return result
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
