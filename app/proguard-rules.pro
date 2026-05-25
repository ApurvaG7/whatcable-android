# Shizuku
-keep class dev.rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
