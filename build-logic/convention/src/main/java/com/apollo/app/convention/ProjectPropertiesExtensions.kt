package com.apollo.app.convention

import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

fun Project.getPropertiesIfExist(fileName: String, message: String): Properties? {
    val doPropertiesExist = File(fileName).exists()
    if (!doPropertiesExist) {
        println(message)
        return null
    }
    return Properties().apply {
        @Suppress("UnstableApiUsage")
        load(FileInputStream(isolated.rootProject.projectDirectory.file(fileName).asFile))
    }
}
