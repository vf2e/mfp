# 过片（Guopian）

本地视频快速浏览、标记与剪切整理 Android App。

**副标题：** 本地视频，滑动过一遍就整理好  
**包名：** `com.guopian.app`  
**最低系统：** Android 8.0（API 26）

## 文档

见 [`docs/README.md`](docs/README.md)。

## 当前进度

- [x] M0 工程骨架（Compose / Hilt / Navigation / 主题）
- [x] M1 起步：SAF 选根目录、Room、扫描 Worker、视频库网格
- [x] M2：快捷筛选芯片、路径/搜索、多选批量标记
- [x] M3：短视频式上下滑动浏览 + ExoPlayer + 双击保留/标记栏
- [x] `assembleDebug` 已通过
- [ ] M4 VLC 兜底播放（**不做缩略图**）
- [ ] M5 剪切任务

### 范围变更

- **不做缩略图**：列表仅用扩展名/标记占位，不截帧、不缓存封面；已移除 Coil 依赖。

## 环境要求

- JDK **17**（推荐 `C:\Program Files\Microsoft\jdk-17.*`）
- Android SDK（compileSdk / targetSdk 35）
- 推荐用 Android Studio 打开本目录

> 若 Gradle 报连不上仓库，检查 `~/.gradle/gradle.properties` 里是否配置了已关闭的本地代理（如 `127.0.0.1:7890`）。本机若代理未开，需注释掉这些行。

## 编译

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
# 若已生成 wrapper：
.\gradlew.bat assembleDebug
# 或使用本机 Gradle 8.11.1：
# & "$env:TEMP\gradle-8.11.1\bin\gradle.bat" assembleDebug
```

Debug APK 输出：

`app/build/outputs/apk/debug/app-debug.apk`

## 模块结构（单 module 包分区）

```
app/src/main/java/com/guopian/app/
  ui/           # Compose 界面
  data/         # Room / DataStore / Repository / SAF
  domain/       # 领域模型
  work/         # WorkManager
  media/        # 播放引擎（后续）
  di/           # Hilt
  core/         # 工具
```
