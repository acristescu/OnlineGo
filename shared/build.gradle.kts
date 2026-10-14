import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.ktorfit)
}

android {
  namespace = "io.zenandroid.onlinego.shared"
  compileSdk = libs.versions.compileSdk.get().toInt()
  ndkVersion = libs.versions.ndkVersion.get()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
  }

  ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }

  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
      freeCompilerArgs.addAll(
        "-opt-in=kotlin.concurrent.atomics.ExperimentalAtomicApi",
        "-Xjvm-default=all"
      )
    }
  }

  buildFeatures {
    buildConfig = true
  }

  composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
  }

  externalNativeBuild {
    cmake {
      path = file("CMakeLists.txt")
      version = libs.versions.cmake.get()
    }
  }
}

dependencies {
  api(platform(libs.compose.bom))
  api(libs.compose.runtime)

  api(libs.kotlinx.serialization.json)
  api(libs.immutable.collections)
  api(libs.kotlinx.datetime)
  api(libs.kermit)
  api(libs.koin.core)

  api(libs.ktorfit.lib)
  api(libs.ktor.client.content.negotiation)
  api(libs.ktor.serialization.kotlinx.json)
  api(libs.okhttp.logging.interceptor)

  api(libs.bundles.room)
  ksp(libs.room.compiler)
  api(libs.androidx.datastore.preferences)

  implementation(libs.androidx.core.ktx)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)

  coreLibraryDesugaring(libs.desugar.jdk.libs)

  testImplementation(libs.junit)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.ktor.client.mock)
  testImplementation(libs.ktor.client.okhttp)
}
