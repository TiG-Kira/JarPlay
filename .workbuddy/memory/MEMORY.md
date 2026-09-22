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

## 本机构建验证（重要：不要在会话里尝试编译）
这台机器上 Gradle 编译**跑不通**，实测结论：
- Bash 工具的 PATH 是坏的（`dirname`/`ls`/`git` 都找不到），需要 `export PATH="/usr/bin:/bin:<git>/cmd:<git>/usr/bin:$PATH"`。
- `cmd.exe` 被安全策略拦截，不能用 `cmd /c gradlew.bat`。
- 用 `sh gradlew` 会因 Git Bash 的 classpath 转换导致 `ClassNotFoundException: GradleWrapperMain`，
  正确姿势是直接：
  `java -classpath "<repo>/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain --console=plain --no-daemon -g <HOME> :app:compileOpenDebugKotlin`
- `services.gradle.org` 在此网络下下载发行版为 0 字节；可把 `~/.gradle/wrapper` 与 `~/.gradle/caches/modules-2`
  用 `_winapi.CreateJunction` junction 到一个**短路径**的 GRADLE_USER_HOME（如 `D:/gh`；长路径会触发 Windows MAX_PATH 报错）。
- 即便如此，Gradle 仍会在 artifact transform 阶段对 `caches/<ver>/transforms/<hash>/<hash>.lock`
  报「拒绝访问」（python 写同名文件正常，疑似环境对 Gradle 子进程的文件操作限制）。重跑能逐步推进，
  最远能到 `:app:compileOpenDebugKotlin`（资源任务 `generateOpenDebugResources` 等都能通过），
  但 Kotlin 编译任务本身永远失败，拿不到真正的编译诊断。
- **用户（2026-09-22）明确要求不要再循环尝试编译。** 需要编译验证请在 Android Studio 里做。

