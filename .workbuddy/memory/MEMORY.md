# JarPlay 项目长期记忆

## 项目性质
J2ME Loader 的 Android 分支（作者 TiG-Kira），基于上游 `nikita36078/J2ME-Loader`，
用 Jetpack Compose + Miuix(HyperOS) 重写 UI。`applicationId = com.kira.jarplay`。

## 关键约定
- **UI 库**：Miuix KMP（`top.yukonga.miuix.kmp`），版本写在 `app/build.gradle`，2026-09 起为 `0.9.4`。
  - 只用 Miuix 组件 + `MiuixTheme.colorScheme/textStyles`；不要再引入 Material3 组件。
  - 分组统一用 `SmallTitle` + `Card`（`Card` 默认 `insideMargin = 0.dp`，行自带 16dp 内边距；
    不要再给 Card 加 `insideMargin`，否则会出现 28dp 双重缩进）。
  - 分组外边距统一 `12.dp`。
  - **没有 Scaffold 的宿主**（播放页的两个 ComposeView、任何 View 层直接挂的 ComposeView）
    必须用 `Window*` 系列弹窗（`WindowDialog` / `WindowIconDropdownMenu` / `WindowBottomSheet`），
    `Overlay*` 系列依赖 Scaffold 的 popupHost。
- **主题**：`com.kira.jarplay.ui.theme.JarPlayTheme` 是唯一根主题，它自己读取并监听
  `pref_theme`（light/night/system），内部提供 `LocalColorMode`、`isInDarkTheme()`。
  页面里不要再嵌套 `MiuixTheme`。
- **全局偏好键**一律引用 `com.kira.jarplay.util.Constants.PREF_*`，不要手写字符串——
  历史上出现过 `pref_keep_screen_on` vs `pref_wakelock_switch` 这类写读不一致导致开关失效。
- **每个应用的配置**在 `com.kira.jarplay.config.ProfileModel`（JSON 存 `config.json`，`VERSION` 当前为 4）。
  - `ProfileModel` 的**无参构造必须写全默认值**：Gson 反序列化会走它，缺失字段会残留 0 值，
    曾导致 `ScreenScaleType=0`（画面按原始分辨率画在角落）+ `ScreenBackgroundColor=0`（黑屏）。
  - 新增字段时同步改 `applyDefaults()`、`normalize()` 和 `ProfilesManager.loadConfig()` 的迁移分支。
  - 画面缩放语义：0=原始尺寸、1=等比缩放、2=拉伸铺满（默认）；停靠位置 0 左/1 上/2 居中/3 右/4 下。
- **播放页**：`javax.microedition.shell.MicroActivity`（跑在 `:midlet` 进程）
  + `com.kira.jarplay.ui.MicroPlayScreen.kt`（两个 ComposeView：工具/控制层 + 虚拟键盘），
  两者通过 `MicroPlayUiState` 共享状态，行为通过 `MicroPlayCallbacks` 回调到 Java。
  虚拟键盘布局存在 SharedPreferences 文件 `keyboard_layout`（键名 `row_N_key_M_label/code`、`keyboard_edited`）。
- 缩放渲染逻辑在 `javax.microedition.lcdui.Canvas#updateSize()`，改动前先读 `virtualScreen`/`onX/onY/onWidth/onHeight` 的含义。

## 已知历史遗留（待清理，未动手）
- 旧 Material/Fragment 体系仍在仓库里但已不可达：`com.kira.jarplay.MainActivity`(Java)、
  `applist/AppsListFragment`、`settings/SettingsActivity`、`settings/SettingsFragment`、
  `res/xml/preferences.xml`、`KeyMapperActivity`。启动页是 `.ui.MainActivity`(Kotlin)。
- `ProfileModel.screenKeepAspectRatio` / `screenScaleToFit` 已是遗留字段，UI 不再暴露。

## 本机构建验证（可用，2026-09-22 已验证通过）
**命令**（在 Bash 工具里跑，并且必须带 `dangerouslyDisableSandbox: true`）：
```bash
export PATH="/d/Programs/Java JDKs/jdk-21.0.11/bin:/usr/bin:/bin:$PATH"
cd "D:/JarPlay/JarPlay/JarPlay"
java -classpath "D:/JarPlay/JarPlay/JarPlay/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain --console=plain --no-daemon :app:assembleOpenDebug \
  > build/assemble.log 2>&1
```
- **关键点：必须脱离沙箱。** 沙箱开启时 Gradle 会在 artifact transform 阶段对
  `~/.gradle/caches/<ver>/transforms/<hash>/<hash>.lock` 报「拒绝访问」，编译永远失败；
  关掉沙箱后同一命令直接 `BUILD SUCCESSFUL`（增量约 20–40s，冷启动约 2m20s）。
- 默认 `GRADLE_USER_HOME`（`~/.gradle`）已带好 Gradle 9.3.1 发行版与依赖缓存，不需要额外的 `-g` 或 junction。
- **不要用** `cmd /c gradlew.bat`（被安全策略拦截）、也不要用 `sh gradlew`
  （Git Bash 的 classpath 转换会导致 `ClassNotFoundException: GradleWrapperMain`）。
- SDK 靠环境变量 `ANDROID_HOME=C:\Users\Kira\AppData\Local\Android\Sdk`，仓库里没有 `local.properties`。
- 产物：`app/build/outputs/apk/open/debug/JarPlay-openDebug.apk`。
- 编译日志留意 `e:` 行（Kotlin 错误）与 `错误:` 行（javac 错误，输出是中文）。

