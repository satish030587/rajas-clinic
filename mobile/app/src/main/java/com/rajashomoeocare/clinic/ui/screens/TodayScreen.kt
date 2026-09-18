package com.rajashomoeocare.clinic.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.UserRole
import com.rajashomoeocare.clinic.domain.RecallItem
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.domain.VisitStatus
import com.rajashomoeocare.clinic.domain.displayDate
import com.rajashomoeocare.clinic.ui.components.EmptyState
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.PatientCard
import com.rajashomoeocare.clinic.ui.components.PendingMessage
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.components.SendMessageSheet
import com.rajashomoeocare.clinic.ui.theme.recallColors
import com.rajashomoeocare.clinic.ui.vm.HomeViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: HomeViewModel,
    role: UserRole,
    onPatientClick: (String) -> Unit,
    onOpenVisit: (String) -> Unit,
    onAddPatient: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recall = MaterialTheme.recallColors
    val scope = rememberCoroutineScope()
    val recallTitle = stringResource(R.string.message_recall)
    var pending by remember { mutableStateOf<Pair<RecallItem, PendingMessage>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.today_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = LocalDate.now().displayDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                onClick = onAddPatient,
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text(stringResource(R.string.today_new_patient)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.error?.let { message ->
                item {
                    ErrorBanner(message = message, onRetry = viewModel::refresh)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.EventAvailable,
                        label = stringResource(R.string.today_due_today),
                        value = state.summary.dueToday.size.toString(),
                        container = recall.dueToday,
                        content = recall.onDueToday,
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CheckCircle,
                        label = stringResource(R.string.today_seen_today),
                        value = state.summary.seenToday.size.toString(),
                        container = recall.settled,
                        content = recall.onSettled,
                    )
                }
            }

            // Fees are doctor-only (spec §6), so reception never sees the total.
            if (role == UserRole.DOCTOR) {
                item {
                    CollectionTile(
                        collected = state.summary.collection,
                        outstanding = state.summary.outstanding,
                    )
                }
            }

            // The waiting room — what the doctor's day actually runs on.
            item {
                SectionHeader(
                    text = stringResource(R.string.queue_title),
                    trailing = state.queue.size.toString(),
                )
            }
            if (state.queue.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventAvailable,
                        title = stringResource(R.string.queue_empty),
                    )
                }
            } else {
                items(state.queue, key = { it.id }) { visit ->
                    val patient = state.queueNames[visit.patientId]
                    if (patient != null) {
                        PatientCard(
                            patient = patient,
                            onClick = { onPatientClick(patient.id) },
                            subtitle = buildString {
                                append(
                                    if (visit.status == VisitStatus.IN_CONSULTATION) {
                                        stringResourceOf(R.string.queue_in_consultation)
                                    } else {
                                        stringResourceOf(R.string.queue_waiting)
                                    }
                                )
                                visit.vitals?.bloodPressure?.let { append(" · BP $it") }
                                visit.vitals?.weightKg?.let { append(" · ${it}kg") }
                            },
                            trailing = {
                                if (role == UserRole.DOCTOR) {
                                    Button(
                                        onClick = { onOpenVisit(visit.id) },
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                    ) {
                                        Text(stringResource(R.string.queue_see_patient))
                                    }
                                }
                            },
                        )
                    }
                }
            }

            if (state.overdueCount > 0) {
                item { OverdueBanner(count = state.overdueCount) }
            }

            item {
                SectionHeader(
                    text = stringResource(R.string.today_due_today),
                    trailing = state.summary.dueToday.size.toString(),
                )
            }

            if (state.summary.dueToday.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventAvailable,
                        title = stringResource(R.string.today_empty),
                    )
                }
            } else {
                items(state.summary.dueToday, key = { it.patient.id }) { item ->
                    PatientCard(
                        patient = item.patient,
                        onClick = { onPatientClick(item.patient.id) },
                        statusText = stringResource(R.string.recall_due_today),
                        statusContainer = recall.dueToday,
                        statusContent = recall.onDueToday,
                        trailing = {
                            FilledTonalIconButton(
                                onClick = {
                                    viewModel.message(
                                        TemplateKey.RECALL, item.patient, item.dueDate,
                                    )?.let { body ->
                                        pending = item to PendingMessage(
                                            patientName = item.patient.name,
                                            phone = item.patient.phone,
                                            language = item.patient.preferredLanguage,
                                            body = body,
                                            title = recallTitle,
                                        )
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.recall_send),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    )
                }
            }

            if (state.summary.seenToday.isNotEmpty()) {
                item {
                    SectionHeader(
                        text = stringResource(R.string.today_seen_today),
                        trailing = state.summary.seenToday.size.toString(),
                    )
                }
                items(state.summary.seenToday, key = { "seen-${it.id}" }) { patient ->
                    PatientCard(patient = patient, onClick = { onPatientClick(patient.id) })
                }
            }
        }
    }

    pending?.let { (item, message) ->
        SendMessageSheet(
            message = message,
            onDismiss = { pending = null },
            onSent = {
                scope.launch {
                    viewModel.markSent(item, TemplateKey.RECALL)
                    pending = null
                }
            },
        )
    }
}

@Composable
private fun stringResourceOf(id: Int): String = stringResource(id)

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = content,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun CollectionTile(collected: Int, outstanding: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CurrencyRupee,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_collection),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "₹$collected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (outstanding > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.visit_unpaid),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "₹$outstanding",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverdueBanner(count: Int) {
    val recall = MaterialTheme.recallColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = recall.overdueSoon),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = recall.onOverdueSoon,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = pluralStringResource(R.plurals.overdue_banner, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = recall.onOverdueSoon,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
