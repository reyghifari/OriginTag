package com.origintag.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.BuildConfig
import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.repository.PassportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part 5c — detail passport */
@HiltViewModel
class PassportDetailViewModel @Inject constructor(
    private val repository: PassportRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val passport: PassportDto? = null,
        val photoUrls: List<String> = emptyList(),
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(tokenId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getPassport(tokenId) }
                .onSuccess { p ->
                    _uiState.update { it.copy(loading = false, passport = p) }
                    loadPhotos(tokenId) // best-effort, tidak menggagalkan detail
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") }
                }
        }
    }

    private suspend fun loadPhotos(tokenId: String) {
        runCatching { repository.getPhotoCount(tokenId) }
            .onSuccess { pc ->
                // Foto disajikan backend (proxy dari Greenfield). URL: {base}items/{id}/photo/{i}
                val urls = (0 until pc.count).map { i ->
                    "${BuildConfig.API_BASE_URL}items/$tokenId/photo/$i"
                }
                _uiState.update { it.copy(photoUrls = urls) }
            }
    }
}
