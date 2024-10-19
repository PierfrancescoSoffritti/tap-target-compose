package com.psoffritti.taptargetcompose

object Configuration {
  const val compileSdk = 35
  const val targetSdk = 35
  const val minSdk = 13
  const val minSdkSampleApp = 21
  private const val majorVersion = 1
  private const val minorVersion = 2
  private const val patchVersion = 0
  const val versionCode = 3
  const val versionName = "$majorVersion.$minorVersion.$patchVersion"
  const val snapshotVersionName = "$majorVersion.$minorVersion.${patchVersion + 1}-SNAPSHOT"
}