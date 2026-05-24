plugins {
    alias(libs.plugins.convention.library)
    alias(libs.plugins.convention.kotlin.serialization)
}

android {
    lint {
        disable += "UElementAsPsi"
    }
}

dependencies {
    api(libs.bundles.ktor)
    implementation(libs.timber)

    testImplementation(libs.bundles.tests.unit)
}
