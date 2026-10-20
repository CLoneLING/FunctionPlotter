package com.yumoo.functionplotter.data.model

data class PlotFunction(
    val id: Long,
    val expression: String,
    val colorArgb: Long,
    val visible: Boolean = true
)