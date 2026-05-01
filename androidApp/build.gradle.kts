plugins {
    id("com.android.application")
    kotlin("android") version "2.0.21"
    id("org.jetbrains.compose")
}

android {
    namespace = "com.egesa.clinic.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.egesa.clinic.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation("androidx.activity:activity-compose:1.9.3")
}
