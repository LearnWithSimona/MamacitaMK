package com.apollo.app.convention.flavors

@Suppress("EnumEntryName")
enum class AppFlavor(
    val dimension: AppDimension,
    val olympusBaseUrl: String,
    val applicationIdSuffix: String? = null,
) {
    prod(
        dimension = AppDimension.environment,
        olympusBaseUrl = "app-api.backstage.britbox.com",
    ),
    staging(
        dimension = AppDimension.environment,
        olympusBaseUrl = "app-api.backstage-staging.britbox.com",
        applicationIdSuffix = ".staging",
    ),
    dev(
        dimension = AppDimension.environment,
        olympusBaseUrl = "app-api.backstage-dev.britbox.com",
        applicationIdSuffix = ".dev",
    ),
    qa(
        dimension = AppDimension.environment,
        olympusBaseUrl = "app-api.backstage-test.britbox.com",
        applicationIdSuffix = ".qa",
    ),
}
