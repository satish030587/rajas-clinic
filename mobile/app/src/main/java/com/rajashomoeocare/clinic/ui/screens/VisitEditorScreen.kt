package com.rajashomoeocare.clinic.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.local.PaymentMode
import com.rajashomoeocare.clinic.ui.components.ChoiceRow
import com.rajashomoeocare.clinic.ui.components.DateField
import com.rajashomoeocare.clinic.ui.components.LabeledField
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.components.SectionHeader
import com.rajashomoeocare.clinic.ui.theme.recallColors
import com.rajashomoeocare.clinic.ui.vm.VisitEditorViewModel
import com.rajashomoeocare.clinic.util.importPhoto
import com.rajashomoeocare.clinic.util.newCameraTarget
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** Quick offsets for the next-visit date — the field the recall system runs on. */
private val QUICK_INTERVALS = listOf(7L, 15L, 30L, 45L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitEditorScreen(
    viewModel: VisitEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onCompare: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showVisitDatePicker by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<File?>(null) }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        cameraTarget?.let { file ->
            if (success) viewModel.addPhoto(file.absolutePath) else file.delete()
        }
        cameraTarget = null
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                importPhoto(context, uri)?.let { viewModel.addPhoto(it.absolutePath) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(
                                if (state.isNew) R.string.visit_title
                                else R.string.visit_edit_title
                            )
                        )
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
                    IconButton(onClick = {
                        viewModel.discardUnsaved()
                        onBack()
                    }) {
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
            SectionCard {
                DateField(
                    date = state.visitDate,
                    onDateChange = { v -> viewModel.edit { it.copy(visitDate = v) } },
                    label = stringResource(R.string.visit_date),
                    showDialog = showVisitDatePicker,
                    onShowDialogChange = { showVisitDatePicker = it },
                )
                LabeledField(
                    value = state.complaint,
                    onValueChange = { v -> viewModel.edit { it.copy(complaint = v) } },
                    label = stringResource(R.string.visit_complaint),
                    singleLine = false,
                    minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(
                        value = state.remedy,
                        onValueChange = { v -> viewModel.edit { it.copy(remedy = v) } },
                        label = stringResource(R.string.visit_remedy),
                        modifier = Modifier.weight(2f),
                    )
                    LabeledField(
                        value = state.potency,
                        onValueChange = { v -> viewModel.edit { it.copy(potency = v) } },
                        label = stringResource(R.string.visit_potency),
                        modifier = Modifier.weight(1f),
                    )
                }
                LabeledField(
                    value = state.advice,
                    onValueChange = { v -> viewModel.edit { it.copy(advice = v) } },
                    label = stringResource(R.string.visit_advice),
                    singleLine = false,
                    minLines = 2,
                )
            }

            SectionHeader(stringResource(R.string.visit_next_due))
            NextVisitCard(
                dueDate = state.nextVisitDue,
                visitDate = state.visitDate,
                onSelect = { v -> viewModel.edit { it.copy(nextVisitDue = v) } },
                showPicker = showDuePicker,
                onShowPickerChange = { showDuePicker = it },
            )

            SectionHeader(
                text = stringResource(R.string.visit_photos),
                trailing = (state.photos.size + state.pendingPhotos.size).toString(),
            )
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            val (file, uri) = newCameraTarget(context)
                            cameraTarget = file
                            takePhoto.launch(uri)
                        },
                        label = { Text(stringResource(R.string.photo_take)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    AssistChip(
                        onClick = {
                            pickPhoto.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        label = { Text(stringResource(R.string.photo_choose)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }

                val thumbs = state.photos.map { it.filePath } + state.pendingPhotos
                if (thumbs.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(thumbs, key = { it }) { path ->
                            PhotoThumb(
                                path = path,
                                onRemove = {
                                    val saved = state.photos.firstOrNull { it.filePath == path }
                                    if (saved != null) {
                                        viewModel.removeSavedPhoto(saved)
                                    } else {
                                        viewModel.removePendingPhoto(path)
                                    }
                                },
                            )
                        }
                    }
                }

                if (state.priorPhotos.isNotEmpty()) {
                    TextButton(onClick = onCompare) {
                        Icon(
                            Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.visit_compare))
                    }
                }
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
                onClick = { scope.launch { if (viewModel.save()) onSaved() } },
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.visit_save))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NextVisitCard(
    dueDate: LocalDate?,
    visitDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    showPicker: Boolean,
    onShowPickerChange: (Boolean) -> Unit,
) {
    val recall = MaterialTheme.recallColors
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_INTERVALS.forEach { days ->
                val target = visitDate.plusDays(days)
                FilterChip(
                    selected = dueDate == target,
                    onClick = { onSelect(target) },
                    label = { Text("$days d") },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = recall.dueToday,
                        selectedLabelColor = recall.onDueToday,
                    ),
                )
            }
        }
        DateField(
            date = dueDate,
            onDateChange = onSelect,
            label = stringResource(R.string.visit_next_due),
            supporting = stringResource(R.string.visit_next_due_hint),
            showDialog = showPicker,
            onShowDialogChange = onShowPickerChange,
        )
    }
}

@Composable
private fun PhotoThumb(path: String, onRemove: () -> Unit) {
    Box {
        Image(
            painter = rememberAsyncImagePainter(File(path)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp),
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.photo_delete),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

