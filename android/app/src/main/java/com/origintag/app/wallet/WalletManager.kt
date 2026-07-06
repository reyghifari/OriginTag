package com.origintag.app.wallet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.web3j.crypto.Credentials
import javax.inject.Inject
import javax.inject.Singleton

private val Context.walletDataStore by preferencesDataStore(name = "wallet_session")

/**
 * Part 5a — sumber kebenaran sesi wallet untuk seluruh app.
 * Menyimpan alamat wallet (hasil login Web3Auth) di DataStore, dan menurunkan
 * alamat ETH dari private key via web3j. Tidak menyimpan private key ke disk.
 */
@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val addressKey = stringPreferencesKey("wallet_address")

    /** Alamat wallet aktif, atau null jika belum login. Diamati Dashboard dsb. */
    val address: Flow<String?> = context.walletDataStore.data.map { it[addressKey] }

    /** Turunkan alamat EVM dari private key secp256k1 (hex) yang diberi Web3Auth. */
    fun addressFromPrivateKey(privateKey: String): String =
        Credentials.create(privateKey).address

    suspend fun saveSession(address: String) {
        context.walletDataStore.edit { it[addressKey] = address }
    }

    suspend fun clear() {
        context.walletDataStore.edit { it.remove(addressKey) }
    }
}
