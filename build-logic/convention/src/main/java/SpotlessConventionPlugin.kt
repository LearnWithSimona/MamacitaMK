import com.diffplug.gradle.spotless.SpotlessExtension
import com.apollo.app.convention.getPluginId
import com.apollo.app.convention.getVersion
import com.apollo.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.getPluginId("spotless"))

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/*.kt")

                    // Exclude files in the build directory and resources
                    targetExclude(
                        "${layout.buildDirectory.asFile.get()}/**/*.kt",
                        "**/resources/**/*.kt",
                        "sdk/**/*.kt",
                    )

                    // Use ktlint and custom .editorconfig
                    ktlint(libs.getVersion(KT_LINT_LIB_NAME))
                        .editorConfigOverride(mapOf("android" to "true"),)
                        .setEditorConfigPath("${project.rootDir}/.editorconfig")

                    // Allow toggling Spotless off and on within code files using comments
                    toggleOffOn()
                }
                format("kts") {
                    target("**/*.kts")
                    targetExclude(
                        "**/build/**/*.kts",
                        "**/resources/**/*.kts",
                        "sdk/**/*.kts",
                    )
                }
                format("xml") {
                    target("**/*.xml")
                    targetExclude(
                        "**/build/**/*.xml",
                        "**/resources/**/*.xml",
                        "sdk/**/*.xml",
                    )
                }
            }
        }
    }

    companion object {
        private const val KT_LINT_LIB_NAME = "ktlint"
    }
}
