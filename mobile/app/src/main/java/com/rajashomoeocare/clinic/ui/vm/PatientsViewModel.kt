package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.domain.PatientRow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PatientsState(
    val query: String = "",
    val patients: List<PatientRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class PatientsViewModel(private val repo: ClinicRepository) : ViewModel() {

    private val _state = MutableStateFlow(PatientsState())
    val state: StateFlow<PatientsState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        load("")
        viewModelScope.launch {
            queryFlow.drop(1).debounce(250).collect { load(it) }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value
    }

    fun refresh() = load(_state.value.query)

    private fun load(query: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        repo.patients(query)
            .onSuccess { rows ->
                _state.update { it.copy(patients = rows, loading = false, error = null) }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message) }
            }
    }
}
