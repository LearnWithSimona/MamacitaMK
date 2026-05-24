import com.android.build.api.dsl.ApplicationExtension
import com.apollo.app.convention.flavors.configureFlavors
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal class FlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<ApplicationExtension> {
                configureFlavors(commonExtension = this) { flavor ->
                    buildConfigField(
                        "String",
                        "OLYMPUS_BASE_URL",
                        "\"${flavor.olympusBaseUrl}\"",
                    )
                }
            }
        }
    }
}
