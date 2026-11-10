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
        versionCode = 1
        versionName = "1.0"
    }
}
dependencies { implementation("com.belerweb:pinyin4j:2.5.1") }
