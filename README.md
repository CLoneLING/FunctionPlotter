![icon](./app_icon.ico)
# FunctionPlotter

一个简洁的 Android 函数图像绘制工具。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)

>该仓库目前由DeepSeek AI自动化管理

当前版本：**1.6.4**（versionCode 20261046）

**官网** <https://functionplotter.dpdns.org>

**演示视频** [B站](https://www.bilibili.com/video/BV1gLxu65EKe)

---

## 功能概览

1. **多函数同时绘制**
   - 最多 5 条曲线，颜色互不重复
   - 顶部 chip 栏切换、删除、新增，可横向滚动
   - 表达式有误的曲线在 chip 上显示警告图标

2. **显函数与隐函数**
   - 直接输入 `x^2`、`sin(x)`、`1/x` 等，按屏幕列采样
   - 输入含等号的方程（如 `x^2+y^2=9`）自动转为 `F(x,y)=0`，用 Marching Squares 绘制
   - `y=...`、`f(x)=...` 会自动识别为显函数，比等值线快一个数量级

3. **手势交互**
   - 单指拖动平移、双指缩放（总倍率 8–900）
   - 手势期间只做 GPU 层变换，松手后才重算路径
   - 点按曲线显示该点坐标，视图变化后标记自动清除
   - 坐标是 π 的有理倍数时，**同时给出 π 形式与小数形式**（如 `(π/2, 0)` 与 `(1.571, 0)`）；
     点击位置落在常见角度（π/6、π/4、π/3、π/2、π…）4px 内会自动对齐，方便点到课本上的特殊角

4. **分页数学键盘**
   - 四页：基础 / 函数 / 高级 / 常数，左右滑动或点页签切换
   - 每个按键都带一行小字说明（正弦、自然对数、平方根…）
   - 常数页内置 6 个示例：抛物线、正弦、双曲、圆、心形、指数

5. **面向普通用户的符号翻译**
   - 键盘插入的是"人类写法"，由 `ExpressionNormalizer` 翻译成 exp4j 语法
   - 支持 `×`、`÷`、`−`、`π`、`√`、`∛`、`|x|`、`x²`、`2x`、`2(x+1)`、`sin(x)cos(x)`
   - 输入框下方实时提示语法错误，点 `?` 查看完整写法说明

6. **应用内更新**
   - 启动时自动检查（可在设置关闭，最多每 24 小时一次）
   - 手动"立即检查更新"，语义化版本比较
   - **先校验"安装未知应用"权限，未授权不下载**，跳转设置授权后返回自动续下
   - 下载走系统 DownloadManager，支持后台下载与进度通知；关闭应用内进度条不影响下载
   - 进程被杀后重新打开，仍能继续跟踪下载进度

7. **本地持久化**
   - 函数列表、颜色、选中项、输入草稿、界面设置全部保存在 DataStore
   - 已备份规则只带走这些数据

8. **深浅色模式**：跟随系统，或强制浅色 / 深色

---

## 输入语法

| 写法 | 含义 | 示例 |
|:---|:---|:---|
| `x^2` / `x²` | 乘方 | `x^3`、`x⁻¹` |
| `×` `÷` `−` | 四则运算 | `2×3` |
| `√` `∛` | 平方根 / 立方根 | `√9`、`√(x+1)`、`∛8` |
| `ln(x)` | 自然对数（底 e） | `ln(x)` |
| `lg(x)` | 常用对数（底 10） | `lg(x)` |
| `log(a, b)` | 以 a 为底 b 的对数 | `log(2, 8)` = 3 |
| `abs(x)` | 绝对值 | `\|x-1\|` |
| `2x`、`2(x+1)` | 省略乘号 | `3sin(x)`、`(x+1)(x-1)` |
| `π`、`e` | 圆周率、自然常数 | `2π`、`e^x` |
| `1e-3` | 科学计数法 | `2.5E6` |
| 含 `=` | 方程（隐函数） | `x^2+y^2=9` |

> exp4j 本身没有 `ln`，它的 `log` 是自然对数。本项目在规范化层做了映射，
> 因此 `ln`、`lg`、`log(a,b)` 都按数学课本的含义工作。

---

## 构建与运行

环境要求：Android Studio Ladybug 或更新、JDK 17、Android SDK 34、最低运行 Android 8.0（API 26）。

```bash
git clone https://github.com/CLoneLING/FunctionPlotter.git
cd FunctionPlotter
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

单元测试：

```bash
./gradlew testDebugUnitTest
```

---

## 使用说明

### 键盘

- 基础页：数字、四则运算、括号、`x`、`y`、`=`、`⌫`、清空
- 函数页：三角 / 反三角 / 双曲、`ln`、`lg`、`logₐ`、`√`、`∛`、`x²`、`x³`、绝对值
- 高级页：`pow(`、`cbrt(`、`log(`、`signum(`、`%`、逗号、取整函数等
- 常数页：`π`、`e`、`x`、`y`、`=`，以及六个示例（点击直接替换当前表达式）
- `logₐ` 会弹出底数选择：2 / 10 / e / 自定义（自定义后输入 `log(3, 8)` 即"以 3 为底 8 的对数"）

### 更新流程

1. 发现新版本 → 弹窗展示更新说明
2. 点"下载并安装"：
   - 若未授权"安装未知应用"，直接跳系统设置，**不开始下载**
   - 授权后返回应用，自动开始下载
3. 下载中：应用内进度条 + 系统通知；点"隐藏"只收起进度条
4. 下载完成：点"安装"调起系统安装器

---

## 项目结构

```text
app/src/main/java/com/yumoo/functionplotter/

  MainActivity.kt                 入口，按设置套主题
  FunctionPlotterApp.kt           Application，持有 SettingsRepository

  data/
    SettingsRepository.kt         设置、函数状态、下载任务 id 持久化
    model/PlotFunction.kt         多函数数据模型
    update/
      GithubApi.kt                Retrofit 接口
      GithubRelease.kt            Release 响应（字段可空）
      UpdateManager.kt            检查更新、下载、安装、权限判断
      UpdateResult.kt             更新结果
      VersionCompare.kt           语义化版本比较
      DownloadNotifications.kt    下载进度常驻通知

  domain/
    parser/
      ExpressionNormalizer.kt     人类写法 → exp4j 写法
      PlotExpression.kt           显式 / 隐式方程解析
      ExpressionCheck.kt          输入体检与中文错误提示
      Exp4jEvaluator.kt           表达式预编译
    plot/
      PlotViewport.kt             世界坐标视口与坐标换算
      CurveSampler.kt             显函数按列采样
      ImplicitCurveRenderer.kt    隐函数 Marching Squares
      PlotFrame.kt                离屏计算一帧的所有曲线
    advanced/                     手绘拟合 / OCR / 微积分预留接口

  ui/
    theme/                        Material 3 主题
    navigation/AppNav.kt          导航 + 全局更新宿主
    screen/                       PlotterScreen / SettingsScreen / InfoScreen
    component/
      FunctionCanvas.kt           画布、相机、手势、网格与刻度
      FunctionChips.kt            函数 chip 栏
      ExpressionInput.kt          输入框、粘贴、帮助、错误提示
      MathKeyboard.kt             分页数学键盘
      UpdateDialog.kt             更新说明弹窗
      UpdateHost.kt               更新流程与下载进度条

  viewmodel/
    PlotterViewModel.kt           函数列表 / 选中项 / 草稿
    SettingsViewModel.kt          设置项
    UpdateViewModel.kt            检查更新、下载、安装状态
```

---

## 性能与稳定性设计

- **表达式只解析一次**：`PreparedFunction` 复用 exp4j 的 `Expression`，
  采样循环里只做 `setVariable().evaluate()`。旧实现每个采样点重新 `build()`，
  隐式曲线一轮要解析两万多次，是卡顿和崩溃的主因。
- **绘图计算离开主线程**：`buildPlotFrame` 在 `Dispatchers.Default` 上执行，
  `produceState` 自动取消过期任务；主线程只负责绘制。
- **相机用世界坐标**：视口是"画布中心的世界坐标 + 每单位像素数"，
  平移不会无限累积像素偏移；中心钳制在 ±10⁶，缩放在 8–900 之间。
- **路径坐标有界**：采样点被夹在 `[-h/2, 1.5h]`，相邻点纵向跳变过大就断开子路径，
  `1/x`、`tan(x)` 不再产生贯穿屏幕的假线，也不会把 ±10⁶ 坐标写进 Path。
- **分层绘制**：曲线在 GPU 变换层；网格、坐标轴、刻度、比例尺每帧按实时视口
  在屏幕空间绘制，任何倍率下都清晰。
- **单条曲线隔离**：某条表达式抛错只影响它自己，其余曲线照常绘制。
- **隐函数网格自适应**：按画布长短边同时约束格子数量，长屏手机不会算出几万个格点。
- **纯点击不重算**：没有实际平移/缩放时跳过 Path 重建。

---

## 已知限制

- 目前只支持一个自变量 `x`，参数方程尚未支持。
- 隐函数使用固定网格，极细的曲线细节可能丢失（可通过放大改善）。
- 表达式语法错误只在输入框下方提示，不会定位到具体字符位置。
- `signum`、`log1p` 等 exp4j 原生函数未在键盘中暴露，可直接手写或粘贴。

---

## 后续计划

`domain/advanced/` 下已预留接口：

- 手绘拟合（HandwritingFitter）：手绘曲线后最小二乘反推表达式
- 拍照识别（OcrRecognizer）：拍照/相册识别公式
- 微积分可视化（CalculusEngine）：求导、定积分并叠加曲线

---

## 许可证

本项目基于 MIT License 开源，详见 LICENSE。

## 致谢

- exp4j：数学表达式解析 `Apache-2.0`
- Jetpack Compose / Material 3
