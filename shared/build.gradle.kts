import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.ktorfit)
  alias(libs.plugins.room)
}

kotlin {
  android {
    namespace = "io.zenandroid.onlinego.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xjvm-default=all")
    }

    withHostTest {
      isReturnDefaultValues = true
    }
  }

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    all {
      languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }

    commonMain.dependencies {
      api(project.dependencies.platform(libs.compose.bom))
      api(libs.compose.runtime)
      api(libs.kotlinx.serialization.json)
      api(libs.immutable.collections)
      api(libs.kotlinx.datetime)
      api(libs.kermit)
      api(libs.room.runtime)
      implementation(libs.androidx.collection)
    }

    androidMain.dependencies {
      api(libs.koin.core)

      api(libs.ktorfit.lib)
      api(libs.ktor.client.content.negotiation)
      api(libs.ktor.serialization.kotlinx.json)
      api(libs.okhttp.logging.interceptor)

      api(libs.androidx.datastore.preferences)

      implementation(libs.androidx.core.ktx)

      implementation(project.dependencies.platform(libs.firebase.bom))
      implementation(libs.firebase.analytics)
      implementation(libs.firebase.crashlytics)
    }

    iosMain.dependencies {
      implementation(libs.sqlite.bundled)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.coroutines.test)
    }

    getByName("androidHostTest").dependencies {
      implementation(libs.junit)
      implementation(libs.ktor.client.mock)
      implementation(libs.ktor.client.okhttp)
    }
  }
}

dependencies {
  add("kspAndroid", libs.room.compiler)
  add("kspIosArm64", libs.room.compiler)
  add("kspIosSimulatorArm64", libs.room.compiler)
}

room {
  schemaDirectory("$projectDir/schemas")
}

ksp {
  arg("room.incremental", "true")
  arg("room.expandProjection", "true")
}

composeCompiler {
  stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}
