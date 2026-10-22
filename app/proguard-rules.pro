# FunctionPlotter 混淆规则
# 当前 release 未开启 minify，保留这些规则是为了以后开启时不必再排查一遍。

# Gson 反射用到的数据类
-keep class com.yumoo.functionplotter.data.model.** { *; }
-keep class com.yumoo.functionplotter.data.update.GithubRelease { *; }
-keep class com.yumoo.functionplotter.data.update.GithubAsset { *; }

# Retrofit 接口
-keep interface com.yumoo.functionplotter.data.update.GithubApi { *; }
-keepattributes Signature
-keepattributes *Annotation*

# exp4j 通过反射访问函数表
-keep class net.objecthunter.exp4j.** { *; }

# 协程
-dontwarn kotlinx.coroutines.**
