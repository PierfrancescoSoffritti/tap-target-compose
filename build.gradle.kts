// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.jetbrains.kotlin.android) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.gradleNexus.publish)
  alias(libs.plugins.jetbrains.dokka)
  alias(libs.plugins.compose.compiler) apply false
}

apply(from = "./scripts/publish-root.gradle")