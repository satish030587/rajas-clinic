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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.local.Language
import com.rajashomoeocare.clinic.data.local.Sex
import com.rajashomoeocare.clinic.ui.components.ChoiceRow
import com.rajashomoeocare.clinic.ui.components.DateField
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
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDobPicker by remember { mutableStateOf(false) }

    val requiredMessage = stringResource(R.string.form_required)
    val phoneMessage = stringResource(R.string.form_invalid_phone)
    val ageMessage = stringResource(R.string.form_age_or_dob)
    fun errorFor(field: FormField): String? = when (state.errors[field]) {
        PatientFormViewModel.ERROR_REQUIRED -> requiredMessage
        PatientFormViewModel.ERROR_PHONE -> phoneMessage
        PatientFormViewModel.ERROR_AGE -> ageMessage
        else -> null
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(
                        value = state.ageText,
                        onValueChange = { v ->
                            viewModel.edit {
                                it.copy(ageText = v.filter(Char::isDigit).take(3))
                            }
                        },
                        label = stringResource(R.string.form_age),
                        keyboardType = KeyboardType.Number,
                        error = errorFor(FormField.AGE),
                        enabled = state.dateOfBirth == null,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(0.dp))
                }
                DateField(
                    date = state.dateOfBirth,
                    onDateChange = { v ->
                        viewModel.edit { it.copy(dateOfBirth = v, ageText = "") }
                    },
                    label = stringResource(R.string.form_dob),
                    showDialog = showDobPicker,
                    onShowDialogChange = { showDobPicker = it },
                )
            }

            SectionHeader(stringResource(R.string.form_section_contact))
            SectionCard {
                LabeledField(
                    value = state.phone,
                    onValueChange = { v -> viewModel.edit { it.copy(phone = v) } },
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

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch { viewModel.save()?.let(onSaved) }
                },
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.form_save))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
