# Keep Kotlinx Serialization routes & companions
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.example.ui.**$$serializer { *; }
-keepclassmembers class com.example.ui.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.ui.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Moshi generated adapters & JSON models
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.example.data.remote.*JsonAdapter { *; }

# Keep Retrofit & OkHttp service interfaces
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Keep Room entities & database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
