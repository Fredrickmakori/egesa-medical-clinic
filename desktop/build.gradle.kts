plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":shared"))
}

compose.desktop {
    application {
        mainClass = "com.egesa.clinic.desktop.MainKt"
    }
}
