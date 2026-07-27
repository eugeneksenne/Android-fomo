# FOMO production shrinker rules.

# Keep line numbers for actionable crash reports while still allowing obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata is needed by serialization/reflection-adjacent libraries used in the app.
-keep class kotlin.Metadata { *; }

# Firebase/Play Services are generally covered by bundled consumer rules. Keep app models that
# Firestore may deserialize reflectively when production data sync is enabled.
-keepclassmembers class com.example.core.data.** { *; }
-keepclassmembers class com.example.feature.**.*Model { *; }

# Native Oboe bridge entry points.
-keep class com.google.oboe.** { *; }
