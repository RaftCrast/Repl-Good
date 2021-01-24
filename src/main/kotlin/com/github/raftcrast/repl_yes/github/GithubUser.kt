package com.github.raftcrast.repl_yes.github

import com.squareup.moshi.Json

data class GithubUser(
    @Json(name = "login") val name: String,
    val id: Long,
)
