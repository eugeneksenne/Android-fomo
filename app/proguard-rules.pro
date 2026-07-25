# FOMO — R8 / ProGuard configuration for release builds.
#
# Release builds run with isMinifyEnabled=true and isShrinkResources=true.
# These rules keep the reflection-dependent parts of the stack working.

# Keep line numbers so Play Console / Crashlytics stack traces stay readable,
# while hiding original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotations & generics are required by Moshi, Retrofit and kotlinx.serialization.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

##---------------------------------------------------------------------------
## kotlinx.serialization — used for type-safe Navigation Compose routes.
## If these are stripped, navigation breaks at runtime with a serializer error.
##---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep every @Serializable class and its generated serializer.
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.example.** { *; }

# Navigation Compose route classes are resolved reflectively.
-keep class com.example.core.navigation.** { *; }

##---------------------------------------------------------------------------
## Data models — deserialized from Firestore via reflection.
##---------------------------------------------------------------------------
-keep class com.example.core.data.** { *; }
-keepclassmembers class com.example.core.data.** {
    <init>();
    <fields>;
}

##---------------------------------------------------------------------------
## Firebase / Google Play Services
##---------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
# Firestore uses reflection for POJO (de)serialization.
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
}

##---------------------------------------------------------------------------
## Credential Manager / Google Identity (Google Sign-In)
##---------------------------------------------------------------------------
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

##---------------------------------------------------------------------------
## Moshi
##---------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}
-keep class **JsonAdapter { <init>(...); *; }
-dontwarn com.squareup.moshi.**

##---------------------------------------------------------------------------
## Retrofit / OkHttp
##---------------------------------------------------------------------------
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

##---------------------------------------------------------------------------
## Room
##---------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

##---------------------------------------------------------------------------
## Coil
##---------------------------------------------------------------------------
-dontwarn coil.**

##---------------------------------------------------------------------------
## WebView JavaScript bridge.
## MapScreen exposes an "AndroidBridge" object to JavaScript. R8 would
## otherwise rename/remove these methods and the map's marker taps would
## silently stop working in release builds.
##---------------------------------------------------------------------------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

##---------------------------------------------------------------------------
## Kotlin / Compose
##---------------------------------------------------------------------------
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

##---------------------------------------------------------------------------
## CameraX
##---------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**
# CameraX loads camera2 implementation classes reflectively via
# CameraXConfig; stripping them yields "No camera provider" at runtime.
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.core.impl.** { *; }

##---------------------------------------------------------------------------
## Firebase Storage (media upload)
##---------------------------------------------------------------------------
-keep class com.google.firebase.storage.** { *; }
-dontwarn com.google.firebase.storage.**
