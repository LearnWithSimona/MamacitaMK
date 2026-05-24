plugins {
    alias(libs.plugins.convention.app)
    alias(libs.plugins.convention.firebase)
}

dependencies {
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.core.network)
    implementation(projects.features.babycenter)
    implementation(projects.features.bebesupermarket)
    implementation(projects.features.libertabebecentar)

    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    androidTestImplementation(libs.bundles.tests.android)
    androidTestImplementation(libs.bundles.compose.test)
}
