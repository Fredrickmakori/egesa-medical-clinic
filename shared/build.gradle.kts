plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            }
        }
    }
}

android {
    namespace = "com.egesa.clinic.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
}
