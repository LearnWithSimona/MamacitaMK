plugins {
    alias(libs.plugins.convention.library)
    alias(libs.plugins.convention.compose.library)
    alias(libs.plugins.convention.hilt)
}

dependencies {
    api(projects.domain)
    implementation(projects.data)
    implementation(projects.core.uiState)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    debugImplementation(libs.bundles.compose.debug)
}
