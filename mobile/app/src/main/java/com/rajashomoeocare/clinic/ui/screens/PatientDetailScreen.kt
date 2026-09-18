package com.rajashomoeocare.clinic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.local.TemplateKey
import com.rajashomoeocare.clinic.data.local.VisitWithBilling
import com.rajashomoeocare.clinic.domain.currentAge
import com.rajashomoeocare.clinic.domain.displayDate
import com.rajashomoeocare.clinic.ui.components.EmptyState
import com.rajashomoeocare.clinic.ui.components.PatientAvatar
import com.rajashomoeocare.clinic.ui.components.PendingMessage
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.components.SendMessageSheet
import com.rajashomoeocare.clinic.ui.components.StatusPill
import com.rajashomoeocare.clinic.ui.theme.recallColors
import com.rajashomoeocare.clinic.ui.vm.PatientDetailViewModel
import com.rajashomoeocare.clinic.util.dialNumber
import com.rajashomoeocare.clinic.util.formatPhone
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    viewModel: PatientDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNewVisit: () -> Unit,
    onEditVisit: (String) -> Unit,
    onCompare: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val recall = MaterialTheme.recallColors
    val welcomeTitle = stringResource(R.string.message_welcome)
    var pending by remember { mutableStateOf<Pair<TemplateKey, PendingMessage>?>(null) }

    val patient = state.patient

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.detail_edit),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewVisit,
                icon = { Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null) },
                text = { Text(stringResource(R.string.detail_new_visit)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (patient == null) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PatientAvatar(name = patient.name, size = 56)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = patient.name,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = buildString {
                                    currentAge(
                                        patient.dateOfBirth,
                                        patient.ageYears,
                                        patient.ageRecordedOn,
                                    )?.let { append("$it yrs · ") }
                                    append(
                                        patient.sex.name.lowercase()
                                            .replaceFirstChar(Char::uppercase)
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPhone(patient.phone),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    state.nextDue?.let { due ->
                        val overdue = due.isBefore(LocalDate.now())
                        StatusPill(
                            text = "${stringResource(R.string.detail_next_due)}: " +
                                due.displayDate(),
                            container = if (overdue) recall.overdueSoon else recall.dueToday,
                            content = if (overdue) recall.onOverdueSoon else recall.onDueToday,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { context.dialNumber(patient.phone) },
                            label = { Text(stringResource(R.string.detail_call)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                        AssistChip(
                            onClick = {
                                scope.launch {
                                    viewModel.message(TemplateKey.WELCOME)?.let { body ->
                                        pending = TemplateKey.WELCOME to PendingMessage(
                                            patientName = patient.name,
                                            phone = patient.phone,
                                            language = patient.preferredLanguage,
                                            body = body,
                                            title = welcomeTitle,
                                        )
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.message_welcome)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.WavingHand,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                        if (state.photos.size > 1) {
                            AssistChip(
                                onClick = onCompare,
                                label = { Text(stringResource(R.string.visit_compare)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.CompareArrows,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            val details = listOfNotNull(
                patient.address?.let { R.string.form_address to it },
                patient.occupation?.let { R.string.form_occupation to it },
                patient.bloodGroup?.let { R.string.form_blood_group to it },
                patient.referredBy?.let { R.string.form_referred_by to it },
                patient.currentMedication?.let { R.string.form_current_medication to it },
            )
            if (details.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.detail_profile)) }
                item {
                    SectionCard {
                        details.forEach { (labelRes, value) ->
                            DetailRow(label = stringResource(labelRes), value = value)
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    text = stringResource(R.string.detail_visits),
                    trailing = state.visits.size.toString(),
                )
            }

            if (state.visits.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.EventNote,
                        title = stringResource(R.string.detail_no_visits),
                    )
                }
            } else {
                items(state.visits, key = { it.id }) { visit ->
                    VisitCard(visit = visit, onClick = { onEditVisit(visit.id) })
                }
            }
        }
    }

    pending?.let { (key, message) ->
        SendMessageSheet(
            message = message,
            onDismiss = { pending = null },
            onSent = {
                scope.launch {
                    viewModel.markSent(key)
                    pending = null
                }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VisitCard(visit: VisitWithBilling, onClick: () -> Unit) {
    val total = (visit.consultationFee ?: 0) + (visit.medicineCharge ?: 0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = visit.visitDate.displayDate(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (total > 0) {
                    Text(
                        text = "₹$total",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (visit.paid == false) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            visit.complaint?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (visit.remedyGiven != null || visit.potency != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = listOfNotNull(visit.remedyGiven, visit.potency).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            visit.nextVisitDue?.let { due ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${stringResource(R.string.detail_next_due)}: ${due.displayDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (visit.photoCount > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.detail_photo_count, visit.photoCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

