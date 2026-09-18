package com.rajashomoeocare.clinic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.domain.Card
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.MedicineForm
import com.rajashomoeocare.clinic.domain.PaymentMode
import com.rajashomoeocare.clinic.domain.Vitals
import com.rajashomoeocare.clinic.ui.components.ChoiceRow
import com.rajashomoeocare.clinic.ui.components.DateField
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.LabeledField
import com.rajashomoeocare.clinic.ui.components.PendingMessage
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.components.SendMessageSheet
import com.rajashomoeocare.clinic.ui.theme.recallColors
import com.rajashomoeocare.clinic.ui.vm.MedicineDraft
import com.rajashomoeocare.clinic.ui.vm.VisitEditorViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Quick offsets for the next-visit date — the field the recall system runs on. */
private val QUICK_INTERVALS = listOf(7L, 15L, 30L, 45L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitEditorScreen(
    viewModel: VisitEditorViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDuePicker by remember { mutableStateOf(false) }
    var cardMessage by remember { mutableStateOf<PendingMessage?>(null) }
    val cardTitle = stringResource(R.string.card_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.visit_title))
                        state.patient?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.error?.let { ErrorBanner(message = it) }

            // Already filled in at the front desk — the doctor only reads it.
            state.vitals?.takeIf { !it.isEmpty }?.let { VitalsSummary(it) }

            SectionCard {
                LabeledField(
                    value = state.complaint,
                    onValueChange = { v -> viewModel.edit { it.copy(complaint = v) } },
                    label = stringResource(R.string.visit_complaint),
                    singleLine = false,
                    minLines = 3,
                )
            }

            SectionHeader(
                text = stringResource(R.string.medicines_title),
                trailing = state.medicines.count { !it.isBlank }.toString(),
            )
            SectionCard {
                state.medicines.forEachIndexed { index, draft ->
                    MedicineRow(
                        draft = draft,
                        canRemove = state.medicines.size > 1,
                        onChange = { viewModel.updateMedicine(index, it) },
                        onRemove = { viewModel.removeMedicine(index) },
                    )
                }
                TextButton(onClick = viewModel::addMedicine) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.medicine_add))
                }
            }

            SectionHeader(stringResource(R.string.card_title))
            SectionCard {
                Text(
                    text = stringResource(R.string.card_none_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.cards.forEach { card ->
                    CardOption(
                        card = card,
                        language = state.patient?.preferredLanguage ?: Language.EN,
                        selected = card.id == state.selectedCardId,
                        onSelect = { viewModel.edit { it.copy(selectedCardId = card.id) } },
                    )
                }
                val patient = state.patient
                val chosen = state.selectedCard
                if (patient != null && chosen != null) {
                    OutlinedButton(
                        onClick = {
                            cardMessage = PendingMessage(
                                patientName = patient.name,
                                phone = patient.phone,
                                language = patient.preferredLanguage,
                                body = chosen.body(patient.preferredLanguage),
                                title = cardTitle,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.card_send))
                    }
                }
            }

            SectionHeader(stringResource(R.string.visit_next_due))
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QUICK_INTERVALS.forEach { days ->
                        val target = LocalDate.now().plusDays(days)
                        FilterChip(
                            selected = state.nextVisitDue == target,
                            onClick = { viewModel.edit { it.copy(nextVisitDue = target) } },
                            label = { Text("$days d") },
                        )
                    }
                }
                DateField(
                    date = state.nextVisitDue,
                    onDateChange = { v -> viewModel.edit { it.copy(nextVisitDue = v) } },
                    label = stringResource(R.string.visit_next_due),
                    supporting = stringResource(R.string.visit_next_due_hint),
                    showDialog = showDuePicker,
                    onShowDialogChange = { showDuePicker = it },
                )
            }

            SectionHeader(stringResource(R.string.visit_billing))
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(
                        value = state.consultationFee,
                        onValueChange = { v ->
                            viewModel.edit { it.copy(consultationFee = v.filter(Char::isDigit)) }
                        },
                        label = stringResource(R.string.visit_consultation_fee),
                        keyboardType = KeyboardType.Number,
                        prefix = "₹",
                        modifier = Modifier.weight(1f),
                    )
                    LabeledField(
                        value = state.medicineCharge,
                        onValueChange = { v ->
                            viewModel.edit { it.copy(medicineCharge = v.filter(Char::isDigit)) }
                        },
                        label = stringResource(R.string.visit_medicine_charge),
                        keyboardType = KeyboardType.Number,
                        prefix = "₹",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.visit_total),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "₹${state.total}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ChoiceRow(
                    options = listOf(PaymentMode.CASH, PaymentMode.UPI),
                    selected = state.paymentMode,
                    onSelect = { v -> viewModel.edit { it.copy(paymentMode = v) } },
                    label = { mode ->
                        stringResource(
                            if (mode == PaymentMode.CASH) R.string.common_cash
                            else R.string.common_upi
                        )
                    },
                )
                FilterChip(
                    selected = state.paid,
                    onClick = { viewModel.edit { it.copy(paid = !it.paid) } },
                    label = {
                        Text(
                            stringResource(
                                if (state.paid) R.string.visit_paid else R.string.visit_unpaid
                            )
                        )
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch { if (viewModel.save(complete = true)) onCompleted() }
                },
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.common_complete_visit))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { scope.launch { viewModel.save(complete = false) } },
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.common_save_draft))
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    cardMessage?.let { message ->
        SendMessageSheet(
            message = message,
            onDismiss = { cardMessage = null },
            onSent = { cardMessage = null },
        )
    }
}

@Composable
private fun VitalsSummary(vitals: Vitals) {
    val recall = MaterialTheme.recallColors
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = recall.onDueToday,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.vitals_recorded_at_desk),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = listOfNotNull(
                        vitals.bloodPressure?.let { "BP $it" },
                        vitals.weightKg?.let { "$it kg" },
                        vitals.heightCm?.let { "$it cm" },
                        vitals.pulse?.let { "Pulse $it" },
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MedicineRow(
    draft: MedicineDraft,
    canRemove: Boolean,
    onChange: (MedicineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabeledField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = stringResource(R.string.medicine_name),
                modifier = Modifier.weight(2.2f),
            )
            LabeledField(
                value = draft.potency,
                onValueChange = { onChange(draft.copy(potency = it)) },
                label = stringResource(R.string.medicine_potency),
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.medicine_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        ChoiceRow(
            options = listOf(MedicineForm.PILLS, MedicineForm.DROPS),
            selected = draft.form,
            onSelect = { onChange(draft.copy(form = it)) },
            label = { form ->
                stringResource(
                    if (form == MedicineForm.PILLS) R.string.medicine_pills
                    else R.string.medicine_drops
                )
            },
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CardOption(
    card: Card,
    language: Language,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                }
            )
            .clickable(onClick = onSelect)
            .padding(14.dp),
    ) {
        Text(
            text = card.label(language),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = card.body(language),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (selected) Int.MAX_VALUE else 3,
        )
    }
}
