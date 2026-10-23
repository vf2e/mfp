# 过图（Guotu）

本地图片快速浏览、标记与剪切整理 Android App。

**副标题：** 本地图片，滑动过一遍就整理好  
**包名：** `com.guotu.app`  
**最低系统：** Android 8.0（API 26）

面向**几十万张**深层级本地图片库：选总文件夹 → 递归扫描扁平索引 → 网格浏览 / 上下滑过图 → 边看边标 → 一键剪切归档。

## 当前能力

- [x] SAF / 全部文件访问选根目录
- [x] WorkManager 流式扫描入库（分批 upsert，适合超大图库）
- [x] Room + Paging 图库网格（Coil 缩略图）
- [x] 快捷筛选：全部 / 收藏 / 待定 / 待删除 / 已剪切
- [x] 上下滑全屏过图 + 双击标记 + 底栏收藏/待定/剪切
- [x] 批量剪切任务
- [x] `assembleDebug`（见下方编译）

## 环境要求

- JDK **17**
- Android SDK（compileSdk / targetSdk 35）
- 推荐用 Android Studio 打开本目录

> 若 Gradle 报连不上仓库，检查 `~/.gradle/gradle.properties` 里是否配置了已关闭的本地代理。

## 编译

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
.\gradlew.bat assembleDebug
```

Debug APK：

`app/build/outputs/apk/debug/app-debug.apk`

## 模块结构

```
app/src/main/java/com/guotu/app/
  ui/           # Compose 界面（图库 / 滑动过图 / 设置…）
  data/         # Room / DataStore / Repository / SAF
  domain/       # 领域模型
  work/         # WorkManager（扫描 / 剪切）
  player/       # PlaylistSession（过图分页会话）
  di/           # Hilt
  core/         # 工具
```

姊妹项目：[过片 Guopian](../mfv)（视频版）。
