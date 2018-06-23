#!/usr/bin/env kscript

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

if (args.isEmpty()) {
    println("Please specify the file to detect symbolic link.")
    System.exit(1)
}

for (arg in args) {
    detectSymbolicLink(arg)
}

fun detectSymbolicLink(file: String) {
    var filePath = Paths.get(file)
    if (Files.notExists(filePath)) {
        println("File: $file not exist.")
        return
    }

    println(filePath.toStringIncludeNormalizedFormat())

    var level = "-"
    while (Files.isSymbolicLink(filePath)) {
        val symbolicLink = Files.readSymbolicLink(filePath)
        println("$level>${symbolicLink.toStringIncludeNormalizedFormat()}")

        level += "-"
        filePath = symbolicLink
    }

    println("-----------------------------------------")
}

fun Path.toStringIncludeNormalizedFormat(): String {
    val filePathStr = this.toString()
    val normalizedFilePath = this.toAbsolutePath().normalize().toString()

    if (normalizedFilePath == filePathStr) {
        return filePathStr
    }

    return "$filePathStr (Normalized: $normalizedFilePath)"
}