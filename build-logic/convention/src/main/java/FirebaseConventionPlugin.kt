import com.android.build.api.dsl.ApplicationExtension
import com.apollo.app.convention.enableCrashlytics
import com.apollo.app.convention.getLibrary
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.googleServicesJsonExists
import com.apollo.app.convention.isReleaseBuild
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class FirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val hasGoogleServicesJson = googleServicesJsonExists
            if (hasGoogleServicesJson) {
                apply(plugin = libs.getPluginId("gms"))
            } else {
                logger.lifecycle(
                    "google-services.json not found in ${project.path}. " +
                        "Skipping Google Services plugin for local build.",
                )
            }
            apply(plugin = libs.getPluginId("crashlytics"))
            apply(plugin = libs.getPluginId("performance"))

            // protolite-well-known-types is superseded by protobuf-javalite; exclude to avoid duplicate class conflict
            configurations.configureEach {
                exclude(mapOf("group" to "com.google.firebase", "module" to "protolite-well-known-types"))
            }

            dependencies {
                "implementation"(platform(libs.getLibrary("firebase-bom")))
                "implementation"(libs.getLibrary("firebase-analytics"))
                "implementation"(libs.getLibrary("firebase-performance"))
                "implementation"(libs.getLibrary("firebase-crashlytics"))
                "implementation"(libs.getLibrary("firebase-remote-config"))
            }

            extensions.configure<ApplicationExtension> {
                buildTypes.configureEach {
                    val isGoogleServicesEnabled = isReleaseBuild && hasGoogleServicesJson
                    enableCrashlytics(isGoogleServicesEnabled)
                }
            }
        }
    }
}
