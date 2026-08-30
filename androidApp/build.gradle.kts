import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

// バージョンはビルド日付 (yyyy.MM.dd / yyyyMMdd) — ユーザ要望: 日付ベース
val buildDate: LocalDate = LocalDate.now()
val appVersionName: String = buildDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
val appVersionCode: Int = buildDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt()

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":composeApp"))
    implementation(project(":core"))
    implementation(project(":ytdlpAndroid"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "uk.rumia_ch.videodrop"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "uk.rumia_ch.videodrop"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
