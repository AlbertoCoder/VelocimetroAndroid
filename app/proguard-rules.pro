# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-android-optimize.txt.

# Keep Play Services Location classes (used for GPS speed)
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# Kotlin
-dontwarn kotlin.**
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
