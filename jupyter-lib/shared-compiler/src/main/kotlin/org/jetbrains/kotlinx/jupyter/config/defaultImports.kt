package org.jetbrains.kotlinx.jupyter.config

import kotlin.script.experimental.dependencies.RepositoryCoordinates

val defaultRepositories = arrayOf(
    "https://jcenter.bintray.com/",
    "https://repo.maven.apache.org/maven2/",
    "https://jitpack.io/",
).map(::RepositoryCoordinates)

val defaultGlobalImports = listOf(
    "kotlin.math.*",
    "org.jetbrains.kotlinx.jupyter.api.*",
    "org.jetbrains.kotlinx.jupyter.api.libraries.*",
)
