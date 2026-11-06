# Flutter ProGuard配置文件
# 用于代码混淆和优化

# 基本规则
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 优化选项
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# 保留Flutter相关类
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# 保留主应用类
-keep class com.example.experiment_app.** { *; }

# 保留SQLite相关类
-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }

# 保留JSON解析相关类
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 保留WebView相关类
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(java.lang.String);
}

# 保留网络请求相关类
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# 保留共享偏好设置相关类
-keep class android.content.SharedPreferences { *; }

# 保留文件操作相关类
-keep class java.io.** { *; }

# 保留时间相关类
-keep class java.text.** { *; }
-keep class java.util.** { *; }

# 保留反射相关类
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 混淆规则：避免混淆特定的枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 混淆规则：避免混淆Parcelable实现
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 保留Application类
-keep public class * extends android.app.Application

# 保留Activity类
-keep public class * extends android.app.Activity

# 保留Service类
-keep public class * extends android.app.Service

# 保留BroadcastReceiver类
-keep public class * extends android.content.BroadcastReceiver

# 保留ContentProvider类
-keep public class * extends android.content.ContentProvider

# 保留WebChromeClient类
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 移除日志输出
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 性能优化：合并类
-mergeinterfacesaggressively

# 移除未使用的资源

# 移除未使用的属性
-adaptresourcefilenames

# 压缩代码
-allowaccessmodification