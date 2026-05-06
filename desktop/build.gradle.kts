plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            @Suppress("DEPRECATION")
            implementation(compose.material3)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.egesa.clinic.desktop.MainKt"
    }
}
