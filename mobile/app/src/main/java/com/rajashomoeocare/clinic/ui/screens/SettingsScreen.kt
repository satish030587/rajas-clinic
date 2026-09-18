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
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.local.Language
import com.rajashomoeocare.clinic.data.local.TemplateKey
import com.rajashomoeocare.clinic.domain.TEMPLATE_PLACEHOLDERS
import com.rajashomoeocare.clinic.ui.components.BrandLockup
import com.rajashomoeocare.clinic.ui.components.LabeledField
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.vm.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var languageTab by remember { mutableStateOf(0) }
    val language = if (languageTab == 0) Language.EN else Language.TA

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            SectionHeader(stringResource(R.string.settings_templates))
            Text(
                text = stringResource(R.string.settings_templates_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )

            TabRow(
                selectedTabIndex = languageTab,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Tab(
                    selected = languageTab == 0,
                    onClick = { languageTab = 0 },
                    text = { Text(stringResource(R.string.settings_template_english)) },
                )
                Tab(
                    selected = languageTab == 1,
                    onClick = { languageTab = 1 },
                    text = { Text(stringResource(R.string.settings_template_tamil)) },
                )
            }

            Spacer(Modifier.height(12.dp))

            TemplateEditor(
                title = stringResource(R.string.message_welcome),
                initial = state.template(TemplateKey.WELCOME, language)?.bodyText.orEmpty(),
                onSave = { viewModel.saveTemplate(TemplateKey.WELCOME, language, it) },
                key = "welcome-$language",
            )

            Spacer(Modifier.height(12.dp))

            TemplateEditor(
                title = stringResource(R.string.message_recall),
                initial = state.template(TemplateKey.RECALL, language)?.bodyText.orEmpty(),
                onSave = { viewModel.saveTemplate(TemplateKey.RECALL, language, it) },
                key = "recall-$language",
            )

            SectionHeader(stringResource(R.string.settings_fee))
            SectionCard {
                var fee by remember(state.defaultFee) {
                    mutableStateOf(state.defaultFee.toString())
                }
                LabeledField(
                    value = fee,
                    onValueChange = {
                        fee = it.filter(Char::isDigit)
                        fee.toIntOrNull()?.let(viewModel::setDefaultFee)
                    },
                    label = stringResource(R.string.settings_fee),
                    keyboardType = KeyboardType.Number,
                    prefix = "₹",
                )
            }

            SectionHeader(stringResource(R.string.settings_security))
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_biometric),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.biometricEnabled,
                        onCheckedChange = viewModel::setBiometricEnabled,
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_about))
            SectionCard {
                BrandLockup(markSize = 52)
                Text(
                    text = stringResource(R.string.settings_version, "1.0"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.patients_count, state.patientCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TemplateEditor(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    key: String,
) {
    var body by remember(key, initial) { mutableStateOf(initial) }
    val dirty = body != initial && body.isNotBlank()

    SectionCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        LabeledField(
            value = body,
            onValueChange = { body = it },
            label = null,
            singleLine = false,
            minLines = 5,
        )
        Text(
            text = stringResource(
                R.string.settings_placeholders,
                TEMPLATE_PLACEHOLDERS.joinToString(" "),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { onSave(body) },
            enabled = dirty,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.form_save))
        }
    }
}
