package com.apollo.app.convention

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension

fun Project.configureJava(configure: Action<JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)
