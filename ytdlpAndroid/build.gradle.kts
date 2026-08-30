import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.chaquopy)
}
android {
    namespace = "uk.rumia_ch.videodrop.ytdlp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        ndk {
            abiFilters += "arm64-v8a"
        }

        // Required for native .so that are actually PIE executables
        // to be extracted as real files accessible via nativeLibraryDir
        packaging {
            // AGP 8+ uses packaging.jniLibs.useLegacyPackaging true to extract
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // no compose needed here
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        // Use python3 from setup-python (3.13) — avoids "Couldn't find Python 3.13" on CI
        buildPython = "python3"
        pip {
            // Generated from config/upstream-versions.json — do not edit manually
            install("yt-dlp==2026.08.19")
            install("yt-dlp-ejs==0.8.0")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.datastore.preferences)
}
