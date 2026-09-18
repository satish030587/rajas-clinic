package com.rajashomoeocare.clinic.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Builds the standard UPI intent string. Filling in the amount is the whole
 * point (spec §4.10) — the patient scans and the total is already correct,
 * which removes the daily source of error in typing it by hand.
 */
fun upiPaymentUri(upiId: String, payeeName: String, amount: Int): String? {
    if (upiId.isBlank() || amount <= 0) return null
    return Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", payeeName.ifBlank { "Clinic" })
        .appendQueryParameter("am", amount.toString())
        .appendQueryParameter("cu", "INR")
        .build()
        .toString()
}

/** Renders [content] as a QR bitmap, or null if it cannot be encoded. */
fun qrBitmap(content: String, sizePx: Int = 512): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        ),
    )
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height) { index ->
        if (matrix[index % width, index / width]) Color.BLACK else Color.WHITE
    }
    createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}.getOrNull()
