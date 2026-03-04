@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.binary.compatibility.validator)
}

kotlin {
    explicitApi()

    jvm()

    applyDefaultHierarchyTemplate()
}
