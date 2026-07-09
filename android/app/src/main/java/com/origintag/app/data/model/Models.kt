package com.origintag.app.data.model

import kotlinx.serialization.Serializable

/** DTO selaras dengan response backend (backend/src/blockchain/blockchain.service.ts) */
@Serializable
data class PassportDto(
    val tokenId: String,
    val owner: String? = null,
    val brand: String,
    val category: String,
    val serialNumber: String? = null,
    val authenticityScore: Int,
    val evidenceObjectId: String? = null,
    val remainingWarrantySeconds: Long? = null,
    val ownershipHistory: List<String> = emptyList(),
    val isFlaggedForReview: Boolean = false,
    val recall: RecallDto? = null,
)

@Serializable
data class RecallDto(val brand: String, val category: String, val reason: String)

/** Item marketplace = passport + harga jual */
@Serializable
data class MarketplaceListingDto(
    val tokenId: String,
    val brand: String,
    val category: String,
    val authenticityScore: Int,
    val priceBnb: String,
    val price: String,
    val seller: String,
)

@Serializable
data class StatsDto(
    val owned: Int,
    val avgScore: Int,
    val highScoreCount: Int,
    val salesCount: Int,
    val trustScore: Int,
    val trustLabel: String,
)

@Serializable
data class BalanceDto(val wei: String, val bnb: String)

@Serializable
data class ServiceRecordsDto(val records: List<String> = emptyList())

@Serializable
data class ListRequest(val priceBnb: String)

@Serializable
data class AiResultDto(
    val score: Int,
    val conditionSummary: String,
    val reasons: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
)

@Serializable
data class RegisterResponse(
    val status: String, // "minted" | "flagged_for_review"
    val tokenId: String? = null,
    val txHash: String? = null,
    val aiResult: AiResultDto? = null,
    val verifyUrl: String? = null,
)

@Serializable
data class TransferRequest(val toAddress: String)

@Serializable
data class UnsignedTx(val to: String, val data: String, val value: String? = null)

@Serializable
data class TransferResponse(
    val txHash: String? = null,
    val unsignedTx: UnsignedTx? = null,
    val note: String? = null,
)

@Serializable
data class WarrantyResponse(val tokenId: String, val remainingSeconds: Long)

@Serializable
data class PhotoCountDto(val count: Int)
