plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id ("kotlin-kapt")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.capstone.peopleconnect"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.capstone.peopleconnect"
        minSdk = 28
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.stripe:stripe-android:20.52.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation("com.android.car.ui:car-ui-lib:2.6.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.0")
    testImplementation("junit:junit:4.13.2")
    implementation ("com.squareup.okhttp3:okhttp:4.9.1")
    implementation ("com.github.bumptech.glide:glide:4.15.0")
    implementation ("com.github.bumptech.glide:compiler:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Stream Chat
    implementation ("io.getstream:stream-chat-android-ui-components:6.5.1")
    implementation ("io.getstream:stream-chat-android-client:6.5.1")
    implementation ("io.getstream:stream-chat-android-offline:6.5.1")
    implementation ("io.getstream:stream-chat-android-compose:6.5.1")


    //Circle image
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation ("com.google.firebase:firebase-messaging:23.4.0")
    implementation ("com.google.firebase:firebase-analytics:21.5.0")


    //volley
    implementation ("com.android.volley:volley:1.2.1")




}