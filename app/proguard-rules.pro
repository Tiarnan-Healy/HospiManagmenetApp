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
-renamesourcefileattribute SourceFile

# Implementing ProGuard for obfuscation/security
# Keep Room entities, R8 must not rename these as Room relies on field names
-keep class com.example.hospimanagmenetapp.data.entities.** { *; }

# Keep DAOs as Room annotation processor generates implementations by name
-keep interface com.example.hospimanagmenetapp.data.dao.** { *; }

# Keep Retrofit DTOs, Gson uses field names for JSON deserialisation
-keep class com.example.hospimanagmenetapp.network.dto.** { *; }

# Keep ZXing barcode scanner
-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }