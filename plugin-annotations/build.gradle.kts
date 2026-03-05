plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.binary.compatibility.validator)
    `maven-publish`
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("mrow") {
            from(components["java"])
        }
    }
}
