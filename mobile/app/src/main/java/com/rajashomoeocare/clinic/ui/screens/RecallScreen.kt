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
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.domain.RecallBucket
import com.rajashomoeocare.clinic.domain.RecallItem
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.ui.components.EmptyState
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.PatientCard
import com.rajashomoeocare.clinic.ui.components.PendingMessage
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.components.SendMessageSheet
import com.rajashomoeocare.clinic.ui.theme.recallColors
import com.rajashomoeocare.clinic.ui.vm.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecallScreen(
    viewModel: HomeViewModel,
    onPatientClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recall = MaterialTheme.recallColors
    val scope = rememberCoroutineScope()
    val recallTitle = stringResource(R.string.message_recall)
    var pending by remember { mutableStateOf<Pair<RecallItem, PendingMessage>?>(null) }

    // Most overdue first — those are the ones at risk of dropping out of treatment.
    val buckets = listOf(
        RecallBucket.OVER_A_MONTH to R.string.recall_overdue_long,
        RecallBucket.THIS_MONTH to R.string.recall_overdue_month,
        RecallBucket.THIS_WEEK to R.string.recall_overdue_week,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recall_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
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

            if (state.overdueCount == 0 && state.error == null) {
                item {
                    EmptyState(
                        icon = Icons.Filled.NotificationsActive,
                        title = stringResource(R.string.recall_empty),
                    )
                }
            }

            buckets.forEach { (bucket, labelRes) ->
                val entries = state.overdueBy(bucket)
                if (entries.isEmpty()) return@forEach

                item(key = "header-$bucket") {
                    SectionHeader(
                        text = stringResource(labelRes),
                        trailing = entries.size.toString(),
                    )
                }
                items(entries, key = { it.patient.id }) { item ->
                    val severe = bucket == RecallBucket.OVER_A_MONTH
                    PatientCard(
                        patient = item.patient,
                        onClick = { onPatientClick(item.patient.id) },
                        statusText = stringResource(
                            R.string.recall_days_overdue, item.daysOverdue,
                        ),
                        statusContainer = if (severe) recall.overdueLong else recall.overdueSoon,
                        statusContent = if (severe) recall.onOverdueLong else recall.onOverdueSoon,
                        trailing = {
                            FilledTonalButton(
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
                        },
                    )
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
