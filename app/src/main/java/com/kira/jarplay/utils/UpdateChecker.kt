package com.kira.jarplay.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<AppVersion> {

    companion object {
        // 版本号形如 "1.0.4"、"v1.0.4"、"1.0.4-open"、"v1.0.4-beta.1"、"1.0.3.RB"。
        // 只取前导的 major.minor.patch，忽略构建变体 / 预发布等后缀 —— 是否为预发布
        // 由 GitHub release 的 prerelease 字段判定，不靠 tag 文本猜。
        // （旧实现把结尾锚死成 "$"，带 "-open" 这类后缀的版本号一律解析失败，
        //   于是 checkForUpdates 直接返回 CheckFailed，更新按钮表现得像空壳。）
        private val VERSION_REGEX = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""")

        fun parse(versionName: String): AppVersion? {
            val match = VERSION_REGEX.find(versionName.trim().trimStart('v', 'V'))
                ?: return null
            return AppVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: 0,
            )
        }
    }

    override fun compareTo(other: AppVersion): Int {
        return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }

    fun toVersionName(): String = "$major.$minor.$patch"
}

sealed class UpdateResult {
    data class UpdateAvailable(
        val latestVersion: AppVersion,
        val latestVersionName: String,
        val currentVersion: AppVersion,
        val currentVersionName: String,
        val releaseUrl: String,
        val apkDownloadUrl: String,
        val releaseNotes: String,
        val isBeta: Boolean
    ) : UpdateResult()

    data class UpToDate(
        val currentVersion: AppVersion,
        val currentVersionName: String
    ) : UpdateResult()

    data object CheckFailed : UpdateResult()
}

object UpdateChecker {
    private const val GITHUB_REPO = "TiG-Kira/JarPlay"
    private const val GITHUB_RELEASES_LIST_URL = "https://api.github.com/repos/$GITHUB_REPO/releases?per_page=50"
    private const val GITHUB_RELEASE_PAGE_URL = "https://github.com/$GITHUB_REPO/releases"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    enum class ReleaseStatus {
        NORMAL,
        PRERELEASE,
        NOT_FOUND
    }

    suspend fun checkForUpdates(
        currentVersionName: String,
        betaEnabled: Boolean = false
    ): UpdateResult {
        val currentVersion = AppVersion.parse(currentVersionName)
            ?: return UpdateResult.CheckFailed

        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_LIST_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext UpdateResult.CheckFailed
                    }

                    val body = response.body?.string() ?: ""
                    val releases = JSONArray(body)

                    var bestRelease: JSONObject? = null
                    var bestVersion: AppVersion? = null
                    var bestVersionName = ""
                    var bestIsPreRelease = false

                    for (i in 0 until releases.length()) {
                        val release = releases.getJSONObject(i)
                        val isDraft = release.optBoolean("draft", false)
                        if (isDraft) continue

                        val tagName = release.optString("tag_name", "")
                        val tagVersion = AppVersion.parse(tagName)

                        if (tagVersion == null) continue

                        val isPreRelease = release.optBoolean("prerelease", false)
                        if (!betaEnabled && isPreRelease) continue

                        if (bestVersion == null || tagVersion > bestVersion) {
                            bestVersion = tagVersion
                            bestVersionName = tagName
                            bestRelease = release
                            bestIsPreRelease = isPreRelease
                        }
                    }

                    if (bestVersion == null || bestVersion <= currentVersion) {
                        return@withContext UpdateResult.UpToDate(
                            currentVersion = currentVersion,
                            currentVersionName = currentVersionName
                        )
                    }

                    val release = bestRelease!!
                    val releaseUrl = release.optString("html_url", "")
                        .ifEmpty { GITHUB_RELEASE_PAGE_URL }
                    val releaseNotes = release.optString("body", "")

                    val assets = release.optJSONArray("assets")
                    var apkUrl = ""
                    if (assets != null && assets.length() > 0) {
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    UpdateResult.UpdateAvailable(
                        latestVersion = bestVersion,
                        latestVersionName = bestVersionName,
                        currentVersion = currentVersion,
                        currentVersionName = currentVersionName,
                        releaseUrl = releaseUrl,
                        apkDownloadUrl = apkUrl,
                        releaseNotes = releaseNotes,
                        isBeta = bestIsPreRelease
                    )
                }
            }
        } catch (e: Exception) {
            UpdateResult.CheckFailed
        }
    }

    suspend fun getReleaseStatus(currentVersionName: String): ReleaseStatus? {
        val currentVersion = AppVersion.parse(currentVersionName)
            ?: return null

        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_LIST_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val body = response.body?.string() ?: ""
                    val releases = JSONArray(body)

                    for (i in 0 until releases.length()) {
                        val release = releases.getJSONObject(i)
                        val tagName = release.optString("tag_name", "")
                        val tagVersion = AppVersion.parse(tagName)

                        if (tagVersion != null && tagVersion == currentVersion) {
                            val isPrerelease = release.optBoolean("prerelease", false)
                            val isDraft = release.optBoolean("draft", false)
                            if (isDraft) {
                                return@withContext ReleaseStatus.NOT_FOUND
                            }
                            return@withContext if (isPrerelease) {
                                ReleaseStatus.PRERELEASE
                            } else {
                                ReleaseStatus.NORMAL
                            }
                        }
                    }

                    ReleaseStatus.NOT_FOUND
                }
            }
        } catch (e: Exception) {
            null
        }
    }

}
