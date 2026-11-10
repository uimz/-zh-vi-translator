plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.uimz.zhvi"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.uimz.zhvi"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.belerweb:pinyin4j:2.5.1")
}
