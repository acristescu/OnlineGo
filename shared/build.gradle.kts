import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.ktorfit)
  alias(libs.plugins.room)
}

kotlin {
  android {
    namespace = "io.zenandroid.onlinego.shared"
    compileSdk {
      version = release(libs.versions.compileSdk.get().toInt()) {
        minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
      }
    }
    minSdk = libs.versions.minSdk.get().toInt()

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xjvm-default=all")
    }

    androidResources {
      enable = true
    }

    withHostTest {
      isReturnDefaultValues = true
    }
  }

  listOf(
    iosArm64() to EstimatorTarget("iphoneos", "arm64-apple-ios15.0"),
    iosSimulatorArm64() to EstimatorTarget("iphonesimulator", "arm64-apple-ios15.0-simulator"),
  ).forEach { (target, estimatorTarget) -> target.estimatorInterop(estimatorTarget) }

  sourceSets {
    all {
      languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }

    commonMain.dependencies {
      api(libs.cmp.runtime)
      api(libs.cmp.ui)
      api(libs.cmp.foundation)
      api(libs.cmp.material3)
      api(libs.cmp.resources)
      api(libs.kotlinx.serialization.json)
      api(libs.immutable.collections)
      api(libs.kotlinx.datetime)
      api(libs.kermit)
      api(libs.room.runtime)
      implementation(libs.androidx.collection)

      api(libs.ktorfit.lib)
      api(libs.ktor.client.content.negotiation)
      api(libs.ktor.serialization.kotlinx.json)

      api(libs.androidx.datastore.preferences)

      api(libs.koin.core)
    }

    androidMain.dependencies {
      api(libs.okhttp.logging.interceptor)
      implementation(libs.ktor.client.okhttp)

      implementation(libs.androidx.core.ktx)

      implementation(project.dependencies.platform(libs.firebase.bom))
      implementation(libs.firebase.analytics)
      implementation(libs.firebase.crashlytics)
    }

    iosMain.dependencies {
      implementation(libs.sqlite.bundled)
      implementation(libs.ktor.client.darwin)
    }

    iosTest.dependencies {
      implementation(libs.cmp.ui.test)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.coroutines.test)
      implementation(libs.ktor.client.mock)
    }

    getByName("androidHostTest").dependencies {
      implementation(libs.junit)
      implementation(libs.ktor.client.okhttp)
    }
  }
}

data class EstimatorTarget(val sdk: String, val triple: String)

abstract class CompileEstimator @Inject constructor(private val exec: ExecOperations) :
  DefaultTask() {
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sources: DirectoryProperty

  @get:Input
  abstract val sdk: Property<String>

  @get:Input
  abstract val triple: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun compile() {
    val objectFile = outputDir.file("estimator.o").get().asFile
    exec.exec {
      commandLine(
        "xcrun", "--sdk", sdk.get(), "clang++", "-std=c++11", "-O3", "-DNDEBUG",
        "-target", triple.get(),
        "-c", sources.file("estimator.cpp").get().asFile.path,
        "-o", objectFile.path,
      )
    }
    exec.exec {
      commandLine(
        "xcrun", "--sdk", sdk.get(), "libtool", "-static",
        "-o", outputDir.file("libestimator.a").get().asFile.path,
        objectFile.path,
      )
    }
  }
}

fun KotlinNativeTarget.estimatorInterop(estimatorTarget: EstimatorTarget) {
  val estimatorSources = layout.projectDirectory.dir("src/nativeInterop/cpp")
  val libraryDir = layout.buildDirectory.dir("estimator/$targetName")
  val compileEstimator =
    tasks.register<CompileEstimator>("compileEstimator${targetName.replaceFirstChar { it.uppercase() }}") {
      sources.set(estimatorSources)
      sdk.set(estimatorTarget.sdk)
      triple.set(estimatorTarget.triple)
      outputDir.set(libraryDir)
    }
  compilations.getByName("main").cinterops.create("estimator") {
    definitionFile.set(layout.projectDirectory.file("src/nativeInterop/cinterop/estimator.def"))
    includeDirs(estimatorSources)
    extraOpts("-libraryPath", libraryDir.get().asFile.path)
    tasks.named(interopProcessingTaskName) { dependsOn(compileEstimator) }
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

compose.resources {
  publicResClass = true
  packageOfResClass = "io.zenandroid.onlinego.resources"
}

composeCompiler {
  stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}
