plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    signingConfigs {
        create("release") {
            storeFile = file("D:\\ProgramSpace\\keystore\\call.keystore")
            storePassword = "guoyang630"
            keyAlias = "call"
            keyPassword = "guoyang630"
        }
    }
    namespace = "com.convenient.salescall"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.convenient.salescall"
        minSdk = 29
        targetSdk = 33
        versionCode = 5
        versionName = "1.5"

        buildConfigField("Boolean", "OPEN_LOG", "true")

        ndk {
            abiFilters.add("arm64-v8a")  // 显式调用add方法
            abiFilters.add("armeabi")  // 显式调用add方法
        }

        manifestPlaceholders += mutableMapOf<String, Any>(
            "JPUSH_PKGNAME" to "com.convenient.salescall",
            "JPUSH_APPKEY" to "e40b3018c2fbd7e27108fcd8",
            "JPUSH_CHANNEL" to "call_service"
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("Boolean", "OPEN_LOG", "true")
        }
        getByName("release") {
            buildConfigField("Boolean", "OPEN_LOG", "false")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)

    // 网络请求框架
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.gson)

    // Kotlin协程
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ViewModel和LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Fragment和Activity扩展
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)

    //极光推送
    implementation(libs.jiguang.jpush)      // 必选，此处以JPush 5.6.0 版本为例，注意：5.0.0 版本开始可以自动拉取 JCore 包，无需另外配置

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}