package com.yumoo.functionplotter.data.update

sealed class UpdateResult {
    data object UpToDate : UpdateResult()
    data object NoApkFound : UpdateResult()
    data class Available(
        val version: String,
        val notes: String,
        val downloadUrl: String,
        val htmlUrl: String
    ) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}