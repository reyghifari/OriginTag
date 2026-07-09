package com.origintag.app.data.api

import com.origintag.app.data.model.BalanceDto
import com.origintag.app.data.model.ListRequest
import com.origintag.app.data.model.MarketplaceListingDto
import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.model.PhotoCountDto
import com.origintag.app.data.model.RegisterResponse
import com.origintag.app.data.model.ServiceRecordsDto
import com.origintag.app.data.model.StatsDto
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
import retrofit2.http.Query

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

    /** Part 4/5c — jumlah foto bukti; app membangun URL /photo/{i} dari sini */
    @GET("items/{tokenId}/photos")
    suspend fun getPhotoCount(@Path("tokenId") tokenId: String): PhotoCountDto

    @GET("users/{walletAddress}/passports")
    suspend fun getUserPassports(@Path("walletAddress") walletAddress: String): List<PassportDto>

    // ── Profil ──
    @GET("users/{walletAddress}/stats")
    suspend fun getStats(@Path("walletAddress") walletAddress: String): StatsDto

    @GET("users/{walletAddress}/balance")
    suspend fun getBalance(@Path("walletAddress") walletAddress: String): BalanceDto

    // ── Marketplace & explore ──
    @GET("marketplace")
    suspend fun getMarketplace(): List<MarketplaceListingDto>

    @GET("explore")
    suspend fun explore(
        @Query("brand") brand: String? = null,
        @Query("category") category: String? = null,
        @Query("minScore") minScore: Int? = null,
    ): List<PassportDto>

    @POST("marketplace/{tokenId}/approve-tx")
    suspend fun approveTx(@Path("tokenId") tokenId: String): TransferResponse

    @POST("marketplace/{tokenId}/list-tx")
    suspend fun listTx(@Path("tokenId") tokenId: String, @Body body: ListRequest): TransferResponse

    @GET("marketplace/{tokenId}/buy-tx")
    suspend fun buyTx(@Path("tokenId") tokenId: String): TransferResponse

    @POST("marketplace/{tokenId}/cancel-tx")
    suspend fun cancelTx(@Path("tokenId") tokenId: String): TransferResponse

    // ── Service records ──
    @GET("items/{tokenId}/service-records")
    suspend fun getServiceRecords(@Path("tokenId") tokenId: String): ServiceRecordsDto
}
