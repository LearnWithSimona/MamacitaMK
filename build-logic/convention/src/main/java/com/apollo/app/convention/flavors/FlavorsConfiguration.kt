package com.apollo.app.convention.flavors

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.kotlin.dsl.invoke

fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: AppFlavor) -> Unit = {},
) {
    commonExtension.apply {
        AppDimension.entries.forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            AppFlavor.entries.forEach { flavor ->
                create(flavor.name) {
                    dimension = flavor.dimension.name
                    flavorConfigurationBlock(this, flavor)

                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        flavor.applicationIdSuffix?.let { applicationIdSuffix = it }
                    }
                }
            }
        }
    }
}

