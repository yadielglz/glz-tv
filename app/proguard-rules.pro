# Project-specific ProGuard rules.

# SAX creates this handler directly today. Keep its members stable if parser construction
# becomes reflective on an older vendor runtime.
-keep class com.glztv.app.EpgParser$GuideHandler { *; }

# WorkManager instantiates workers by class name from persisted work specifications.
-keep class com.glztv.app.GlzSyncWorker { <init>(...); }
