import com.apollo.app.convention.getLibrary
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal class KotlinSerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("kotlin-serialization"))

            dependencies {
                "implementation"(libs.getLibrary("kotlin-serialization-json"))
            }
        }
    }
}
