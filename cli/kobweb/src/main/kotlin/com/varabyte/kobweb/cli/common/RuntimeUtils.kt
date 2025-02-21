package com.varabyte.kobweb.cli.common

import com.varabyte.kobweb.common.io.consumeAsync
import java.io.File
import java.io.FileNotFoundException

fun Runtime.gradlew(vararg args: String): Process {
    val gradlewBinary = if (!Os.isWindows()) "gradlew" else "gradlew.bat"
    var currDir: File? = File(".").absoluteFile
    while (currDir != null && !File(currDir, gradlewBinary).exists()) {
        currDir = currDir.parentFile
    }

    if (currDir == null) {
        throw FileNotFoundException("\"$gradlewBinary\" not found in current or parent directories")
    }

    val finalArgs = mutableListOf("${currDir.absolutePath}/$gradlewBinary")
    finalArgs.addAll(args)
    return exec(finalArgs.toTypedArray())
}

fun Runtime.git(vararg args: String): Process {
    val finalArgs = mutableListOf("git")
    finalArgs.addAll(args)
    return exec(finalArgs.toTypedArray())
}

private fun defaultOutputHandler(line: String, isError: Boolean) {
    if (isError) {
        System.err.println(line)
    }
    else {
        println(line)
    }
}

fun Process.consumeProcessOutput(onLineRead: (line: String, isError: Boolean) -> Unit = ::defaultOutputHandler) {
    inputStream.consumeAsync { line -> onLineRead(line, false) }
    errorStream.consumeAsync { line -> onLineRead(line, true) }
}