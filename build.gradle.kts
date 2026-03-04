plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
}

allprojects {
    group = "cat.mona.kittin"
    version = "1.0.0"
}
