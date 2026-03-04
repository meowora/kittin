pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kittin"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
