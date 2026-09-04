package com.fortresstv.player.transfer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates a clean black-on-white QR code Bitmap using ZXing Core.
 *
 * - High error correction (Level H).
 * - Generous quiet zone (4 modules, enforced by explicit padding).
 * - No logos or overlays on the QR module grid.
 * - Returns null if encoding fails so the caller can show a fallback.
 */
object QrCodeGenerator {

    fun generate(content: String, sizePx: Int = 512): Bitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 4,       // 4-module quiet zone
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    fun generateImageBitmap(content: String, sizePx: Int = 512): ImageBitmap? =
        generate(content, sizePx)?.asImageBitmap()
}
