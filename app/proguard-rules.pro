# Add project specific ProGuard rules here.
# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.cookiesandcream.queuebuddy.** {
    *** Companion;
}
-keepclasseswithmembers class com.cookiesandcream.queuebuddy.** {
    kotlinx.serialization.KSerializer serializer(...);
}
