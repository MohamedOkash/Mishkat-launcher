package com.mishkat.remote.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TvApiClient {

    suspend fun sendKey(ip: String, port: Int = 8787, key: String): Boolean = withContext(Dispatchers.IO) {
        postForm("http://$ip:$port/api/key", mapOf("key" to key)) != null
    }

    suspend fun sendTouch(
        ip: String,
        port: Int = 8787,
        dx: Float,
        dy: Float,
        action: String = "move"
    ): Boolean = withContext(Dispatchers.IO) {
        postForm(
            "http://$ip:$port/api/touch",
            mapOf("dx" to dx.toString(), "dy" to dy.toString(), "action" to action)
        ) != null
    }

    suspend fun sendText(ip: String, port: Int = 8787, text: String): Boolean = withContext(Dispatchers.IO) {
        postForm("http://$ip:$port/api/text", mapOf("text" to text)) != null
    }

    suspend fun launchPackage(ip: String, port: Int = 8787, pkg: String): Boolean = withContext(Dispatchers.IO) {
        postForm("http://$ip:$port/api/launch", mapOf("pkg" to pkg)) != null
    }

    suspend fun sendBoost(ip: String, port: Int = 8787): String? = withContext(Dispatchers.IO) {
        postForm("http://$ip:$port/api/boost", emptyMap())
    }

    suspend fun sendM3u(ip: String, port: Int = 8787, urlOrContent: String): Boolean = withContext(Dispatchers.IO) {
        postForm("http://$ip:$port/api/m3u", mapOf("url" to urlOrContent, "content" to urlOrContent)) != null
    }

    suspend fun openStorage(ip: String, port: Int = 8787): Boolean = withContext(Dispatchers.IO) {
        postForm("http://$ip:$port/api/storage", emptyMap()) != null
    }

    suspend fun fetchInfo(ip: String, port: Int = 8787): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("http://$ip:$port/api/info")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("Accept", "application/json")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    private fun postForm(urlString: String, params: Map<String, String>): String? = runCatching {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 1500
            readTimeout = 1500
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }

        val body = params.entries.joinToString("&") { (k, v) ->
            val ek = URLEncoder.encode(k, "UTF-8")
            val ev = URLEncoder.encode(v, "UTF-8")
            "$ek=$ev"
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
            it.write(body)
            it.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    }.getOrNull()
}
