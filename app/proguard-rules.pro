# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
