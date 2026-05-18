plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.egesa.clinic.desktop.MainKt"
        jvmArgs += listOf("-Xmx2G")
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "EgesaClinic"
            packageVersion = "1.0.0"
        }
    }
}
