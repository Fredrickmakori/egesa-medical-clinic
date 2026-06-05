@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget {
    }
    jvm()

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "shared.js"
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    open = true
                    // Disable overlay to prevent ReferenceError: document is not defined in Web Workers
                    client = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer.Client(
                        overlay = false
                    )
                }
            }
        }
        binaries.executable()
    }

    // iOS targets require a macOS host for building/running.
    // On Windows/Linux, keeping these enabled can break IDE import/sync (appleMain/iosMain resolvers).
    val enableIos = (findProperty("enableIos") as? String)?.equals("true", ignoreCase = true) == true
    val isMacHost = System.getProperty("os.name").contains("Mac", ignoreCase = true)
    if (enableIos || isMacHost) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        if (name.startsWith("ios")) {
            binaries.framework {
                baseName = "shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        val mobileMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)
        findByName("iosMain")?.dependsOn(mobileMain)

        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.async)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(compose.runtime)
            implementation(compose.foundation)
            @Suppress("DEPRECATION") implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
        }
        findByName("iosMain")?.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.sqldelight.web.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("ClinicDatabase") {
            packageName.set("com.egesa.clinic.shared.db")
            generateAsync.set(true)
        }
    }
}

android {
    namespace = "com.egesa.clinic.shared"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
