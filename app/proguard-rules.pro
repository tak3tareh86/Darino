# Add project specific ProGuard rules here.
# Diagnostic protection for Moshi Kotlin-reflection backup models.
# This is intentionally scoped to the backup package for the Release restore test.
-keep class com.example.data.backup.** { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keep class kotlin.Metadata { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name for the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
