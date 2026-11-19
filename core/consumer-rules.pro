# Keep kotlinx.serialization generated serializers in :core
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.ironlog.core.**$$serializer { *; }
-keepclassmembers class com.example.ironlog.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.ironlog.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
