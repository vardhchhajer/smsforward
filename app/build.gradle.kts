plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.smsforwarderpro"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.smsforwarderpro"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.example.smsforwarderpro"
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("SMS_FORWARDER_KEYSTORE").orNull
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("SMS_FORWARDER_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("SMS_FORWARDER_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("SMS_FORWARDER_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true && !releaseSigning.keyAlias.isNullOrBlank()) {
                signingConfig = releaseSigning
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/LICENSE*"
        excludes += "/META-INF/NOTICE*"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Dagger Hilt
  implementation(libs.hilt.android)
  "ksp"(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.hilt.work)
  "ksp"(libs.androidx.hilt.compiler)

  // Room Database
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  "ksp"(libs.room.compiler)

  // OkHttp & Retrofit
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.okhttp.logging.interceptor)

  // Jetpack Security
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.biometric)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

  // Google Fonts
  implementation(libs.androidx.compose.ui.text.google.fonts)

  // Material 3 Adaptive Layouts
  implementation(libs.androidx.compose.material3.adaptive)
  implementation(libs.androidx.compose.material3.adaptive.layout)
  implementation(libs.androidx.compose.material3.adaptive.navigation)

  // AppAuth for OAuth 2.0
  implementation("net.openid:appauth:0.11.1")

  // SQLCipher for Room Database Encryption
  implementation("net.zetetic:android-database-sqlcipher:4.5.4")
  implementation("androidx.sqlite:sqlite-framework:2.4.0")
}

tasks.register<Copy>("copyApkWithVersion") {
    val vName = android.defaultConfig.versionName ?: "1.0"
    val vCode = android.defaultConfig.versionCode ?: 1
    val buildDir = layout.buildDirectory
    from(buildDir.dir("outputs/apk/debug"))
    include("app-debug.apk")
    into(buildDir.dir("outputs/apk/renamed"))
    rename { "SMSForwarderPro-v${vName}-c${vCode}-debug.apk" }
}

tasks.configureEach {
    if (name == "assembleDebug") {
        finalizedBy("copyApkWithVersion")
    }
}
