import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.apollo.app.convention.commonVersioning
import com.apollo.app.convention.configureAppPluginPackageAndNameSpace
import com.apollo.app.convention.configureBuildFeatures
import com.apollo.app.convention.configureJavaCompatibilityCompileOptions
import com.apollo.app.convention.configureKotlin
import com.apollo.app.convention.configurePrintApksTask
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal class AppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("android-application"))
            apply(plugin = libs.getPluginId("convention-compose-app"))
            apply(plugin = libs.getPluginId("convention-hilt"))
            apply(plugin = libs.getPluginId("convention-flavors"))
            apply(plugin = libs.getPluginId("convention-spotless"))
            apply(plugin = libs.getPluginId("convention-lint"))

            extensions.configure<ApplicationExtension> {
                testOptions.animationsDisabled = true

                configureKotlin<KotlinAndroidProjectExtension>()
                commonVersioning(this)
                configureAppPluginPackageAndNameSpace(this)
                configureBuildFeatures()
                configureJavaCompatibilityCompileOptions(this)
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                configurePrintApksTask(this)
            }
        }
    }
}
