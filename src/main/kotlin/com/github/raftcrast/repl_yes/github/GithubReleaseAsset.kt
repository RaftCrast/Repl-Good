package com.github.raftcrast.repl_yes.github

import com.squareup.moshi.Json

data class GithubReleaseAsset(
    @Json(name = "url") val assetInfoUrl: String,
    val id: Long,
    @Json(name = "node_id") val nodeId: String,
    val name: String,
    val label: String,
    val uploader: GithubUser,
    @Json(name = "content_type") val contentType: String,
    val state: String,
    val size: Long,
    @Json(name = "download_count") val downloadCount: Long,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "browser_download_url") val downloadUrl: String
)
