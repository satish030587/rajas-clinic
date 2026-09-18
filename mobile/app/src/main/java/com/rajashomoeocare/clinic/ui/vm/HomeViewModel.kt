package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.local.PatientRow
import com.rajashomoeocare.clinic.data.local.TemplateKey
import com.rajashomoeocare.clinic.domain.RecallBucket
import com.rajashomoeocare.clinic.domain.RecallItem
import com.rajashomoeocare.clinic.domain.renderTemplate
import com.rajashomoeocare.clinic.domain.toRecallItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeState(
    val dueToday: List<RecallItem> = emptyList(),
    val overdue: Map<RecallBucket, List<RecallItem>> = emptyMap(),
    val upcoming: List<RecallItem> = emptyList(),
    val seenToday: List<PatientRow> = emptyList(),
    val collection: Int = 0,
    val outstanding: Int = 0,
) {
    val overdueCount: Int get() = overdue.values.sumOf { it.size }
}

/** Backs both the Today screen and the Recall screen — they read the same due list. */
class HomeViewModel(private val repo: ClinicRepository) : ViewModel() {

    private val today = LocalDate.now()

    val state: StateFlow<HomeState> = combine(
        repo.patients.observeDueOnOrBefore(today).map { it.toRecallItems(today) },
        repo.patients.observeUpcoming(today, today.plusDays(UPCOMING_WINDOW_DAYS))
            .map { it.toRecallItems(today) },
        repo.visits.observeSeenOn(today),
        repo.collectionOn(today),
        repo.outstandingOn(today),
    ) { due, upcoming, seen, collection, outstanding ->
        val (dueToday, overdue) = due.partition { it.bucket == RecallBucket.DUE_TODAY }
        HomeState(
            dueToday = dueToday,
            overdue = overdue.groupBy(RecallItem::bucket),
            upcoming = upcoming,
            seenToday = seen,
            collection = collection,
            outstanding = outstanding,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** Builds the recall text in the patient's own language (spec §7). */
    suspend fun recallMessage(item: RecallItem): String? {
        val template = repo.template(TemplateKey.RECALL, item.patient.preferredLanguage)
            ?: return null
        return renderTemplate(
            body = template.bodyText,
            patientName = item.patient.name,
            dueDate = item.dueDate,
            lastVisit = item.patient.lastVisitDate,
        )
    }

    suspend fun markRecallSent(item: RecallItem) {
        repo.recordMessageSent(
            patientId = item.patient.id,
            key = TemplateKey.RECALL,
            language = item.patient.preferredLanguage,
        )
    }

    private companion object {
        const val UPCOMING_WINDOW_DAYS = 7L
    }
}
