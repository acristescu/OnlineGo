import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.screenshot)
}

android {
  compileSdk {
    version = release(libs.versions.compileSdk.get().toInt()) {
      minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
    }
  }
  ndkVersion = libs.versions.ndkVersion.get()

  experimentalProperties["android.experimental.enableScreenshotTest"] = true

  defaultConfig {
    applicationId = "io.zenandroid.onlinego"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 42
    versionName = "beta_b${versionCode}"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  androidResources {
    localeFilters += listOf("en", "ca", "de", "es", "fr", "nl", "pt", "ro", "sk")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      manifestPlaceholders["crashlyticsCollectionEnabled"] = true
      manifestPlaceholders["analyticsCollectionEnabled"] = true
      configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
        mappingFileUploadEnabled = true
      }
    }
    debug {
      applicationIdSuffix = ".debug"
      manifestPlaceholders["crashlyticsCollectionEnabled"] = false
      manifestPlaceholders["analyticsCollectionEnabled"] = false
    }
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
        "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        "-opt-in=kotlin.concurrent.atomics.ExperimentalAtomicApi",
        "-Xjvm-default=all"
      )
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
  }

  namespace = "io.zenandroid.onlinego"

  externalNativeBuild {
    cmake {
      path = file("CMakeLists.txt")
      version = libs.versions.cmake.get()
    }
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
    resources.excludes.add("META-INF/*")
  }
}

dependencies {
  implementation(project(":shared"))

  // Compose BOM - manages all Compose library versions
  implementation(platform(libs.compose.bom))
  androidTestImplementation(platform(libs.compose.bom))

  // Compose
  implementation(libs.bundles.compose)
  debugImplementation(libs.compose.ui.tooling)
  implementation(libs.compose.material3)

  // Compose Preview Screenshot Testing
  screenshotTestImplementation(platform(libs.compose.bom))
  screenshotTestImplementation(libs.screenshot.validation.api)
  screenshotTestImplementation(libs.compose.ui.tooling)

  // Datastore
  implementation(libs.androidx.datastore.preferences)

  // Android Core
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)

  // Google Play Services
  implementation(libs.play.services.auth)
  implementation(libs.play.review.ktx)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // Work Manager
  implementation(libs.bundles.work)

  // Dependency Injection
  implementation(libs.bundles.koin)

  // Image Loading
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  implementation(libs.navigation.compose)

  // Material (provides Theme.Material3.* XML theme attributes)
  implementation(libs.material)

  // Utility Libraries
  implementation(libs.immutable.collections)
  implementation(libs.kermit)
  implementation(libs.kotlinx.datetime)
  implementation(libs.billing.ktx)

  // Core Library Desugaring
  coreLibraryDesugaring(libs.desugar.jdk.libs)

  // Testing Dependencies
  implementation(libs.espresso.idling.resource)

  // Unit Testing
  testImplementation(libs.bundles.testing)

  // Android Testing
  androidTestImplementation(libs.bundles.android.testing)
}
