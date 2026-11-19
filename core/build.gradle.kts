plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example.ironlog.core"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
}
