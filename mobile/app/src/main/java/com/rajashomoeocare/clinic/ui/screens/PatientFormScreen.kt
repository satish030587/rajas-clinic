package com.rajashomoeocare.clinic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.Sex
import com.rajashomoeocare.clinic.ui.components.ChoiceRow
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.LabeledField
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.vm.FormField
import com.rajashomoeocare.clinic.ui.vm.PatientFormViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    viewModel: PatientFormViewModel,
    canRecordVitals: Boolean,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onOpenExisting: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val requiredMessage = stringResource(R.string.form_required)
    val phoneMessage = stringResource(R.string.form_invalid_phone)
    val ageMessage = stringResource(R.string.form_age_or_dob)
    fun errorFor(field: FormField): String? = when (state.errors[field]) {
        PatientFormViewModel.ERROR_REQUIRED -> requiredMessage
        PatientFormViewModel.ERROR_PHONE -> phoneMessage
        PatientFormViewModel.ERROR_AGE -> ageMessage
        else -> null
    }

    // Duplicate phone means a duplicated patient, and a duplicated patient
    // means a missed recall (spec §4.11).
    state.duplicate?.let { existing ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicate,
            title = { Text(stringResource(R.string.patients_title)) },
            text = {
                Text(stringResource(R.string.form_duplicate_found, existing.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDuplicate()
                    onOpenExisting(existing.id)
                }) { Text(stringResource(R.string.form_duplicate_open)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicate) {
                    Text(stringResource(R.string.form_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.form_new_patient
                            else R.string.form_edit_patient
                        )
                    )
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

            SectionHeader(stringResource(R.string.form_section_identity))
            SectionCard {
                LabeledField(
                    value = state.name,
                    onValueChange = { v -> viewModel.edit { it.copy(name = v) } },
                    label = stringResource(R.string.form_name),
                    error = errorFor(FormField.NAME),
                )
                ChoiceRow(
                    options = listOf(Sex.MALE, Sex.FEMALE, Sex.OTHER),
                    selected = state.sex,
                    onSelect = { v -> viewModel.edit { it.copy(sex = v) } },
                    label = { sex ->
                        stringResource(
                            when (sex) {
                                Sex.MALE -> R.string.common_male
                                Sex.FEMALE -> R.string.common_female
                                Sex.OTHER -> R.string.common_other
                            }
                        )
                    },
                )
                LabeledField(
                    value = state.ageText,
                    onValueChange = { v ->
                        viewModel.edit { it.copy(ageText = v.filter(Char::isDigit).take(3)) }
                    },
                    label = stringResource(R.string.form_age),
                    keyboardType = KeyboardType.Number,
                    error = errorFor(FormField.AGE),
                )
            }

            SectionHeader(stringResource(R.string.form_section_contact))
            SectionCard {
                LabeledField(
                    value = state.phone,
                    onValueChange = { v ->
                        viewModel.edit { it.copy(phone = v) }
                        if (v.filter(Char::isDigit).length >= 10) viewModel.checkPhone()
                    },
                    label = stringResource(R.string.form_phone),
                    keyboardType = KeyboardType.Phone,
                    prefix = "+91 ",
                    error = errorFor(FormField.PHONE),
                )
                LabeledField(
                    value = state.alternatePhone,
                    onValueChange = { v -> viewModel.edit { it.copy(alternatePhone = v) } },
                    label = stringResource(R.string.form_alt_phone),
                    keyboardType = KeyboardType.Phone,
                    prefix = "+91 ",
                    error = errorFor(FormField.ALT_PHONE),
                )
                LabeledField(
                    value = state.address,
                    onValueChange = { v -> viewModel.edit { it.copy(address = v) } },
                    label = stringResource(R.string.form_address),
                    singleLine = false,
                    minLines = 2,
                )
                Text(
                    text = stringResource(R.string.form_language),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceRow(
                    options = listOf(Language.EN, Language.TA),
                    selected = state.language,
                    onSelect = { v -> viewModel.edit { it.copy(language = v) } },
                    label = { lang ->
                        stringResource(
                            if (lang == Language.EN) R.string.common_english
                            else R.string.common_tamil
                        )
                    },
                )
            }

            // Vitals are taken at the desk so the doctor doesn't have to (spec §4.2).
            if (canRecordVitals && state.isNew) {
                SectionHeader(stringResource(R.string.vitals_title))
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabeledField(
                            value = state.heightText,
                            onValueChange = { v ->
                                viewModel.edit { it.copy(heightText = v.numeric()) }
                            },
                            label = stringResource(R.string.vitals_height),
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                        LabeledField(
                            value = state.weightText,
                            onValueChange = { v ->
                                viewModel.edit { it.copy(weightText = v.numeric()) }
                            },
                            label = stringResource(R.string.vitals_weight),
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabeledField(
                            value = state.systolicText,
                            onValueChange = { v ->
                                viewModel.edit {
                                    it.copy(systolicText = v.filter(Char::isDigit).take(3))
                                }
                            },
                            label = stringResource(R.string.vitals_systolic),
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                        LabeledField(
                            value = state.diastolicText,
                            onValueChange = { v ->
                                viewModel.edit {
                                    it.copy(diastolicText = v.filter(Char::isDigit).take(3))
                                }
                            },
                            label = stringResource(R.string.vitals_diastolic),
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LabeledField(
                        value = state.pulseText,
                        onValueChange = { v ->
                            viewModel.edit { it.copy(pulseText = v.filter(Char::isDigit).take(3)) }
                        },
                        label = stringResource(R.string.vitals_pulse),
                        keyboardType = KeyboardType.Number,
                    )
                }
            }

            SectionHeader(stringResource(R.string.form_section_clinical))
            SectionCard {
                LabeledField(
                    value = state.occupation,
                    onValueChange = { v -> viewModel.edit { it.copy(occupation = v) } },
                    label = stringResource(R.string.form_occupation),
                )
                LabeledField(
                    value = state.bloodGroup,
                    onValueChange = { v -> viewModel.edit { it.copy(bloodGroup = v) } },
                    label = stringResource(R.string.form_blood_group),
                )
                LabeledField(
                    value = state.referredBy,
                    onValueChange = { v -> viewModel.edit { it.copy(referredBy = v) } },
                    label = stringResource(R.string.form_referred_by),
                )
                LabeledField(
                    value = state.currentMedication,
                    onValueChange = { v -> viewModel.edit { it.copy(currentMedication = v) } },
                    label = stringResource(R.string.form_current_medication),
                    singleLine = false,
                    minLines = 2,
                )
            }

            if (state.isNew) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.form_queue_after_save),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = state.queueAfterSave,
                        onCheckedChange = { v ->
                            viewModel.edit { it.copy(queueAfterSave = v) }
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { scope.launch { viewModel.save()?.let(onSaved) } },
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(
                        if (state.isNew && state.queueAfterSave) {
                            R.string.form_save_and_queue
                        } else {
                            R.string.form_save
                        }
                    )
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun String.numeric(): String =
    filter { it.isDigit() || it == '.' }.take(5)
