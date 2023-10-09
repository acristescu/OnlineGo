plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.zenandroid.onlinego.engine.katago"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        externalNativeBuild {
            cmake {
                arguments(
                    "-DUSE_BACKEND=EIGEN",
                    "-DANDROID_ARM_NEON=TRUE",
                    "-DEigen3_DIR=/usr/share/eigen3/cmake/",
                )
                cFlags("-fexceptions", "-frtti")
                cppFlags(
                    "-D__STDC_FORMAT_MACROS",
                    "-DBYTE_ORDER=LITTLE_ENDIAN")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    externalNativeBuild {
        cmake {
            path("src/cpp/CMakeLists.txt")
            version = "3.22.1"
            buildStagingDirectory = file("${buildDir}/bin")
        }
    }
}

dependencies {
  //implementation("androidx.core:core-ktx:1.9.0")
  //implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
  //implementation("androidx.appcompat:appcompat:1.6.1")
  //implementation("com.google.android.material:material:1.10.0")
  //testImplementation("junit:junit:4.13.2")
  //androidTestImplementation("androidx.test.ext:junit:1.1.5")
  //androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
