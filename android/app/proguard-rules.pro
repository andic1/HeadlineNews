# keep kotlinx.serialization
-keep,includedescriptorclasses class com.demo.toutiao.**$$serializer { *; }
-keepclassmembers class com.demo.toutiao.** { *** Companion; }
-keepclasseswithmembers class com.demo.toutiao.** { kotlinx.serialization.KSerializer serializer(...); }
