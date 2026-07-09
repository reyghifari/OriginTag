package com.origintag.app.wallet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.web3j.crypto.Credentials
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Part 5a — sumber kebenaran sesi wallet untuk seluruh app.
 *
 * Sesi TIDAK disimpan di DataStore sendiri — kita andalkan sesi Web3Auth: saat
 * app dibuka, MainActivity memanggil web3Auth.initialize() (yang memulihkan sesi
 * Web3Auth yang tersimpan internal SDK), lalu memanggil restoreFromWeb3Auth().
 * Jadi status login selalu konsisten dengan ketersediaan private key untuk
 * tanda tangan transaksi — tidak ada risiko "alamat tersimpan tapi sesi mati".
 */
@Singleton
class WalletManager @Inject constructor() {

    /** Info user dari Web3Auth (social login) untuk halaman profil. */
    data class UserProfile(
        val name: String? = null,
        val email: String? = null,
        val profileImage: String? = null,
    )

    sealed interface SessionState {
        /** Belum diketahui — initialize() Web3Auth masih berjalan */
        data object Checking : SessionState
        data class LoggedIn(val address: String, val profile: UserProfile? = null) : SessionState
        data object LoggedOut : SessionState
    }

    private val _session = MutableStateFlow<SessionState>(SessionState.Checking)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    /** Alamat wallet aktif, null jika belum login / masih dicek. Dibaca Dashboard & Register. */
    val currentAddress: String?
        get() = (_session.value as? SessionState.LoggedIn)?.address

    /** Profil user aktif (nama/email/foto dari Web3Auth), null jika mode demo. */
    val currentProfile: UserProfile?
        get() = (_session.value as? SessionState.LoggedIn)?.profile

    /** Turunkan alamat EVM dari private key secp256k1 (hex) milik Web3Auth. */
    fun addressFromPrivateKey(privateKey: String): String =
        Credentials.create(privateKey).address

    /**
     * Dipanggil MainActivity setelah web3Auth.initialize() selesai.
     * @param privateKey key dari sesi yang dipulihkan, atau null jika tidak ada sesi.
     */
    fun restoreFromWeb3Auth(privateKey: String?, profile: UserProfile? = null) {
        _session.value = if (!privateKey.isNullOrBlank()) {
            SessionState.LoggedIn(addressFromPrivateKey(privateKey), profile)
        } else {
            SessionState.LoggedOut
        }
    }

    /** Set sesi login (login baru via Web3Auth, atau wallet demo). */
    fun loginWithAddress(address: String, profile: UserProfile? = null) {
        _session.value = SessionState.LoggedIn(address, profile)
    }

    fun logout() {
        _session.value = SessionState.LoggedOut
    }
}
