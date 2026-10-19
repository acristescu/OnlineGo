// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kotlin.multiplatform.library) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ktorfit) apply false
  alias(libs.plugins.room) apply false
}

tasks.register<Delete>("clean") {
  delete(rootProject.buildDir)
} 