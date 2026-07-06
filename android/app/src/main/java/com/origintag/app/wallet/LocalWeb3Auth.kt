package com.origintag.app.wallet

import androidx.compose.runtime.compositionLocalOf
import com.web3auth.core.Web3Auth

/**
 * Web3Auth harus dimiliki Activity (butuh Activity context + CustomTab, dan
 * menyimpannya di singleton akan me-leak Activity). Disediakan ke pohon Compose
 * lewat CompositionLocal ini oleh MainActivity.
 */
val LocalWeb3Auth = compositionLocalOf<Web3Auth?> { null }
