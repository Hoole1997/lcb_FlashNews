# Weather 模块通过 Gson 反射解析接口响应和本地缓存。
# 保留模型字段名与构造信息，防止 Release 混淆后天气为空或缓存反序列化失败。
-keepattributes Signature,*Annotation*
-keep class com.browser.weather.data.OpenMeteoResponse { *; }
-keep class com.browser.weather.data.OpenMeteoCurrent { *; }
-keep class com.browser.weather.data.OpenMeteoDaily { *; }
-keep class com.browser.weather.data.GeocodingResponse { *; }
-keep class com.browser.weather.data.GeocodingResult { *; }
-keep class com.browser.weather.data.IpGeoResponse { *; }
-keep class com.browser.weather.data.DailyForecastData { *; }
-keep class com.browser.weather.data.WeatherData { *; }
-keep class com.browser.weather.data.SavedWeatherLocation { *; }
