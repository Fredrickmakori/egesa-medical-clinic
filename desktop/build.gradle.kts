plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            @Suppress("DEPRECATION")
            implementation(compose.material3)
            implementation(libs.logback.classic)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.egesa.clinic.desktop.MainKt"
    }
}


kotlin {
    jvmToolchain(21)
}
