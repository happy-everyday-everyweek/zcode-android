# zcode-android

把 ZCode（Z.ai 的 coding agent harness）编译成 Android 原生二进制并内嵌到 APK 中运行。

## 目标

ZCode 桌面端是 Node.js Single Executable Application（SEA）：esbuild 打包 JS bundle，再用 postject 注入到 Node 二进制。本项目把 ZCode 的 `linux-arm64` SEA 二进制放进 APK 的 assets，在 Android 应用私有目录（filesDir）中执行，观察它在移动端的运行情况。

## 结构

- `app/src/main/java/com/zcode/android/MainActivity.kt` — Kotlin 壳：按钮触发，从 assets 解压 `zcode-linux-arm64` 到 filesDir，chmod +x，exec，捕获 stdout/stderr/exitCode 显示在界面
- `app/build.gradle` — `targetSdk 28`（Android 9 之前允许在应用私有目录直接 exec 二进制，绕过 Android 10+ 的 W^X 限制）
- `.github/workflows/build-apk.yml` — GHA 云端构建

## 构建（GHA）

推送到 main 或手动触发 workflow，GHA 在云端完成：

1. checkout `zai-org/ZCode`
2. `pnpm install` + turbo 构建依赖包 + esbuild 打包 `zcode.cjs`
3. `node scripts/build-sea.mjs --target linux-arm64`（下载 Node linux-arm64 + postject 注入 SEA blob）
4. 产物 `zcode-linux-arm64` 复制到 `app/src/main/assets/`
5. `gradlew :app:assembleDebug`
6. APK 上传为 artifact `zcode-android-apk`

## 已知约束

- ZCode SEA 底层是 Node 官方 `linux-arm64` 二进制，动态链接 **glibc**（libc.so.6 / ld-linux-aarch64.so.1）。Android 用 **Bionic libc**，因此 glibc 二进制在 Android 上直接执行会报解释器/libc 找不到——这正是本实验要观察的报错。
- ZCode 的 native 依赖（native-search: bfs/rg/ugrep，node-pty，playwright-core）在 Android bionic 环境下需要单独处理。