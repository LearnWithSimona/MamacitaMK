package com.apollo.app.convention

import com.android.build.api.dsl.CommonExtension

internal fun CommonExtension.configureBuildFeatures() {
    buildFeatures.apply {
        shaders = false
        buildConfig = true
    }
}
