plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.knockly"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.knockly"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.security.crypto)
    implementation(libs.androidx.biometric)

    /* FTP library */
    implementation(libs.commons.net)

    /* ───── Retrofit / OkHttp for API calls ───── */
    implementation(libs.retrofit)           // core Retrofit
    implementation(libs.converter.gson)     // JSON <--> POJO
    implementation(libs.okhttp.logging)     // HTTP logcat interceptor

    /* ───── tests ───── */
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    /* ───── SSH ───── */
    implementation(libs.jsch)

    /* ───── RTSP ───── */
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.rtsp)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-messaging:23.0.0")
}