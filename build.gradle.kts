// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // AÑADE ESTA LÍNEA para Firebase:
    alias(libs.plugins.google.services) apply false
}

// ESTE ES EL LUGAR CORRECTO para el classpath:
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
    }
}