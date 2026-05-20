import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "di"
            isStatic = true
        }
    }


    sourceSets {
        androidMain.dependencies {
        }
        iosMain.dependencies {
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(project(":core:data"))
            implementation(project(":core:domain"))
            implementation(project(":core:datasource:local"))
            implementation(libs.sqldelight.runtime)
//            implementation(project(":feature:auth"))
//            implementation(project(":feature:home"))
//            implementation(project(":feature:profile"))
//            implementation(project(":feature:manageProduct"))
//            implementation(project(":feature:adminPanel"))
//            implementation(project(":feature:productOverview"))
//            implementation(project(":feature:details"))
//            implementation(project(":feature:cart"))
//            implementation(project(":feature:categoryProducts"))
//            implementation(project(":feature:checkout"))
        }
    }
}

android {
    namespace = "com.example.di"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}