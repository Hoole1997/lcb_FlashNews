# 广告收入配置由 Gson 从远程 JSON 反序列化，字段不可被 R8 改名或移除。
-keepattributes Signature,*Annotation*
-keep class net.corekit.metrics.revenue.RevenueConfigItem { *; }

# Provider 是 Metrics 模块的清单入口，保留完整实现以兼容不同宿主的合并结果。
-keep class net.corekit.metrics.provider.MetricsModuleProvider { *; }
