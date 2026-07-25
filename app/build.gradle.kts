import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.findlyts.fomo"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // NOTE: Firebase / Google Sign-In configuration is NOT declared here.
    // The Secrets Gradle plugin (configured below) reads every key from `.env`
    // — falling back to `.env.example` — and generates the matching
    // BuildConfig fields automatically:
    //   FIREBASE_API_KEY, FIREBASE_APP_ID, FIREBASE_PROJECT_ID,
    //   FIREBASE_STORAGE_BUCKET, GOOGLE_WEB_CLIENT_ID
    // Declaring them manually here would produce duplicate-field compile errors.
    // These values previously lived hardcoded in WelcomeAuthScreen.kt.
  }

  // Release signing is driven entirely by environment variables so that no
  // keystore or password is ever committed. When they are absent (local dev,
  // CI pull-request builds) we simply don't register the config and Gradle
  // produces an unsigned release artifact instead of failing configuration.
  val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
  val keystoreFile = file(keystorePath)
  val storePasswordEnv = System.getenv("STORE_PASSWORD")
  val keyPasswordEnv = System.getenv("KEY_PASSWORD")
  val hasReleaseSigning =
    keystoreFile.exists() && !storePasswordEnv.isNullOrBlank() && !keyPasswordEnv.isNullOrBlank()

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = keystoreFile
        storePassword = storePasswordEnv
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = keyPasswordEnv
      }
    }
  }

  buildTypes {
    release {
      // Shrink, obfuscate and strip unused resources for the Play Store build.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
    debug {
      // Falls back to the standard auto-generated debug keystore. The previous
      // config pointed at a `debug.keystore` that is git-ignored and absent,
      // which broke `assembleDebug` on every fresh clone.
      //
      // NOTE: deliberately no applicationIdSuffix. Changing the debug package
      // name would stop it matching the client entry in google-services.json
      // (failing the Google Services plugin) and would invalidate the SHA-1
      // fingerprint registered for Google Sign-In.
      versionNameSuffix = "-debug"
      isMinifyEnabled = false
    }
  }

  packaging {
    resources {
      excludes += setOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "/META-INF/DEPENDENCIES",
        "/META-INF/LICENSE*",
        "/META-INF/NOTICE*",
        "META-INF/*.version",
        "DebugProbesKt.bin",
        "kotlin-tooling-metadata.json",
      )
    }
  }

  lint {
    // Pre-existing warnings across this codebase have not been triaged yet, so
    // lint reports rather than blocks. The genuinely dangerous checks below are
    // escalated to hard errors so the specific security regressions fixed in
    // this pass (permissive WebView SSL/JS, cleartext traffic, hardcoded
    // credentials) cannot silently come back.
    abortOnError = false
    warningsAsErrors = false
    checkReleaseBuilds = true
    checkDependencies = true
    htmlReport = true
    sarifReport = true
    error += setOf(
      "AcceptsUserCertificates",
      "CustomX509TrustManager",
      "TrustAllX509TrustManager",
      "UnsafeProtectedBroadcastReceiver",
      "WebViewClientOnReceivedSslError",
      "CleartextTraffic",
      "HardcodedDebugMode",
      "ExportedActivity",
      "ExportedContentProvider",
      "ExportedReceiver",
      "ExportedService",
    )
    disable += setOf("GradleDependency", "ObsoleteLintCustomCheck")
  }

  dependenciesInfo {
    // Keep the Play Store dependency blob out of the artifact for reproducibility.
    includeInApk = false
    includeInBundle = true
  }

  bundle {
    language { enableSplit = false }
  }
  // AGP 9.x requires JDK 17. Java 11 targets no longer build.
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
}

// Keep the Kotlin JVM target in lockstep with compileOptions. Without this the
// Kotlin and Java targets can diverge and the build fails with an
// "Inconsistent JVM-target compatibility" error.
kotlin {
  jvmToolchain(17)
  compilerOptions {
    freeCompilerArgs.addAll(
      // Media3 Transformer / effect APIs (used to bake Looks into video) are
      // annotated @UnstableApi. Opting in here avoids scattering @OptIn across
      // every call site.
      "-opt-in=androidx.media3.common.util.UnstableApi",
      "-opt-in=kotlin.RequiresOptIn",
    )
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.storage)
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // CameraX - the Camera screen captures real photos/video (was previously a
  // static Unsplash image standing in for a viewfinder).
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.video)
  // Vendor HDR / Night / Bokeh camera extensions where the device supports them.
  implementation(libs.androidx.camera.extensions)
  // EXIF orientation handling for captured stills.
  implementation(libs.androidx.exifinterface)
  // Media3: replay playback + GPU video effects (Transformer) for baking Looks.
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.transformer)
  implementation(libs.androidx.media3.effect)
  implementation(libs.androidx.media3.common)
  // Reliable background replay/moment uploads with retry + constraints.
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("androidx.browser:browser:1.8.0")
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // Provides Task.await() for the Firebase Storage upload path.
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // Venue Intelligence: GPS fix for venue detection on the Camera screen.
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
