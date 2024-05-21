# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#-flattenpackagehierarchy
#-ignorewarnings
#-keep class * extends com.google.gson.reflect.TypeToken
#
## Optional. For using GSON @Expose annotation
#-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
#-keep class com.hexawave.otlight.Models.**{*;}
#
## Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
# -keep,allowobfuscation,allowshrinking interface retrofit2.Call
# -keep,allowobfuscation,allowshrinking class retrofit2.Response

# Optimization Settings
# Kotlin and standard Android libraries
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# AndroidX libraries
-keep class androidx.** { *; }
-dontwarn androidx.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keepattributes Signature

# Retrofit and OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-keep class com.squareup.retrofit2.** { *; }
-dontwarn com.squareup.retrofit2.**

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.Glide
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.**

# Dagger
-keep class dagger.** { *; }
-dontwarn dagger.**
-dontwarn javax.inject.**
-keep class javax.inject.** { *; }

# Paging, Room, and WorkManager
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Shimmer
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Miscellaneous libraries
-keep class com.intuit.sdp.** { *; }
-dontwarn com.intuit.sdp.**
-keep class com.github.PhilJay.** { *; }
-dontwarn com.github.PhilJay.**
-keep class com.github.fornewid.neumorphism.** { *; }
-dontwarn com.github.fornewid.neumorphism.**

# Reflection and serialization
-keepclassmembers class ** {
    @org.jetbrains.annotations.Nullable *;
    @org.jetbrains.annotations.NotNull *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.* *;
}

# Multidex support
-keep class androidx.multidex.** { *; }
-dontwarn androidx.multidex.**

# Debug information
-keepattributes SourceFile, LineNumberTable

# General optimization
-dontobfuscate
-dontoptimize
-dontpreverify

# Keep your API response models
-keep class com.eresto.plus.data.models.** { *; }

-keep class com.eresto.plus.data.** { *; }

# keep everything in this package from being renamed only
-keepnames class com.eresto.plus.data.** { *; }

-keep class com.eresto.plus.retrofit.** { *; }

# keep everything in this package from being renamed only
-keepnames class com.eresto.plus.retrofit.** { *; }


# Kotlin and AndroidX
-keep class kotlin.** { *; }
-keep class androidx.** { *; }
-dontwarn kotlin.**
-dontwarn androidx.**

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Gson serialized fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep API response models
-keep class com.eresto.plus.data.models.** { *; }

# Keep Retrofit service interfaces
-keep interface com.eresto.plus.data.network.** { *; }

# Prevent obfuscation of Retrofit annotations
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# keep everything in this package from being removed or renamed
-keep class com.eresto.captain.model.** { *; }

# keep everything in this package from being renamed only
-keepnames class com.eresto.captain.model.** { *; }

-keep class com.google.gson.reflect.TypeToken {
    <fields>;
    <methods>;
}

-keepattributes Signature


