package com.github.raftcrast.repl_yes.github

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubLatestReleases(
    val assets: Array<GithubReleaseAsset>,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GithubLatestReleases

        if (!assets.contentEquals(other.assets)) return false

        return true
    }

    override fun hashCode(): Int {
        return assets.contentHashCode()
    }
}
