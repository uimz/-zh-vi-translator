import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.uimz.zhvi"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.uimz.zhvi"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "4.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
dependencies {
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
}
