package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.domain.Investigation
import com.rajashomoeocare.clinic.domain.InvestigationKind
import com.rajashomoeocare.clinic.util.clearStaged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** One study followed over time — "Kidney USG" in March and again in June. */
data class InvestigationSeries(
    val title: String,
    val entries: List<Investigation>,
) {
    val earliest: Investigation? get() = entries.minByOrNull { it.takenOn }
    val latest: Investigation? get() = entries.maxByOrNull { it.takenOn }
    val canCompare: Boolean get() = entries.size > 1
}

data class InvestigationsState(
    val series: List<InvestigationSeries> = emptyList(),
    val loading: Boolean = true,
    val uploading: Boolean = false,
    val error: String? = null,

    // New-report form
    val formOpen: Boolean = false,
    val title: String = "",
    val kind: InvestigationKind = InvestigationKind.SCAN,
    val takenOn: LocalDate = LocalDate.now(),
    val note: String = "",
)

class InvestigationsViewModel(
    private val repo: ClinicRepository,
    private val patientId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(InvestigationsState())
    val state: StateFlow<InvestigationsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        repo.investigations(patientId)
            .onSuccess { list ->
                val grouped = list.groupBy { it.title }
                    .map { (title, entries) ->
                        InvestigationSeries(title, entries.sortedBy { it.takenOn })
                    }
                    .sortedBy { it.title }
                _state.update { it.copy(series = grouped, loading = false, error = null) }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message) }
            }
    }

    fun openForm(prefillTitle: String = "") = _state.update {
        it.copy(
            formOpen = true,
            title = prefillTitle,
            note = "",
            takenOn = LocalDate.now(),
            error = null,
        )
    }

    fun closeForm() = _state.update { it.copy(formOpen = false) }

    fun edit(transform: (InvestigationsState) -> InvestigationsState) =
        _state.update { transform(it).copy(error = null) }

    fun fileUrl(fileId: String) = repo.fileUrl(fileId)

    /**
     * Creates the report, then uploads the pages. The record is created first so
     * a failed page upload leaves a report to retry into rather than losing
     * everything the user just photographed.
     */
    fun save(pages: List<Pair<File, String>>) = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Give the report a title, such as Kidney USG") }
            return@launch
        }

        _state.update { it.copy(uploading = true, error = null) }

        val created = repo.createInvestigation(
            patientId = patientId,
            title = s.title,
            kind = s.kind,
            takenOn = s.takenOn,
            note = s.note,
        ).getOrElse { e ->
            _state.update { it.copy(uploading = false, error = e.message) }
            return@launch
        }

        for ((file, mime) in pages) {
            val result = repo.uploadInvestigationFile(created.id, file, mime)
            if (result.isFailure) {
                _state.update {
                    it.copy(uploading = false, error = result.exceptionOrNull()?.message)
                }
                return@launch
            }
            clearStaged(file)
        }

        _state.update { it.copy(uploading = false, formOpen = false) }
        refresh()
    }

    fun delete(investigation: Investigation) = viewModelScope.launch {
        repo.deleteInvestigation(investigation.id)
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }
}
