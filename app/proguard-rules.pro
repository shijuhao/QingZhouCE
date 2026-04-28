-keep class org.luaj.vm2.** { *; }
-keep class org.luaj.vm2.lib.** { *; }
-keep class org.luaj.vm2.script.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn com.google.zxing.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn javax.script.**
-dontwarn org.luaj.vm2.script.**
-dontwarn net.objecthunter.exp4j.**
-dontwarn org.luaj.vm2.lib.**
-dontwarn org.apache.bcel.**
-dontwarn org.luaj.vm2.luajc.**
-dontwarn com.google.re2j.**

-keepattributes Signature
-keepattributes *Annotation*

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-optimizations !code/simplification/arithmetic
-optimizationpasses 5

-allowaccessmodification
-mergeinterfacesaggressively