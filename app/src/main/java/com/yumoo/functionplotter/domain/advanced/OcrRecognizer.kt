package com.yumoo.functionplotter.domain.advanced

import android.net.Uri

/**
 * 拍照/相册识别接口
 */
interface OcrRecognizer {
    suspend fun recognize(imageUri: Uri): String?
}