import com.android.build.api.dsl.LibraryExtension
import com.apollo.app.convention.configureAndroidCompose
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class LibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("kotlin-compose"))

            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidCompose(extension)
        }
    }
}
