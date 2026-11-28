# Add project specific ProGuard rules here.
# Keep Moshi Kotlin-reflection backup models in minified Release builds.
-keep class com.example.data.backup.** { *; }
# Backup JSON restores database model classes by their original fully-qualified names.
# Keep the whole database package so R8 cannot remove or rename referenced entities.
-keep class com.example.data.database.** { *; }
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
