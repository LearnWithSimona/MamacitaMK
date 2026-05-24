package com.apollo.app.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Packaging
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

private val Project.packageName
    get() = libs.getVersion("app-version-appId")
private val Project.testRunner
    get() = libs.getVersion("app-build-testRunner")
private val Project.minSDKVersion
    get() = libs.getVersion("app-build-minimumSDK").toInt()
private val Project.compileSDKVersion
    get() = libs.getVersion("app-build-compileSDKVersion").toInt()
private val Project.versionName
    get() = libs.getVersion("app-version-versionName")
private val Project.versionCode
    get() = libs.getVersion("app-version-versionCode").toInt()

internal fun Project.commonVersioning(applicationExtension: ApplicationExtension) {
    applicationExtension.apply {
        compileSdk = compileSDKVersion
        defaultConfig {
            minSdk = minSDKVersion
            targetSdk = compileSDKVersion
            testInstrumentationRunner = testRunner
            versionName = this@commonVersioning.versionName
            versionCode = this@commonVersioning.versionCode
        }
    }
}

internal fun Project.commonVersioning(libraryExtension: LibraryExtension) {
    libraryExtension.apply {
        compileSdk = compileSDKVersion
        defaultConfig {
            minSdk = minSDKVersion
            testInstrumentationRunner = testRunner
        }
    }
}

internal fun LibraryExtension.addLibrariesConfig() {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
}

internal fun Project.configureAppPluginPackageAndNameSpace(commonExtensions: ApplicationExtension) {
    commonExtensions.apply {
        namespace = packageName
        defaultConfig {
            applicationId = libs.getVersion("app-version-appId")
        }

        packaging.setupResources()
    }
}

internal fun Project.configureJavaCompatibilityCompileOptions(commonExtensions: CommonExtension) {
    commonExtensions.apply {
        compileOptions.apply {
            val currentJavaVersionFromLibs = JavaVersion.valueOf(
                libs.getVersion("app-build-javaVersion"),
            )

            sourceCompatibility = currentJavaVersionFromLibs
            targetCompatibility = currentJavaVersionFromLibs
            isCoreLibraryDesugaringEnabled = true
        }

        dependencies {
            "coreLibraryDesugaring"(libs.getLibrary("android-desugarJdkLibs"))
        }
    }
}

internal fun Project.configureLibraryAndTestNameSpace() {
    configure<CommonExtension> {
        namespace = packageName.plus(path.replace(":", ".").replace("-", "."))
        packaging.setupResources()
    }
}

internal val Project.googleServicesJsonExists get() = file("google-services.json").exists()

private fun Packaging.setupResources() {
    resources {
        excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        excludes.add("google/protobuf/descriptor.proto")
    }
}

inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> TODO("Unsupported project extension $this ${T::class}")
    }.apply {
        freeCompilerArgs.addAll(
            // Enable experimental coroutines APIs, including Flow
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

fun Project.composeCompiler(block: ComposeCompilerGradlePluginExtension.() -> Unit) {
    extensions.configure<ComposeCompilerGradlePluginExtension>(block)
}
