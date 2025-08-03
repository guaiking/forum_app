plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.chat"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.example.chat"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}
dependencies {
    implementation ("com.squareup.okhttp3:okhttp:4.9.1") // 添加OkHttp库
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3") // 网络请求
    implementation ("com.google.code.gson:gson:2.8.9")   // JSON处理
    implementation ("androidx.recyclerview:recyclerview:1.3.2") // 聊天列表
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("io.noties.markwon:core:4.6.2")
//    implementation ("androidx.drawerlayout:drawerlayout:1.1.1")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
}
;