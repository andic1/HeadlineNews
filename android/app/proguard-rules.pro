# keep kotlinx.serialization
-keep,includedescriptorclasses class com.headline.news.**$$serializer { *; }
-keepclassmembers class com.headline.news.** { *** Companion; }
-keepclasseswithmembers class com.headline.news.** { kotlinx.serialization.KSerializer serializer(...); }
