# kotlinx.serialization ships its own consumer rules; keep our @Serializable models explicit.
-keep,includedescriptorclasses class com.mishkat.launcher.theme.**$$serializer { *; }
-keepclassmembers class com.mishkat.launcher.theme.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.mishkat.launcher.radio.**$$serializer { *; }
-keepclassmembers class com.mishkat.launcher.radio.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.mishkat.launcher.prayer.**$$serializer { *; }
-keepclassmembers class com.mishkat.launcher.prayer.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.mishkat.launcher.weather.**$$serializer { *; }
-keepclassmembers class com.mishkat.launcher.weather.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.mishkat.launcher.livetv.**$$serializer { *; }
-keepclassmembers class com.mishkat.launcher.livetv.** {
    *** Companion;
}

# Storage protocols: JCIFS-NG (Samba SMB) & Apache Commons Net (FTP)
-keep class jcifs.** { *; }
-dontwarn jcifs.**
-dontwarn org.slf4j.**
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# R9: strip verbose logging calls in release (Logger is also gated by BuildConfig.DEBUG).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
