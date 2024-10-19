import com.psoffritti.taptargetcompose.Configuration
import org.gradle.kotlin.dsl.android

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.psoffritti.taptargetcompose"
  compileSdk = Configuration.compileSdk

  defaultConfig {
    applicationId = "com.psoffritti.taptargetcompose"
    minSdk = Configuration.minSdkSampleApp
    targetSdk = Configuration.targetSdk
    versionCode = Configuration.versionCode
    versionName = Configuration.versionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(project(":tap-target-compose"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui.tooling)
}