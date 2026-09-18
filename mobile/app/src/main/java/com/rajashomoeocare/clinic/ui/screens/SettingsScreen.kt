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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.UserRole
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.TEMPLATE_PLACEHOLDERS
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.ui.components.BrandLockup
import com.rajashomoeocare.clinic.ui.components.ChoiceRow
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.LabeledField
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.vm.SettingsViewModel

private val TEMPLATE_LABELS = listOf(
    TemplateKey.WELCOME to R.string.message_welcome,
    TemplateKey.RECALL to R.string.message_recall,
    TemplateKey.APPOINTMENT_CONFIRMATION to R.string.message_appointment_confirmation,
    TemplateKey.APPOINTMENT_REMINDER to R.string.message_appointment_reminder,
    TemplateKey.MEDICINE_DISPATCHED to R.string.message_dispatched,
    TemplateKey.REVIEW_REQUEST to R.string.message_review,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    role: UserRole,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var languageTab by remember { mutableStateOf(0) }
    val templateLanguage = if (languageTab == 0) Language.EN else Language.TA

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
            state.error?.let { ErrorBanner(message = it, onRetry = viewModel::refresh) }

            // Spec §4.7 — the app's own language, independent of the patient's.
            SectionHeader(stringResource(R.string.settings_app_language))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_app_language_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ChoiceRow(
                    options = listOf(Language.EN, Language.TA),
                    selected = state.uiLanguage,
                    onSelect = viewModel::setUiLanguage,
                    label = { lang ->
                        stringResource(
                            if (lang == Language.EN) R.string.common_english
                            else R.string.common_tamil
                        )
                    },
                )
            }

            // Template editing is doctor-only (spec §6).
            if (role == UserRole.DOCTOR) {
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

                TEMPLATE_LABELS.forEach { (key, labelRes) ->
                    TemplateEditor(
                        title = stringResource(labelRes),
                        initial = state.templates[key to templateLanguage].orEmpty(),
                        editorKey = "$key-$templateLanguage",
                        onSave = { viewModel.saveTemplate(key, templateLanguage, it) },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                SectionHeader(stringResource(R.string.settings_cards))
                SectionCard {
                    state.cards.forEach { card ->
                        Text(
                            text = card.label(templateLanguage),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = card.body(templateLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_about))
            SectionCard {
                BrandLockup(markSize = 52)
                Text(
                    text = stringResource(
                        R.string.signed_in_as, state.displayName, state.role,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_version, "3.0"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { viewModel.signOut(onSignedOut) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.login_sign_out))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TemplateEditor(
    title: String,
    initial: String,
    editorKey: String,
    onSave: (String) -> Unit,
) {
    var body by remember(editorKey, initial) { mutableStateOf(initial) }
    val dirty = body != initial && body.isNotBlank()

    SectionCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        LabeledField(
            value = body,
            onValueChange = { body = it },
            label = null,
            singleLine = false,
            minLines = 4,
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
