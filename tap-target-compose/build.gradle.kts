plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.psoffritti.taptargetcompose"
  compileSdk = 34

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
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
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.4"
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
  implementation(composeBom)
  androidTestImplementation(composeBom)
  implementation("androidx.compose.animation:animation")
  implementation("androidx.compose.foundation:foundation")

  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.ui:ui-tooling")
}