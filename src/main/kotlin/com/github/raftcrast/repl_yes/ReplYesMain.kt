@file:Suppress("SpellCheckingInspection")

package com.github.raftcrast.repl_yes

import com.github.raftcrast.repl_yes.github.GithubLatestReleases
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

val httpClient = OkHttpClient.Builder()
    .build()
const val zipPackageName = "v2ray-linux-64.zip"

fun main() {
    println("Checking v2ray core...")
    if (!checkV2rayCore()) {
        downloadAndInstallV2ray()
    } else {
        println("It seems that you are ready for v2ray. Do you want to continue to configure v2ray? " +
                "This will lose the existing configuration file!")
        val input = getInput(
            "If you are sure, please enter (y)es, " +
                    "otherwise enter something else to cancel the operation: "
        )
        if (input != "y") {
            println("OK, before the next execution, you need to know whether you really need to install it again. " +
                    "If it is a misoperation, you can do it again.")
            exitProcess(1)
        } else {
            println("It seems that you have already decided, so let's start to configure v2ray!\n" +
                    "Remember, until the last step, you can cancel the configuration with `Ctrl + C`.")
        }
    }
    setReplStartScript()
    println("There are two last steps left!")
    generateV2rayConfig()
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
            println("V2ray installation package downloaded.")
        } catch (e: Throwable) {
            throw GetResourceException("No response content for resource request.", e)
        }
    }
    installV2ray(saveFile, installDirectory)
}

fun installV2ray(zipFile: File = File(zipPackageName), installDirectory: File = File(".")) {
    print("Installing V2ray...")
    ZipInputStream(FileInputStream(zipFile)).use {
        while (true) {
            val zipEntry = it.nextEntry ?: break
            val entryFile = File(installDirectory, zipEntry.name)
            if (zipEntry.isDirectory) {
                if (!entryFile.exists() && !entryFile.mkdirs()) {
                    throw IOException("Folder creation failed: ${zipEntry.name}")
                }
            } else {
                entryFile.createNewFile()
                Files.copy(it, entryFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
    println("Install Success!")
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
        // Sometimes, I really don't like to use data classes to parse JSON,
        // which requires me to write several data classes but only use them once in the code :(
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
    val startScriptFile = File(installDirectory, "main.sh")
    if (!startScriptFile.exists() && !startScriptFile.createNewFile()) {
        throw FileNotFoundException("Start script creation failed")
    } else if (!startScriptFile.isFile) {
        throw FileNotFoundException("The start script is not a file")
    }

    BufferedWriter(FileWriter(startScriptFile)).use {
        it.append("./v2ray")
    }
}

fun generateV2rayConfig(installDirectory: File = File(".")) {
    val randomUUID = UUID.randomUUID()
    val randomPath = "/${randomString(16)}"

    val shareLink = generateShareLink(
        getInput("Please tell me your repl user name: ").toLowerCase(),
        getInput("And your repl workspace name: ").replace(' ', '-'),
        randomUUID,
        randomPath
    )

    generateConfigToFile(installDirectory, uuid = randomUUID, path = randomPath)
    println("success! Repl is ready! Click the run button at the top, start v2ray server, " +
            "import the following sharing link into the client, and you're done! Enjoy it~")
    println(shareLink)
}

fun generateConfigToFile(installDirectory: File, uuid: UUID, path: String) {
    val uuidFlag = "{new.uuid}"
    val pathFlag = "{new.path}"

    val configFile = File(installDirectory, "config.json")
    if (!configFile.exists() && !configFile.createNewFile()) {
        throw FileNotFoundException("Configuration file creation failed")
    }

    val configTemplateStream = GetResourceException::class.java.getResourceAsStream("/server.json")
        ?: throw GetResourceException("Configuration template not found")

    val configBuffer = StringBuilder()
    BufferedReader(InputStreamReader(configTemplateStream, StandardCharsets.UTF_8))
        .lines().forEach { configBuffer.append(it).append('\n') }

    val uuidIndex = configBuffer.indexOf(uuidFlag)
    if (uuidIndex >= 0) {
        configBuffer.replace(uuidIndex, uuidIndex + uuidFlag.length, uuid.toString())
    }

    val pathIndex = configBuffer.indexOf(pathFlag)
    if (pathIndex >= 0) {
        configBuffer.replace(pathIndex, pathIndex + pathFlag.length, path)
    }
    configFile.bufferedWriter(StandardCharsets.UTF_8).use { it.write(configBuffer.toString()) }
}

fun generateShareLink(replUserName: String, replName: String, uuid: UUID, path: String): String {
    val shareInfo = """
        {
            "add":"$replName.$replUserName.repl.co",
            "aid":16,
            "host":"",
            "id":"$uuid",
            "net":"ws",
            "path":"$path",
            "port":443,
            "ps":"repl-${path.subSequence(1, 11)}",
            "tls":"tls",
            "type":"none",
            "v":2
        }
    """.trimIndent()
    return "vmess://" + Base64.getEncoder().encodeToString(shareInfo.encodeToByteArray())
}

/**
 * Receives an input from the standard input, ending with a carriage return.
 * @param msg prompt information.
 */
fun getInput(msg: String?): String = Scanner(System.`in`).let {
    if (msg != null) {
        print(msg)
    }
    return it.nextLine()
}

/**
 * Random string generation for generating random names and random paths.
 * @param length string length.
 */
fun randomString(length: Int): String {
    val charsString = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val chars: Array<Char> = Array(charsString.length, init = {' '})
    charsString.forEachIndexed { index, char ->
        chars[index] = char
    }

    val random = Random()
    val builder = StringBuilder(length)

    for (index: Int in 1..length) {
        builder.append(chars[random.nextInt(chars.size)])
    }
    return builder.toString()
}

/**
 * Check whether v2ray is installed by calling the main program of v2ray.
 * @param installDirectory v2ray installation directory
 */
fun checkV2rayCore(installDirectory: File? = File(".")) :Boolean {
    return try {
        val process = Runtime.getRuntime().exec("./v2ray -version", emptyArray(), installDirectory)
        println(String(process.inputStream.readAllBytes()))
        process.waitFor(5L, TimeUnit.SECONDS)
        process.exitValue() == 0
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
