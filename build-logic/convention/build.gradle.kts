plugins {
    `kotlin-dsl`
    alias(libs.plugins.android.lint) apply false
}

group = libs.versions.app.version.appId

val supportsAndroidLintPlugin = GradleVersion.current() >= GradleVersion.version("9.3.1")

if (supportsAndroidLintPlugin) {
    apply(plugin = libs.plugins.android.lint.get().pluginId)
} else {
    logger.warn(
        "Skipping ${libs.plugins.android.lint.get().pluginId} for build-logic: " +
            "requires Gradle >= 9.3.1, current is ${GradleVersion.current()}."
    )
}

java {
    toolchain {
        languageVersion.set(libs.versions.app.build.kotlinJVMTarget.map(JavaLanguageVersion::of))
    }
}

dependencies {
    compileOnly(libs.gradlePlugin.android)
    compileOnly(libs.gradlePlugin.android.tools.common)
    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.compose.compiler)
    compileOnly(libs.gradlePlugin.firebase.crashlytics)
    compileOnly(libs.gradlePlugin.firebase.performance)
    compileOnly(libs.gradlePlugin.spotless)
    compileOnly(libs.gradlePlugin.ksp)
    compileOnly(libs.gradlePlugin.room)
    if (supportsAndroidLintPlugin) {
        add("lintChecks", libs.androidx.lint.gradle)
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("appConventionPlugin") {
            id = libs.plugins.convention.app.get().pluginId
            id = "convention.android.app"
            implementationClass = "AppConventionPlugin"
        }

        register("androidConventionComposeApp") {
            id = libs.plugins.convention.compose.app.get().pluginId
            implementationClass = "AppComposeConventionPlugin"
        }

        register("hiltConventionPlugin") {
            id = libs.plugins.convention.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }

        register("kotlinSerializationConventionPlugin") {
            id = libs.plugins.convention.kotlin.serialization.get().pluginId
            implementationClass = "KotlinSerializationConventionPlugin"
        }

        register("libraryConventionPlugin") {
            id = libs.plugins.convention.library.get().pluginId
            implementationClass = "LibraryConventionPlugin"
        }

        register("libraryComposeConventionPlugin") {
            id = libs.plugins.convention.compose.library.get().pluginId
            implementationClass = "LibraryComposeConventionPlugin"
        }

        register("kotlinLibraryConventionPlugin") {
            id = libs.plugins.convention.kotlin.library.get().pluginId
            implementationClass = "KotlinLibraryConventionPlugin"
        }

        register("androidFlavorsConventionPlugin") {
            id = libs.plugins.convention.flavors.get().pluginId
            implementationClass = "FlavorsConventionPlugin"
        }

        register("roomConventionPlugin") {
            id = libs.plugins.convention.room.get().pluginId
            implementationClass = "RoomConventionPlugin"
        }

        register("spotlessConventionPlugin") {
            id = libs.plugins.convention.spotless.get().pluginId
            implementationClass = "SpotlessConventionPlugin"
        }

        register("androidLintConventionPlugin") {
            id = libs.plugins.convention.lint.get().pluginId
            implementationClass = "AndroidLintConventionPlugin"
        }

        register("androidJacocoLibraryConventionPlugin") {
            id = libs.plugins.convention.jacoco.library.get().pluginId
            implementationClass = "AndroidLibraryJacocoConventionPlugin"
        }

        register("androidJacocoApplicationConventionPlugin") {
            id = libs.plugins.convention.jacoco.android.get().pluginId
            implementationClass = "AndroidApplicationJacocoConventionPlugin"
        }

        register("firebaseConventionPlugin") {
            id = libs.plugins.convention.firebase.get().pluginId
            implementationClass = "FirebaseConventionPlugin"
        }
    }
}
