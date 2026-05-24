plugins {
    alias(libs.plugins.convention.library)
    alias(libs.plugins.convention.kotlin.serialization)
    alias(libs.plugins.convention.hilt)
}

dependencies {
    api(projects.domain)
    implementation(projects.core.network)

    implementation(libs.bundles.ktor)
    implementation(libs.ksoup)
    implementation(libs.timber)
    implementation(libs.bundles.coroutines)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    testImplementation(libs.bundles.tests.unit)
}
