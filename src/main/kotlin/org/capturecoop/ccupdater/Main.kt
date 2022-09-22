package org.capturecoop.ccupdater

import io.wollinger.zipper.ZipBuilder
import io.wollinger.zipper.ZipMethod
import java.awt.GraphicsEnvironment
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JProgressBar
import javax.swing.UIManager

private var url: String? = null
private var filename: String? = null
private var dir: String? = null
private var gui = false
private var exec: String? = null
private var extract = false
private var deleteFile = false

fun main(args: Array<String>) = checkArguments(args)

fun checkArguments(args: Array<String>)  {
    if(args.isEmpty()) {
        println("CCUpdater")
        println("Available Arguments:")
        println("-url https://server.com/file.zip")
        println("-filename newName.zip")
        println("-dir C:/place/to/save/")
        println("-gui")
        println("-exec file.jar")
        println("-extract")
        println("-deleteFile")
        return
    }

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    args.forEachIndexed { index, part ->
        when(part) {
            "-url" -> url = checkArg(index, args)
            "-filename" -> filename = checkArg(index, args)
            "-dir" -> dir = checkArg(index, args)
            "-gui" ->  gui = true
            "-exec" -> exec = checkArg(index, args)
            "-extract" -> extract = true
            "-deleteFile"-> deleteFile = true
        }
    }
    run()
}

fun checkArg(index: Int, args: Array<String>): String = if(args.size > index + 1) args[index + 1] else throw IllegalArgumentException("Missing argument after $index for $args")

fun run() {
    println("CCUpdater")

    val progressBar = JProgressBar(JProgressBar.HORIZONTAL, 0, 100)
    val frame = JFrame().apply {
        if(gui) {
            setSize(512, 128)
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            title = "Starting..."
            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode.also { dm ->
                setLocation(dm.width / 2 - width / 2, dm.height / 2 - height / 2)
            }
            add(progressBar)
            iconImage = ImageIcon(javaClass::class.java.getResource("/org/capturecoop/ccupdater/download.png")).image
            isVisible = true
        }
    }


    TimeUnit.SECONDS.sleep(1)
    executeDL(frame, progressBar)
}

fun executeDL(frame: JFrame, progressBar: JProgressBar) {
    val uri = URL(url + "?cache=" + System.currentTimeMillis())
    if(filename == null)
        filename = File(URL(url).file).name

    val httpcon = uri.openConnection() as HttpURLConnection
    httpcon.addRequestProperty("User-Agent", "Mozilla/4.0")

    var path = filename ?: throw IllegalArgumentException("Bad path!")
    if(!dir.isNullOrEmpty()) {
        dir?.let { File(it).mkdirs() }
        path = "$dir//$filename"
    }

    path = File(path).absolutePath

    println("Starting download from: $uri")
    println("Saving to: $path")

    val inStream = BufferedInputStream(httpcon.inputStream)
    val outStream = FileOutputStream(path)
    frame.title = "Downloading..."
    val dataBuffer = ByteArray(1024)
    var bytesRead: Int
    val total = httpcon.contentLength
    var downloaded = 0

    var lastProgress = 0
    while (inStream.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
        outStream.write(dataBuffer, 0, bytesRead)
        downloaded += bytesRead
        val currentProgress = (downloaded.toDouble() / total.toDouble() * 100.0).toInt()
        if(lastProgress != currentProgress) {
            frame.title = "Downloading... ($currentProgress%)"
            if (gui) progressBar.value = currentProgress
            println("Status: $currentProgress%")
        }
        lastProgress = currentProgress
    }

    if(!exec.isNullOrEmpty()) {
        var toExecute = "$dir//$exec"
        if(dir == null)
            toExecute = exec ?: throw java.lang.IllegalArgumentException("exec is bad!")
        if(exec?.lowercase()?.endsWith(".jar") == true) {
            println("Launching jar: $toExecute")
            ProcessBuilder("java", "-jar", toExecute).start()
        } else {
            println("Launching: $toExecute")
            ProcessBuilder(toExecute).start()
        }
    }

    if(extract) {
        ZipBuilder().apply {
            setMethod(ZipMethod.UNZIP)
            addInput(path)
            addOutput(if(!dir.isNullOrEmpty()) dir else LocationFinder.getCurrentFolder())
            setCopyOption(StandardCopyOption.REPLACE_EXISTING)
            build()
        }
    }

    if(deleteFile) {
        println("Deleting file: $path")
        File(path).delete()
    }
    frame.dispose()
}
class LocationFinder {
    companion object {
        fun getCurrentFolder(): String {
            URLDecoder.decode(Paths.get(LocationFinder::class.java.protectionDomain.codeSource.location.toURI()).toString(), "UTF-8")?.let { ftu ->
                File(ftu).also {
                    return if (it.name.endsWith(".jar")) ftu.replace(it.name, ""); else ftu;
                }
            }
            throw FileNotFoundException("Couldnt find jar location!")
        }
    }
}


