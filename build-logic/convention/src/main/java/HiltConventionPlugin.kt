import com.android.build.gradle.api.AndroidBasePlugin
import com.apollo.app.convention.getLibrary
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("ksp"))

            dependencies {
                "ksp"(libs.getLibrary("hilt-compiler"))
                "ksp"(libs.getLibrary("kotlin.metadata"))
                "testImplementation"(libs.getLibrary("hilt-android-testing"))
                if (target.projectDir.resolve("src/androidTest").exists()) {
                    "androidTestImplementation"(libs.getLibrary("hilt-android-testing"))
                }
                "kspTest"(libs.getLibrary("hilt-compiler"))
            }

            // Add support for Jvm Module, based on org.jetbrains.kotlin.jvm
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.getLibrary("hilt-core"))
                }
            }

            /** Add support for Android modules, based on [AndroidBasePlugin] */
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = libs.getPluginId("hilt"))
                dependencies {
                    "implementation"(libs.getLibrary("hilt-android"))
                }
            }
        }
    }
}
