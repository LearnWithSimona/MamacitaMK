package com.apollo.app.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.getPluginId(pluginName: String): String =
    findPlugin(pluginName).get().get().pluginId

internal fun VersionCatalog.getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

internal fun VersionCatalog.getBundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    findBundle(alias).get()

internal fun VersionCatalog.getVersion(alias: String): String =
    findVersion(alias).get().toString()
