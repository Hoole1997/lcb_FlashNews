# 保留泛型、注解和源码行号，确保 Gson 等反射框架可读取元数据，
# 同时让线上混淆堆栈仍可通过 mapping 文件准确还原。
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Launcher SDK 本身已经经过混淆，且内部包含反射创建的页面和组件。
# 避免 R8 再次裁剪或优化这些入口，降低仅在 Release 包中出现崩溃的风险。
-keep class com.leafmotivation.quizguessoncolor.** { *; }

# 以下类是三方 SDK 的可选集成能力，当前产品没有接入对应运行时。
# 忽略其静态引用告警，不会掩盖项目自身的缺失依赖。
-dontwarn com.kwad.sdk.datacollection.KsSafetyPrivateDataController
-dontwarn com.unity3d.player.**
-dontwarn org.joda.convert.**
