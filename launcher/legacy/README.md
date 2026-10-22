# Android 4.4 兼容模块

包名 `com.gathin.tvhome.legacy`，最低 API 19，当前版本 0.5-kitkat。基于 CleanYourTV 的桌面概念与字体重写核心界面，原版 `app` 模块保留。

## 操作

方向键选择，确认打开，长按确认编辑常用栏，菜单键显示所有应用。首次启动优先显示已安装的绿联云、小电视、腾讯视频和酷喵；用户编辑后保存配置。

暴风设置入口为 `com.baofengtv.settings.MainActivity`，网络使用 `page=home, group=0`；信号源为 `com.baofengtv.sourceswitch.MainActivity`。

## 独立编译（本地真机验证采用此方式）

```sh
JAVA_HOME=/path/to/jdk17 \
ANDROID_JAR=/path/to/android-19/android.jar \
BUILD_TOOLS=/path/to/build-tools/29.0.3 \
AAPT2=/path/to/native/aapt2 \
./build-standalone.sh
```

使用 API 19 的 android.jar 编译，防止误用高版本框架 API。Apple Silicon 可使用 Maven `com.android.tools.build:aapt2:8.5.2-11315950:osx` 中的通用二进制。

输出 `build/standalone/cleanyourtv-kitkat.apk`。脚本在忽略的 build 目录生成开发签名密钥；请自行备份以便后续覆盖升级。也可使用 `LEGACY_KEYSTORE` 指定开发密钥（别名 legacy、开发密码 android）。此方式用于个人开发测试，不是正式生产签名方案。

```sh
adb install -r build/standalone/cleanyourtv-kitkat.apk
adb shell am start -n com.gathin.tvhome.legacy/.LegacyHomeActivity
```

## Home 接管与恢复

`BootReceiver` 在开机后启动桌面。`HomeRedirectService` 仅订阅暴风首页和 Launcher3 的窗口切换，匹配首页 Activity 后打开轻简版。不请求读取窗口内容，不联网。需用户在系统无障碍设置中主动启用。通过 ADB 配置时必须保留所有已有服务，不得直接覆盖服务列表。

关闭此无障碍服务即可停止接管，原厂桌面没有被此 APK 停用。卸载：`adb uninstall com.gathin.tvhome.legacy`。

## 已验证与局限

在 BAOFENG_TV MST_6A358 / Android 4.4.4 上验证了首页、方向键、应用启动、应用列表、长按编辑、原厂设置与信号源、重启后自动启动及 Home 接管。其他机型尚未实测。标准 Gradle 构建路径尚未在本次环境执行，实际验证使用上面的独立脚本。

0.5 将窗口事件通知等待从 100ms 改为 0，并缩短重复事件抑制时间；厂商首页仍可能短暂出现。原版主题、插件、复杂动画和屏保不包含在此移植中。关闭推广屏保和停用广告组件属于设备配置，不由 APK 自动执行；不能宣称广告已全部移除。

MIT 许可证和 Outfit 字体 SIL OFL 许可证随 APK 放入 assets。
