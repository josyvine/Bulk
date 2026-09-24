import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.bulk.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.bulk.app"
    // minSdk set to 26 for runtime InMemoryDexClassLoader support
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // 1. KEYSTORE BASE64 / PATH RESOLUTION
      val base64Keystore = System.getenv("KEYSTORE_BASE64")
      val keystorePathEnv = System.getenv("KEYSTORE_PATH")

      val resolvedKeystore = when {
        // Option A: If Base64 string is provided in env, decode it directly
        !base64Keystore.isNullOrBlank() -> {
          val decodedFile = file("${rootDir}/safevoice.jks")
          decodedFile.writeBytes(Base64.getDecoder().decode(base64Keystore.trim()))
          decodedFile
        }
        // Option B: KEYSTORE_PATH environment variable passed by YAML
        !keystorePathEnv.isNullOrBlank() -> file(keystorePathEnv)
        // Option C: safevoice.jks decoded in workflow workspace
        file("${rootDir}/safevoice.jks").exists() -> file("${rootDir}/safevoice.jks")
        // Option D: Local default fallback
        else -> file("${rootDir}/my-upload-key.jks")
      }

      storeFile = resolvedKeystore

      // 2. KEYSTORE PASSWORD (matches YAML KEYSTORE_PASSWORD)
      storePassword = System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("STORE_PASSWORD")

      // 3. KEY ALIAS (matches YAML KEY_ALIAS with default fallback "bulk")
      keyAlias = System.getenv("KEY_ALIAS") ?: "bulk"

      // 4. KEY PASSWORD (matches YAML KEY_PASSWORD with fallback to KEYSTORE_PASSWORD)
      keyPassword = System.getenv("KEY_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")
        ?: System.getenv("STORE_PASSWORD")
    }

    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
      isDebuggable = true
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = false
    buildConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.recyclerview:recyclerview:1.4.0")
  implementation("androidx.constraintlayout:constraintlayout:2.2.1")
  implementation(libs.play.services.ads)
}