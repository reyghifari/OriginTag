package com.origintag.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.origintag.app.data.api.OriginTagApi
import com.origintag.app.data.model.RegisterResponse
import com.origintag.app.data.model.TransferRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassportRepository @Inject constructor(
    private val api: OriginTagApi,
    @ApplicationContext private val context: Context,
) {
    suspend fun getPassports(walletAddress: String) = api.getUserPassports(walletAddress)

    suspend fun getPassport(tokenId: String) = api.getPassport(tokenId)

    suspend fun getWarranty(tokenId: String) = api.getWarranty(tokenId)

    suspend fun getPhotoCount(tokenId: String) = api.getPhotoCount(tokenId)

    suspend fun transfer(tokenId: String, toAddress: String) =
        api.transferPassport(tokenId, TransferRequest(toAddress))

    /**
     * Part 5b — kompres tiap foto (JPEG quality ~80, longest edge maks 1600px untuk
     * menekan ukuran upload dari kamera HP) lalu kirim multipart ke /items/register.
     */
    suspend fun registerItem(
        photoUris: List<Uri>,
        brand: String,
        category: String,
        serialNumber: String,
        purchaseDate: String,
        warrantyDurationDays: Int,
        ownerAddress: String,
    ): RegisterResponse = withContext(Dispatchers.IO) {
        val photoParts = photoUris.mapIndexed { index, uri ->
            val jpeg = compressToJpeg(uri)
            val body = jpeg.toRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("photos", "photo_$index.jpg", body)
        }

        fun field(value: String): RequestBody = value.toRequestBody("text/plain".toMediaType())
        val fields = mapOf(
            "brand" to field(brand),
            "category" to field(category),
            "serialNumber" to field(serialNumber),
            "purchaseDate" to field(purchaseDate),
            "warrantyDurationDays" to field(warrantyDurationDays.toString()),
            "ownerAddress" to field(ownerAddress),
        )

        api.registerItem(photoParts, fields)
    }

    private fun compressToJpeg(uri: Uri, maxEdge: Int = 1600, quality: Int = 80): ByteArray {
        val original = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Tidak bisa membaca gambar: $uri")

        val scaled = scaleDown(original, maxEdge)
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (scaled != original) scaled.recycle()
            original.recycle()
            out.toByteArray()
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true,
        )
    }
}
