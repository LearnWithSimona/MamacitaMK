import com.apollo.app.convention.configureJava
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.getVersion
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

internal class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("java-library"))
            apply(plugin = libs.getPluginId("kotlin-jvm"))
            apply(plugin = libs.getPluginId("convention-lint"))

            configureJava {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(libs.getVersion("app-build-kotlinJVMTarget")))
                }
            }
            dependencies {
                "testImplementation"(kotlin("test"))
            }
        }
    }
}
