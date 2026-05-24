import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.apollo.app.convention.addLibrariesConfig
import com.apollo.app.convention.commonVersioning
import com.apollo.app.convention.configureBuildFeatures
import com.apollo.app.convention.configureJavaCompatibilityCompileOptions
import com.apollo.app.convention.configureKotlin
import com.apollo.app.convention.configureLibraryAndTestNameSpace
import com.apollo.app.convention.configurePrintApksTask
import com.apollo.app.convention.disableUnnecessaryAndroidTests
import com.apollo.app.convention.flavors.configureFlavors
import com.apollo.app.convention.getLibrary
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal class LibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("android-library"))
            apply(plugin = libs.getPluginId("convention-lint"))
            if (!target.projectDir.absolutePath.contains("/sdk/")) {
                apply(plugin = libs.getPluginId("convention-spotless"))
            }

            configureLibraryAndTestNameSpace()

            extensions.configure<LibraryExtension> {
                configureKotlin<KotlinAndroidProjectExtension>()
                commonVersioning(this)
                configureBuildFeatures()
                configureFlavors(commonExtension = this)
                addLibrariesConfig()
                configureJavaCompatibilityCompileOptions(this)
            }

            extensions.configure<LibraryAndroidComponentsExtension> {
                configurePrintApksTask(this)
                disableUnnecessaryAndroidTests(target)
            }

            dependencies {
                if (target.projectDir.resolve("src/androidTest").exists()) {
                    "androidTestImplementation"(libs.getLibrary("kotlin-test"))
                }
                "testImplementation"(libs.getLibrary("kotlin-test"))
                "testImplementation"(libs.getLibrary("unit-test-junit"))

                "implementation"(libs.getLibrary("androidx-tracing"))
            }
        }
    }
}
