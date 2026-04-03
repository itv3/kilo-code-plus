plugins {
    alias(libs.plugins.kotlin)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellij.platform)
        bundledModule("intellij.platform.frontend")
    }

    implementation(project(":shared"))
}
