package com.yumoo.functionplotter.data.update

import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubRelease
}