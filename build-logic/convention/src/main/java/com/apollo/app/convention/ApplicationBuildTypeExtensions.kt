package com.apollo.app.convention

import com.android.build.api.dsl.ApplicationBuildType
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.kotlin.dsl.configure

internal val ApplicationBuildType.isReleaseBuild: Boolean
    get() = name.contains(BuildTypes.release)

internal fun ApplicationBuildType.enableCrashlytics(shouldEnable: Boolean) {
    crashlyticsManifestPlaceholder(shouldEnable)
    configure<CrashlyticsExtension> {
        mappingFileUploadEnabled = shouldEnable
    }
}

private fun ApplicationBuildType.crashlyticsManifestPlaceholder(shouldEnable: Boolean) {
    manifestPlaceholders["crashlyticsCollectionEnabled"] = shouldEnable.toString()
}
