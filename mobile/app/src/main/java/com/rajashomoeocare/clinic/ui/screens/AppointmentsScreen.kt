package com.rajashomoeocare.clinic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.domain.displayDate
import com.rajashomoeocare.clinic.ui.components.EmptyState
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.PatientCard
import com.rajashomoeocare.clinic.ui.components.PendingMessage
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.components.SendMessageSheet
import com.rajashomoeocare.clinic.ui.theme.recallColors
import com.rajashomoeocare.clinic.ui.vm.AppointmentsViewModel
import com.rajashomoeocare.clinic.ui.vm.ReminderRow

/**
 * The day-before reminder round (spec §4.8): who confirmed a date for tomorrow,
 * and one tap to send each of them the reminder in their own language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    viewModel: AppointmentsViewModel,
    onPatientClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recall = MaterialTheme.recallColors
    val reminderTitle = stringResource(R.string.message_appointment_reminder)
    var pending by remember { mutableStateOf<Pair<ReminderRow, PendingMessage>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appointments_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.error?.let { message ->
                item { ErrorBanner(message = message, onRetry = viewModel::refresh) }
            }

            item {
                SectionHeader(
                    text = stringResource(R.string.appointments_reminders_tomorrow),
                    trailing = state.tomorrow.size.toString(),
                )
            }

            if (state.tomorrow.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventAvailable,
                        title = stringResource(R.string.appointments_none_tomorrow),
                    )
                }
            }

            items(state.tomorrow, key = { it.appointment.id }) { row ->
                val alreadySent = row.appointment.id in state.sentIds
                PatientCard(
                    patient = row.patient,
                    onClick = { onPatientClick(row.patient.id) },
                    statusText = stringResource(
                        R.string.appointments_booked_for,
                        row.appointment.date.displayDate(),
                    ),
                    statusContainer = recall.dueToday,
                    statusContent = recall.onDueToday,
                    trailing = {
                        if (alreadySent) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = stringResource(
                                    R.string.appointments_reminder_sent
                                ),
                                tint = recall.onSettled,
                            )
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.message(
                                        row, TemplateKey.APPOINTMENT_REMINDER,
                                    )?.let { body ->
                                        pending = row to PendingMessage(
                                            patientName = row.patient.name,
                                            phone = row.patient.phone,
                                            language = row.patient.preferredLanguage,
                                            body = body,
                                            title = reminderTitle,
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.recall_send_short))
                            }
                        }
                    },
                )
            }
        }
    }

    pending?.let { (row, message) ->
        SendMessageSheet(
            message = message,
            onDismiss = { pending = null },
            onSent = {
                viewModel.markSent(row)
                pending = null
            },
        )
    }
}
