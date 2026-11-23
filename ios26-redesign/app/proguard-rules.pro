# Lumo UI / Compose
-dontwarn com.nomanr.**
-keep class ru.school28.diary.ui_components.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson (модели данных)
-keep class ru.school28.diary.api.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Сохраняем BuildConfig
-keep class ru.school28.diary.BuildConfig { *; }