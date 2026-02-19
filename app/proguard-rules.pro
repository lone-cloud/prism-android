# keep classes used for Json deserializing
-keep class app.lonecloud.prism.api.data.** { *; }
-keepnames class org.unifiedpush.** { *; }

# preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile