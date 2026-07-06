package com.origintag.app.feature.scan

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Part 5e — analyzer ML Kit yang mendeteksi QR dan mengekstrak tokenId dari
 * URL verifikasi (origintag.app/verify/{tokenId}) atau angka mentah.
 * onFound dipanggil sekali saja (analyzer berhenti setelah match pertama).
 */
class QrAnalyzer(private val onFound: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    @Volatile private var handled = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || handled) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { it.valueType == Barcode.TYPE_URL || it.valueType == Barcode.TYPE_TEXT }
                    ?.rawValue
                    ?.let(::extractTokenId)
                    ?.let { tokenId ->
                        if (!handled) {
                            handled = true
                            onFound(tokenId)
                        }
                    }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun extractTokenId(raw: String): String? {
        // Format QR: https://origintag.app/verify/{tokenId} atau angka mentah
        val verifyMatch = Regex("/verify/([0-9]+)").find(raw)
        if (verifyMatch != null) return verifyMatch.groupValues[1]
        return raw.trim().takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
    }
}
