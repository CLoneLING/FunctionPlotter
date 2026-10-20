 
# FunctionPlotter

一个简洁的 Android 函数图像绘制工具，支持显函数与隐函数方程，内置计算器风格符号键盘，适配系统深色/浅色模式。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)
![Compose](https://img.shields.io/badge/Compose-Material%203-4285F4.svg)

---
 

 
## 功能特性

- 显函数绘制：x^2、sin(x)、ln(x)、1/x 等，自动在定义域断裂处断开，不产生错误连线。
- 隐函数方程：支持 F(x,y)=0 形式的方程，例如：
  - 椭圆：x^2/4+y^2/9=1
  - 圆：x^2+y^2=4
  - 双曲线：x^2/4-y^2/9=1
  - 笛卡尔心形线：(x^2+y^2-1)^3-x^2*y^3=0
- 手势交互：双指缩放、单指拖拽平移，网格、坐标轴、曲线同步变换，带极端值防护不闪退。
- 计算器风格键盘：按钮显示数学符号（根号、圆周率、除号、乘号），内部插入合法表达式（sqrt(、pi、/、*），输入框 readOnly 不弹出系统输入法。
- 坐标轴刻度：自动绘制网格、坐标轴和数值标签，可在设置中开关。
- 深色/浅色模式：跟随系统，也可在设置中强制指定。
- 设置持久化：主题模式、网格显示、刻度显示、采样精度通过 DataStore 保存。
- 应用内更新：通过 GitHub Releases API 检查新版本，展示更新说明，下载 APK 并调起系统安装器。

---
 

 
## 界面概览

| 主界面 | 设置页 | 信息页 |
| :---: | :---: | :---: |
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

### 显函数

直接输入以 x 为自变量的表达式，例如：

```
x^2
sin(x)
sqrt(x)
1/x
```

### 隐函数方程

输入含等号的方程，程序会自动拆成“左式减右式等于零”并用 Marching Squares 算法绘制：

```
x^2+y^2=4
x^2/4+y^2/9=1
(x^2+y^2-1)^3-x^2*y^3=0
```

### 支持的运算符与函数

| 类型 | 写法 |
| :--- | :--- |
| 四则运算 | + - * / |
| 幂 | ^ |
| 括号 | ( ) |
| 三角 | sin( cos( tan( |
| 对数 | ln( log( |
| 开方 | sqrt( |
| 绝对值 | abs( |
| 常量 | pi、e（也可写 exp(1)） |
| 变量 | x、y |

---
 

 
## 项目结构

```text
app/src/main/java/com/yumoo/functionplotter/
├── MainActivity.kt
├── FunctionPlotterApp.kt
├── data/
│   ├── SettingsRepository.kt          # DataStore 偏好设置
│   └── update/                        # GitHub Releases 更新
├── domain/
│   ├── parser/                        # exp4j 封装、显/隐函数解析
│   ├── plot/                          # 显函数采样、隐函数 Marching Squares
│   └── advanced/                      # 手绘拟合 / OCR / 微积分预留接口
├── ui/
│   ├── theme/                         # Material 3 主题、深浅色
│   ├── navigation/                    # 页面导航
│   ├── screen/                        # 主界面、设置、信息
│   └── component/                     # 输入框、Canvas、计算器键盘
└── viewmodel/                         # PlotterViewModel、SettingsViewModel
```

---
 

 
## 技术栈

| 类别 | 选型 |
| :--- | :--- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 表达式解析 | exp4j |
| 隐函数绘制 | Marching Squares 自实现 |
| 偏好存储 | DataStore Preferences |
| 网络请求 | Retrofit + OkHttp |
| 导航 | Navigation Compose |
| 更新分发 | GitHub Releases API + DownloadManager + FileProvider |

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
 
