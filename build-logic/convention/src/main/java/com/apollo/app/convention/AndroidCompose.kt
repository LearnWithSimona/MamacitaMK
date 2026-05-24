package com.apollo.app.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
        }

        dependencies {
            "implementation"(libs.getBundle("compose"))
            "implementation"(libs.getBundle("compose-animations"))
            "implementation"(libs.getBundle("compose-graphics"))
            "implementation"(libs.getBundle("coil"))
            "implementation"(libs.getLibrary("kotlin-immutable-collections"))
            "debugImplementation"(libs.getBundle("compose-debug"))
            if (this@configureAndroidCompose.projectDir.resolve("src/androidTest").exists()) {
                "androidTestImplementation"(libs.getBundle("compose-test"))
            }
            "lintChecks"(libs.getLibrary("lints-compose"))
        }
    }

    composeCompiler {
        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }


        fun Provider<*>.relativeToRootProject(dir: String) = map {
            @Suppress("UnstableApiUsage")
            isolated.rootProject.projectDirectory
                .dir("build")
                .dir(projectDir.toRelativeString(rootDir))
        }.map { it.dir(dir) }

        providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
            .relativeToRootProject("compose-metrics")
            .let(metricsDestination::set)

        providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
            .relativeToRootProject("compose-reports")
            .let(reportsDestination::set)

        @Suppress("UnstableApiUsage")
        stabilityConfigurationFiles.add(
            isolated.rootProject.projectDirectory.file(
                "compose_compiler_stability_config.conf",
            ),
        )
    }
}
