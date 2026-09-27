package com.origintag.app.wallet

import com.origintag.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Part 5d — menandatangani & broadcast transaksi ke BSC Testnet langsung dari app.
 * Transfer passport HARUS ditandatangani wallet pemilik (validasi ownerOf on-chain,
 * PRD §8.1); backend hanya menyusun calldata, penandatanganan terjadi di sini.
 */
@Singleton
class TransactionSigner @Inject constructor() {

    private val web3j: Web3j by lazy { Web3j.build(HttpService(BuildConfig.BSC_RPC_URL)) }

    /**
     * Kirim transaksi ke `toContract` dengan `data` (calldata dari backend),
     * ditandatangani `privateKey`. `valueWei` = jumlah BNB (wei desimal) yang dikirim
     * — dipakai untuk buyItem marketplace; null/0 untuk transfer/approve/list.
     * Menunggu sampai tx masuk blok (status sukses) lalu mengembalikan tx hash,
     * supaya pemanggil yang me-refresh data langsung melihat state chain terbaru.
     */
    suspend fun signAndSend(
        toContract: String,
        data: String,
        privateKey: String,
        valueWei: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val credentials = Credentials.create(privateKey)

        val nonce = web3j
            .ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
            .send()
            .transactionCount
        val gasPrice = web3j.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(250_000)
        val value = if (valueWei.isNullOrBlank()) BigInteger.ZERO else BigInteger(valueWei)

        val rawTx = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            toContract,
            value,
            data,
        )

        val signed = TransactionEncoder.signMessage(rawTx, BuildConfig.BSC_CHAIN_ID, credentials)
        val sent = web3j.ethSendRawTransaction(Numeric.toHexString(signed)).send()
        if (sent.hasError()) {
            error("Transaksi ditolak: ${sent.error.message}")
        }
        val hash = sent.transactionHash

        // BSC Testnet ~3 dtk/blok; 60 dtk cukup longgar.
        repeat(60) {
            val receipt = web3j.ethGetTransactionReceipt(hash).send().transactionReceipt
            if (receipt.isPresent) {
                if (!receipt.get().isStatusOK) error("Transaksi gagal di chain (revert): $hash")
                return@withContext hash
            }
            delay(1_000)
        }
        error("Transaksi belum terkonfirmasi setelah 60 detik: $hash")
    }
}
