import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.molecule)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndkVersion.get()
    
    defaultConfig {
        applicationId = "io.zenandroid.onlinego"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 42
        versionName = "alpha_b${versionCode}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        buildConfigField("String", "BASE_URL", "\"https://online-go.com\"")
    }
    
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
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
        kotlinOptions {
            freeCompilerArgs += "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
            freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
            freeCompilerArgs += "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            freeCompilerArgs += "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
            freeCompilerArgs += "-Xjvm-default=all"
        }
    }
    
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
    }
    
    composeCompiler {
        enableStrongSkippingMode = true
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
    }
    
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }
    
    namespace = "io.zenandroid.onlinego"
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Compose BOM - manages all Compose library versions
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    // Compose
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)

    // Accompanist
    implementation(libs.bundles.accompanist)

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    // Google Play Services
    implementation(libs.play.services.auth)

    // Networking
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.persistent.cookie.jar)

    // Serialization
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)

    // Reactive Programming
    implementation(libs.bundles.rx)

    // Work Manager
    implementation(libs.bundles.work)

    // Room Database
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Dependency Injection
    implementation(libs.bundles.koin)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // UI & Material Design
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.legacy.preference.v14)
    implementation(libs.material.progress.bar)

    // Utility Libraries
    implementation(libs.immutable.collections)
    implementation(libs.markwon)
    implementation(libs.jsoup)
    implementation(libs.billing.ktx)
    implementation(libs.bubble.view)
    implementation(libs.mp.android.chart)
    implementation(libs.socket.io.client) {
        // excluding org.json which is provided by Android
        exclude(group = "org.json", module = "json")
    }

    // Development & Debugging
    implementation(libs.stetho)
    implementation(libs.stetho.okhttp3)

    // UI Libraries
    implementation(libs.groupie)
    implementation(libs.groupie.viewbinding)

    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Testing Dependencies
    implementation(libs.espresso.idling.resource)

    // Unit Testing
    testImplementation(libs.bundles.testing)

    // Android Testing
    androidTestImplementation(libs.bundles.android.testing)
}
