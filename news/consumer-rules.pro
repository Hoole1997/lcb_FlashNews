# 新闻 SDK 对外 API 和数据模型需要保持稳定，方便宿主侧反射、日志和后续混淆排查。
-keepattributes Signature,*Annotation*
-keep class com.example.lcb.news.api.** { *; }
-keep class com.example.lcb.news.model.** { *; }
