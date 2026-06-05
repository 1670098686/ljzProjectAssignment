# Flutter 收支记账APP - ProGuard 代码混淆配置

# Flutter 特定规则
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.**  { *; }
-keep class io.flutter.util.**  { *; }
-keep class io.flutter.view.**  { *; }
-keep class io.flutter.**  { *; }
-keep class io.flutter.plugins.**  { *; }

# sqflite 数据库混淆规则
-keep class com.tekartik.sqflite.** { *; }

# shared_preferences 混淆规则
-keep class com.example.flutter_app.SharedPreferencesPlugin { *; }
-keep class androidx.** { *; }

# Provider 状态管理混淆规则
-keep class provider.** { *; }

# 图片加载库混淆规则
-keep class com.bumptech.glide.** { *; }
-keep class com.github.bumptech.glide.** { *; }

# 网络请求库混淆规则
-keep class io.flutter.plugin.common.** { *; }

# 数据模型混淆规则（保留必要字段）
-keep class com.example.finance_app.models.** { 
    <fields>;
    <methods>;
}

# 业务逻辑服务类混淆规则（保留必要方法）
-keep class com.example.finance_app.services.** {
    <methods>;
    <fields>;
}

# API 接口混淆规则
-keep interface com.example.finance_app.api.** { *; }

# 常量类混淆规则
-keep class com.example.finance_app.constants.** { 
    public static final <fields>;
}

# 工具类混淆规则
-keep class com.example.finance_app.utils.** { 
    public static <methods>;
}

# 性能优化服务混淆规则
-keep class com.example.finance_app.core.services.** {
    <methods>;
    <fields>;
}

# 数据库实体类混淆规则
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# JSON 序列化类混淆规则
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# 移除日志输出（在发布版本中）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 保留反射相关类
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留泛型
-keepattributes Signature
-keepattributes *Annotation*

# 保留行号信息用于调试
-keepattributes SourceFile,LineNumberTable

# 优化规则
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# 移除调试类
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# 保留 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable 实现
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 保留 JNI 相关类
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留注解
-keep @interface *