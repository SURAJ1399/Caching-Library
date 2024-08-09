package com.example.cachinglibrary

import android.content.Context
import android.util.Log
import androidx.collection.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Created by Suraj Kumar
 * Date: 02.04.2024
 * Email: suraj.kumar@sharechat.co
 *
 * Description: https://chat.openai.com/c/f86fd6e7-071c-4bbe-a31c-6a49b5f60a77
 * Jira:
 */
class CacheManager(context: Context) {

    private var lruCache: LruCache<String, Any> = object : LruCache<String, Any>(CacheUtil.size) {
        override fun sizeOf(key: String, value: Any): Int {
            return super.sizeOf(key, value)
        }
    }
    private lateinit var diskCacheDir: File

    init {
        /* Initialize disk cache directory */
        diskCacheDir = File(context.externalCacheDir, "cache")
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    fun get(key: String): Any? {
        var data = lruCache.get(key)

        if (data == null) {
            // Try to fetch from disk cache
            data = getDataFromDisk(key)
            if (data != null) {
                lruCache.put(key, data)
            }
        }

        return data
    }

    fun getDataFromMemory(key: String): Any? {
        return lruCache.get(key) ?: getDataFromDisk(key)
    }

    private fun getDataFromDisk(key: String): Any? {
        val file = File(diskCacheDir, key)
        if (file.exists())
            file.inputStream().use {
                return it.read()
            }
        return null
    }

    private fun saveDataToDisk(key: String, data: ByteArrayOutputStream) {
        val file = File(diskCacheDir, key.hashCode().toString()+".mp4")
        try {
            ObjectOutputStream(FileOutputStream(file)).use { stream ->
                stream.writeObject(data)
            }
        } catch (e: IOException) {
           Log.i("suraj",e.message.toString())
            e.printStackTrace() // Print the stack trace for debugging
        }
    }

    // Method to put data into cache
    fun put(key: String, data: ByteArrayOutputStream) {
        lruCache.put(key, data)
        // Also save to disk cache
        saveDataToDisk(key, data)
    }

    fun storeKey(key: String, context: Context) {
        if (lruCache.get(key) != null)
            return
        donwloadDataFromServer(key = key, context = context) {
            put(key = key, it)
        }

    }

    private fun donwloadDataFromServer(key: String, context: Context, onSuccess: (ByteArrayOutputStream) -> Unit) {
        val request = Request.Builder().url(key).build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(responseCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                val byteArray = ByteArray(8124)
                val outputByteStream = ByteArrayOutputStream()
                var bytesRead: Int = 0
                response.body?.byteStream()?.use { input ->
                    while (input.read(byteArray).also {
                            bytesRead = it
                        } != -1) {
                        Log.i("Suraj", "downloading$bytesRead")
                        outputByteStream.write(byteArray)

                    }
                    val resultByteArray = outputByteStream.toByteArray()
                    return onSuccess.invoke(outputByteStream)

                }
            }

        })
    }

}