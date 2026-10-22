package com.yumoo.functionplotter.data.update

import com.google.gson.annotations.SerializedName

/**
 * GitHub Release 响应。
 * 字段全部可空：Gson 会绕过 Kotlin 构造器，接口少返回字段时非空类型会直接 NPE。
 */
data class GithubRelease(
    @SerializedName("tag_name") val tagName: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("html_url") val htmlUrl: String?,
    @SerializedName("assets") val assets: List<GithubAsset>?
)

data class GithubAsset(
    @SerializedName("name") val name: String?,
    @SerializedName("browser_download_url") val browserDownloadUrl: String?
)
