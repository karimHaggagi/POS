import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
//    alias(libs.plugins.composeMultiplatform)
//    alias(libs.plugins.composeCompiler)
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
            baseName = "data"
            isStatic = true
        }
    }


    sourceSets {
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.13.1")

        }
        iosMain.dependencies {
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)
            implementation(project(":core:domain"))
            implementation(project(":core:model"))
            implementation(project(":core:datasource:local"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.example.data"
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