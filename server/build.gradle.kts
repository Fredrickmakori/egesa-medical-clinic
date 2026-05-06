plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.egesa"
version = "0.1.0"

application {
    mainClass.set("com.egesa.clinic.server.ServerKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
}
