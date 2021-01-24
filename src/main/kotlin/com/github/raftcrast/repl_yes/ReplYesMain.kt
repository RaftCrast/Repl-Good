package com.github.raftcrast.repl_yes

import com.github.raftcrast.repl_yes.github.GithubLatestReleases
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

val httpClient = OkHttpClient()
const val zipPackageName = "v2ray-linux-64.zip"

fun main() {
    downloadAndInstallV2ray()
    setReplStartScript()
    // println(getLatestV2rayDownloadUrl())
}

fun downloadAndInstallV2ray(installDirectory: File = File(".")) {
    val downloadUrl = getLatestV2rayDownloadUrl()
    println("Downloading: $downloadUrl")
    val resourceRequest = Request.Builder()
        .get()
        .url(downloadUrl)
        .build()
    val saveFile = File(installDirectory, zipPackageName)
    val httpCall = httpClient.newCall(resourceRequest)

    httpCall.execute().use {
        try {
            it.body!!.byteStream().use { stream ->
                Files.copy(stream, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            installV2ray(saveFile, installDirectory)
        } catch (e: Throwable) {
            throw GetResourceException("No response content for resource request.", e)
        }
    }
}

fun installV2ray(zipFile: File = File(zipPackageName), installDirectory: File = File(".")) {
    ZipInputStream(FileInputStream(zipFile)).use {
        while (true) {
            val zipEntry = it.nextEntry ?: break
            val entryFile = File(installDirectory, zipEntry.name)
            if (zipEntry.isDirectory && !entryFile.exists() && !entryFile.mkdirs()) {
                throw IOException("Folder creation failed: ${zipEntry.name}")
            } else {
                entryFile.createNewFile()
                Files.copy(it, entryFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

fun getLatestV2rayDownloadUrl(): String {
    val latestReleasesUrl = "https://api.github.com/repos/v2fly/v2ray-core/releases/latest"
    val packageName = "v2ray-linux-64.zip"

    val request = Request.Builder()
        .get()
        .url(latestReleasesUrl)
        .build()
    val targetUrl: String

    println("Getting the latest version of v2ray...")
    httpClient.newCall(request).execute().use {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val parseAdapter = moshi.adapter(GithubLatestReleases::class.java)
        try {
            val resultJson = it.body!!.string()
            val releases = parseAdapter.fromJson(resultJson)
            println("V2ray latest version: ${releases?.name}")
            for (asset in releases!!.assets) {
                if (asset.name == packageName) {
                    targetUrl = asset.downloadUrl
                    return@use
                }
            }
            throw GetResourceException("The required file was not found: $packageName")
        } catch (e: NullPointerException) {
            throw GetResourceException("GitHub API request failed", e)
        }
    }
    return targetUrl
}

fun setReplStartScript(installDirectory: File? = File(".")) {
    val startScriptFile = File(installDirectory, "com.github.raftcrast.repl_yes.main.sh")
    if (!startScriptFile.exists() && startScriptFile.createNewFile()) {
        throw FileNotFoundException("Start script creation failed")
    } else if (!startScriptFile.isFile) {
        throw FileNotFoundException("The start script is not a file")
    }

    BufferedWriter(FileWriter(startScriptFile)).use {
        it.append("./v2ray")
    }
}

