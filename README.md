
# FunctionPlotter

一个简洁的 Android 函数图像绘制工具。

功能包括多函数同时绘制、隐函数方程、点击取点、动态比例尺、计算器风格符号键盘、本地状态持久化、悬浮圆钮、深浅色模式，以及基于 GitHub Releases 的应用内更新。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)

---



## 功能特性

1. 多函数同时绘制
  - 最多 5 条曲线，各自独立颜色
  - 顶部 chip 栏切换、删除、新增，超出屏幕可横向滚动
  - 当前选中的函数以高亮背景区分

2. 显函数绘制
  - 直接输入以 x 为自变量的表达式，如 x^2、sin(x)、ln(x)、1/x
  - 定义域断裂处自动断开，不产生错误连线

3. 隐函数方程
  - 输入含等号的方程，自动转为 F(x,y)=0 后用 Marching Squares 算法绘制
  - 支持椭圆、圆、双曲线、笛卡尔心形线等

4. 点击取点
  - 点击曲线附近显示该点坐标 (x, y)
  - 显函数吸附最近曲线，隐函数判断 F 值接近 0
  - 拖动或缩放后标记自动清除

5. 动态比例尺
  - 画布右下角绘制比例尺
  - 缩放时数值在 1、2、5、10 之间自动切换
  - 与坐标轴同层实时重绘，跟随手指无滞后

6. 手势交互
  - 双指缩放、单指拖拽
  - 缩放总倍率限制在 10 到 500 之间，到达上下限后禁止继续
  - 手势期间只做 GPU 层变换，不重算 Path，松手后合并并重绘

7. 计算器风格键盘
  - 按钮显示数学符号，内部插入合法表达式
  - 输入框 readOnly，不弹出系统输入法
  - 支持粘贴按钮，一键读取系统剪贴板

8. 悬浮圆钮
  - 无 ActionBar
  - 设置与信息入口为 chip 栏右侧两个 40dp 圆钮
  - systemBarsPadding 保证不遮挡通知栏与摄像头

9. 深浅色模式
  - 跟随系统，也可在设置中强制指定浅色或深色

10. 设置持久化
  - 主题模式、网格显示、刻度显示、采样精度通过 DataStore 保存

11. 函数状态持久化
  - 函数列表、颜色、当前选中项、输入框草稿全部本地保存
  - 退出应用后重新打开，恢复到退出前的状态

12. 应用内更新
  - 通过 GitHub Releases API 检查新版本
  - 弹窗展示更新说明
  - 下载时显示进度条
  - 完成后调起系统安装器
---
 

 
## 界面概览

|        主界面        |        设置页        |   信息页   |
|:-----------------:|:-----------------:|:-------:|
| 输入框 + 绘图区 + 计算器键盘 | 外观 / 绘图 / 更新 / 关于 | 应用与作者信息 |

截图待补充。可将图片放入 docs/screenshots/ 目录后在表格中引用。

---
 

 
## 构建与运行

### 环境要求

- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 34
- 最低运行版本：Android 8.0（API 26）

### 步骤

```bash
git clone https://github.com/CLoneLING/FunctionPlotter.git
cd FunctionPlotter
```

用 Android Studio 打开项目，等待 Gradle 同步完成，连接真机或启动模拟器，点击运行即可。

命令行构建：

```bash
./gradlew assembleDebug
```

APK 输出路径：app/build/outputs/apk/debug/app-debug.apk

---



## 使用说明

### 多函数

- 顶部 chip 栏点击新增添加曲线，默认取调色板下一个颜色
- 点击任意 chip 切换到该函数，输入框同步显示其表达式
- chip 右侧的 x 图标删除该函数，至少保留一个
- 最多 5 个函数，超出后新增按钮隐藏
- 函数数量超出屏幕宽度时，chip 栏可横向滚动

### 显函数

直接输入以 x 为自变量的表达式，例如：

x^2
sin(x)
sqrt(x)
1/x

### 隐函数方程

输入含等号的方程，程序会自动拆成“左式减右式等于零”后绘制，例如：

x^2+y^2=4
x^2/4+y^2/9=1
(x^2+y^2-1)^3-x^2*y^3=0

### 点击取点

- 显函数：点击靠近曲线位置，吸附到最近的一条可见曲线，显示坐标
- 隐函数：点击接近曲线处，F 值足够小时显示点击位置坐标
- 拖动或缩放后，标记自动清除

### 比例尺

右下角随缩放自动调整，标注数值代表该线段对应的数学长度。

### 粘贴

输入框右侧的粘贴按钮会读取系统剪贴板内容，去除首尾空白后填入输入框。

### 支持的运算符与函数

四则运算    加 减 乘 除
幂          ^
括号        左括号 右括号
三角        sin  cos  tan
对数        ln   log
开方        sqrt
绝对值      abs
常量        pi   e   也可写 exp(1)
变量        x    y
---
 

 
## 项目结构

```text

app/src/main/java/com/yumoo/functionplotter/

  MainActivity.kt
  FunctionPlotterApp.kt

  data/
    SettingsRepository.kt          偏好设置与函数状态持久化
    model/PlotFunction.kt          多函数数据模型
    update/
      GithubApi.kt
      GithubRelease.kt
      UpdateManager.kt
      UpdateResult.kt

  domain/
    parser/
      Exp4jEvaluator.kt            表达式求值，单变量与双变量
      PlotExpression.kt            显函数与隐函数解析
    plot/
      CurveSampler.kt              显函数逐像素采样
      ImplicitCurveRenderer.kt     隐函数 Marching Squares
    advanced/
      HandwritingFitter.kt         手绘拟合预留接口
      OcrRecognizer.kt             拍照识别预留接口
      CalculusEngine.kt            微积分可视化预留接口

  ui/
    theme/                         Material 3 主题与深浅色
    navigation/AppNav.kt           页面导航
    screen/
      PlotterScreen.kt             主界面
      SettingsScreen.kt            设置页
      InfoScreen.kt                信息页
    component/
      FunctionCanvas.kt            绘图区，手势与双 Canvas 分层
      FunctionChips.kt             函数 chip 栏
      ExpressionInput.kt           输入框与粘贴、生成按钮
      MathKeyboard.kt              计算器风格键盘
      UpdateDialog.kt              更新说明与下载进度弹窗

  viewmodel/
    PlotterViewModel.kt
    SettingsViewModel.kt
```

---
 

 
## 技术栈

| 类别    | 选型                                                   |
|:------|:-----------------------------------------------------|
| 语言    | Kotlin                                               |
| UI    | Jetpack Compose + Material 3                         |
| 表达式解析 | exp4j                                                |
| 隐函数绘制 | Marching Squares 自实现                                 |
| 偏好存储  | DataStore Preferences                                |
| 网络请求  | Retrofit + OkHttp                                    |
| 导航    | Navigation Compose                                   |
| 更新分发  | GitHub Releases API + DownloadManager + FileProvider |

---
## 性能调度说明

- 双 Canvas 分层
  - 函数曲线、网格、刻度数字由 graphicsLayer 做 GPU 级缩放平移，手势期间不重建 Path
  - 坐标轴和比例尺独立一层，每帧按实时视口重绘，跟随手指无滞后

- Path 缓存 key 为 functions、committedViewport、sizePx、sampleStep
  - 手势期间以上均不变化，因此不触发 exp4j 重算

- 停手 120ms 后 debounce 一次
  - 把 live 变换合并进 committed 视口，并重建全部 Path，恢复清晰

- 缩放总倍率钳制在 10 到 500
  - 单次手势 live 倍率上限 10
  - 到达上下限后继续缩放无效

- 显函数采样步长限制 1.5 到 8 像素
  - 最大迭代次数由画布宽度与步长推算，超过即终止

- 隐函数网格上限 160x160
  - 步长由画布短边除以 80 自动推算，最小 8 像素

- 网格与刻度自适应
  - 网格步长 = niceNumber(80 / scale) * scale，稳定在 80 到 160 像素
  - 刻度标签间距为其两倍，缩小视图不会挤在一起

- 数值防护
  - 所有坐标检查 isFinite、isNaN、isInfinite
  - 平移偏移钳制在正负 100000
  - 坐标轴刻度最多 50 个，网格线最多 300 条

- 布局防护
  - 画布内容通过 clipToBounds 双重裁剪
  - 不会覆盖输入框和 chip 栏

---

 
## 应用内更新

应用通过 https://api.github.com/repos/{owner}/{repo}/releases/latest 获取最新 Release：

- tag_name 用于版本比较
- body 展示更新说明
- assets 中查找 .apk 文件，取其 browser_download_url 下载

发布新版本时请遵循：

1. tag_name 直接写版本号，例如 0.2.0
2. 每个 Release 上传一个签名 APK 作为 asset
3. 签名必须与已安装版本一致，否则覆盖安装会失败

发布前请将 UpdateManager.kt 中的 GITHUB_OWNER 和 GITHUB_REPO 替换为真实值。

---
 

 
## 后续计划

domain/advanced/ 下已预留以下接口，欢迎参与实现：

- 手绘拟合（HandwritingFitter）：用户手绘曲线后，用最小二乘拟合反推函数表达式。
- 拍照识别（OcrRecognizer）：拍照或从相册选择公式图片，识别并转为可绘制表达式。
- 微积分可视化（CalculusEngine）：求导、定积分，并将结果作为第二条曲线叠加显示。

---
 

 
## 贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 新建分支：git checkout -b feature/your-feature
3. 提交改动：git commit -m "Add your feature"
4. 推送分支：git push origin feature/your-feature
5. 打开 Pull Request

---
 

 
## 许可证

本项目基于 MIT License 开源。详见根目录 LICENSE 文件。

---

## 致谢

- exp4j：数学表达式解析
- Jetpack Compose：声明式 UI
- Material 3：设计规范
 
