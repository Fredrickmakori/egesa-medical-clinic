plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

tasks.register("prepareKotlinBuildScriptModel") {}
tasks.register("prepareKotlinIdeaImport") {}

kotlin {
    jvmToolchain(21)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(libs.androidx.activity.compose)
}
