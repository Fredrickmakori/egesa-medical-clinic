plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.egesa.clinic.desktop.MainKt"
    }
}
