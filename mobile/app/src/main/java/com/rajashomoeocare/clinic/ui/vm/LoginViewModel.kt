package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.SessionStore
import com.rajashomoeocare.clinic.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

data class LoginState(
    val username: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

class LoginViewModel(
    private val api: ApiService,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onUsername(value: String) = _state.update { it.copy(username = value, error = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "Enter your username and password") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val response = withContext(Dispatchers.IO) {
                    api.login(current.username.trim(), current.password)
                }
                session.save(
                    token = response.accessToken,
                    userId = response.user.id,
                    displayName = response.user.displayName,
                    role = response.user.role,
                    uiLanguage = response.user.uiLanguage,
                )
                _state.update { it.copy(busy = false, signedIn = true) }
            } catch (e: HttpException) {
                val message = if (e.code() == 401) {
                    "Incorrect username or password"
                } else {
                    "Sign-in failed (${e.code()})"
                }
                _state.update { it.copy(busy = false, error = message) }
            } catch (e: IOException) {
                _state.update {
                    it.copy(
                        busy = false,
                        error = "Cannot reach the clinic server. Check the network.",
                    )
                }
            }
        }
    }
}
