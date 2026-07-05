package com.origintag.app.data.api

import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.model.RegisterResponse
import com.origintag.app.data.model.TransferRequest
import com.origintag.app.data.model.TransferResponse
import com.origintag.app.data.model.WarrantyResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path

/** Endpoint backend sesuai PRD §12 */
interface OriginTagApi {

    /**
     * Part 5b — registrasi barang. `photos` = MultipartBody.Part per foto,
     * `fields` = brand, category, serialNumber, purchaseDate,
     * warrantyDurationDays, ownerAddress.
     */
    @Multipart
    @POST("items/register")
    suspend fun registerItem(
        @Part photos: List<MultipartBody.Part>,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    ): RegisterResponse

    @GET("items/{tokenId}")
    suspend fun getPassport(@Path("tokenId") tokenId: String): PassportDto

    @POST("items/{tokenId}/transfer")
    suspend fun transferPassport(
        @Path("tokenId") tokenId: String,
        @Body body: TransferRequest,
    ): TransferResponse

    @GET("items/{tokenId}/warranty")
    suspend fun getWarranty(@Path("tokenId") tokenId: String): WarrantyResponse

    @GET("users/{walletAddress}/passports")
    suspend fun getUserPassports(@Path("walletAddress") walletAddress: String): List<PassportDto>
}
