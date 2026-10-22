# CleanYourTV KitKat — Android 4.4 轻简版

基于 [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的 Android 4.4 / API 19 兼容版本。保留上游源码和历史，新增独立的 `launcher/legacy` 模块；它实现原版桌面的核心布局，不包含原版全部功能。

## 功能

- 深色首页、四个可编辑常用应用、全部应用网格、中文时钟。
- 遥控器方向键选择，确认启动，长按编辑，菜单键打开全部应用。
- 暴风电视专用网络、设置和信号源入口，其他机型提供通用入口回退。
- 开机自动启动；可选的无障碍 Home 接管，仅识别原厂首页窗口，不读取屏幕内容。
- 无 AndroidX 依赖，无网络请求，API 19 原生控件。

已在 **BAOFENG_TV MST_6A358 / Android 4.4.4** 上实测安装、应用启动、设置、网络、信号源及开机接管。

## 版本与限制

当前兼容模块：**0.5-kitkat**。这是核心功能移植，原版主题、插件卡片、复杂动画和屏保未移植。厂商固件可能先显示原厂首页再被接管，不能保证完全没有闪现。此项目不会自动移除广告、停用系统应用或升级固件。

Home 接管属于可选无障碍功能，应在理解用途后启用，保留已有服务配置；不需要时可在系统无障碍设置中关闭。

## 编译与安装

需要 JDK 17、Android SDK 34 和项目指定的 Gradle：

```sh
cd launcher
./gradlew :legacy:assembleDebug
adb install -r legacy/build/outputs/apk/debug/legacy-debug.apk
adb shell am start -n com.gathin.tvhome.legacy/.LegacyHomeActivity
```

另提供直接使用 API 19 平台编译的 [独立构建脚本](launcher/legacy/build-standalone.sh)。详见 [兼容模块说明](launcher/legacy/README.md)。

签名密钥不会提交到仓库。自行编译的 APK 与其他来源 APK 签名可能不同，无法直接覆盖安装；卸载会删除常用应用配置。

## 源码与许可证

- [Android 4.4 兼容模块](launcher/legacy/)
- [上游项目说明](README.upstream.md)
- [MIT 许可证与上游版权](LICENSE)
- [Outfit 字体许可证](launcher/FONT-LICENSE-OFL.txt)

本仓库不包含电视日志、网络配置、设备备份、ADB 密钥或签名私钥。
