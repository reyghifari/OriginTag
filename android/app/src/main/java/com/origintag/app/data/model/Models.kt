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
)

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
data class UnsignedTx(val to: String, val data: String)

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
