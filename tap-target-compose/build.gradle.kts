import com.psoffritti.taptargetcompose.Configuration

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.psoffritti.taptargetcompose"
  compileSdk = Configuration.compileSdk

  defaultConfig {
    minSdk = Configuration.minSdk

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
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
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.foundation)

  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui.tooling)
}

extra.apply {
  set("PUBLISH_GROUP_ID", "com.pierfrancescosoffritti.taptargetcompose")
  set("PUBLISH_ARTIFACT_ID", "core")
  set("PUBLISH_VERSION", Configuration.versionName)
  set("PUBLISH_DESCRIPTION", "A compose implementation of Material Design tap targets, for feature discovery.")
}

apply(from = "../scripts/publish-module.gradle")