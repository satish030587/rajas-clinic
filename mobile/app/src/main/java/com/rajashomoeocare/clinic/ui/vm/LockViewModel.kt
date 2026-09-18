package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LockStage { LOADING, SETUP, CONFIRM, ENTER, UNLOCKED }

enum class LockError { WRONG_PIN, MISMATCH }

data class LockState(
    val stage: LockStage = LockStage.LOADING,
    val entry: String = "",
    val error: LockError? = null,
    val biometricEnabled: Boolean = false,
)

class LockViewModel(private val settings: SettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(LockState())
    val state: StateFlow<LockState> = _state.asStateFlow()

    private var firstEntry: String? = null

    init {
        viewModelScope.launch {
            val hasPin = settings.hasPin.first()
            _state.value = LockState(
                stage = if (hasPin) LockStage.ENTER else LockStage.SETUP,
                biometricEnabled = hasPin && settings.biometricEnabled.first(),
            )
        }
    }

    fun press(digit: Char) {
        val entry = _state.value.entry
        if (entry.length >= PIN_LENGTH) return
        val next = entry + digit
        _state.update { it.copy(entry = next, error = null) }
        if (next.length == PIN_LENGTH) submit(next)
    }

    fun backspace() {
        _state.update { it.copy(entry = it.entry.dropLast(1), error = null) }
    }

    fun unlockViaBiometric() {
        _state.update { it.copy(stage = LockStage.UNLOCKED) }
    }

    private fun submit(entry: String) = viewModelScope.launch {
        when (_state.value.stage) {
            LockStage.SETUP -> {
                firstEntry = entry
                _state.update { it.copy(stage = LockStage.CONFIRM, entry = "") }
            }

            LockStage.CONFIRM -> {
                if (entry == firstEntry) {
                    settings.setPin(entry)
                    _state.update { it.copy(stage = LockStage.UNLOCKED, entry = "") }
                } else {
                    firstEntry = null
                    _state.update {
                        it.copy(stage = LockStage.SETUP, entry = "", error = LockError.MISMATCH)
                    }
                }
            }

            LockStage.ENTER -> {
                if (settings.checkPin(entry)) {
                    _state.update { it.copy(stage = LockStage.UNLOCKED, entry = "") }
                } else {
                    _state.update { it.copy(entry = "", error = LockError.WRONG_PIN) }
                }
            }

            else -> Unit
        }
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}
