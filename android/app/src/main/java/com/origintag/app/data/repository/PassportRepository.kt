package com.origintag.app.data.repository

import com.origintag.app.data.api.OriginTagApi
import com.origintag.app.data.model.TransferRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassportRepository @Inject constructor(
    private val api: OriginTagApi,
) {
    suspend fun getPassports(walletAddress: String) = api.getUserPassports(walletAddress)

    suspend fun getPassport(tokenId: String) = api.getPassport(tokenId)

    suspend fun getWarranty(tokenId: String) = api.getWarranty(tokenId)

    suspend fun transfer(tokenId: String, toAddress: String) =
        api.transferPassport(tokenId, TransferRequest(toAddress))

    // TODO(Part 5b): helper registerItem — kompresi foto (Bitmap → JPEG quality ~80)
    // lalu bungkus jadi MultipartBody.Part sebelum api.registerItem(...).
}
