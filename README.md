# 鱼团团 · AiLivePet

一只从对话框里爬出来的粉色桌宠。悬浮在安卓所有 App 之上，戳它会说话、会闹、会假装生气。

本工程参考 **[AI-Live-Overflow](https://github.com/Vael-KY/AI-Live-Overflow)** 的架构蓝图搭建，
把蓝图里那只"渲染层身体"落地成了一套**可编译的 Android 工程**，
主角形象是 **鱼团团**（`app/src/main/assets/pet.html`）。

---

## 它是什么

- **一个前台服务 + 透明悬浮 WebView**（`OverlayService.kt`）
- 渲染层是鱼团团页面（`assets/pet.html`），SVG + CSS 动画，无任何外部依赖
- **手势**：拖动、单击、双击、长按、甩出后自己爬回
- **台词**：100+ 句，按热度分档递进，带彩蛋（戳满 30 / 60 / 100 次各触发一句）

## 它不是什么

- 它**不是**一个打开就能用的现成 App —— 需要先编译出 APK 并安装
- 悬浮窗权限、通知权限**必须你在手机上手动授予**（系统不允许全自动）

---

## 怎么编译出 APK

### 方式 A：GitHub Actions 云编译（无需电脑，推荐）

1. 把本工程推到 GitHub 仓库（**注意：要传整个工程文件，别只传 zip**）
2. 仓库自带工作流 `.github/workflows/build.yml`
3. 推上去后，进仓库 **Actions** 页，等约 2～3 分钟
4. 在 **Artifacts** 里下载 `livepet-apk`，解压得到 `app-debug.apk`
5. 把这个 APK 传到手机，安装

### 方式 B：本地用 Android Studio / Gradle

```
# 需要 JDK 17 + Android SDK 35
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> 注意：Android 官方的 `aapt2` 目前**没有 arm64 原生版**，
> 所以**手机上直接编译会失败**（`aapt2 daemon startup failed`）。
> 手机用户请走方式 A（云编译）。

---

## 怎么装、怎么让它出来

1. 安装 APK（会提示"未知来源"，允许即可）
2. 打开 App「鱼团团」
3. 点 **① 授予悬浮窗权限** → 在系统页打开开关 → 返回
4. 点 **② 召唤鱼团团**
5. 切到任何 App，鱼团团就趴在屏幕角落了

**它怎么动：**

- **戳一下** → 它说话（热度越高，语气越不一样）
- **双击** → 讨抱抱
- **长按** → 认输
- **拖着走** → 抱怨你搬它
- **飞快甩出去** → 它自己爬回屏幕内

---

## 目录结构

```
app/src/main/
  AndroidManifest.xml              ← 权限、服务声明
  java/com/qiyu/livepet/
    MainActivity.kt                ← 授权引导页
    OverlayService.kt              ← 悬浮窗核心 + 手势状态机
  assets/
    pet.html                       ← ★ 鱼团团本体（改这里就是改它）
  res/                             ← 布局、主题、图标
.github/workflows/build.yml        ← 云编译
gradle/wrapper/                    ← Gradle Wrapper（真 jar，已带）
```

**想改表情/台词/颜色，只改 `assets/pet.html` 就够了。**
它是独立的一份 HTML，改完重新编译即可。

---

## 已知限制

- 华为 / 小米 / OPPO / 一加 等 ROM 会杀后台，需要把本 App 加入电池白名单
  （部分机型还要单独允许"在其他应用上层显示"）
- 安卓 8.0 以上（minSdk 26）
- 悬浮窗盖不住系统级弹窗，这是系统限制
- 目前是"纯本地反应"，还没接后端。想接入双向通信可以再加

---

## 协议

跟随原蓝图仓库 **CC BY-NC-SA 4.0**：可用、可改、可分享、需署名、不可商用。
