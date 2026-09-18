package com.rajashomoeocare.clinic.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import com.rajashomoeocare.clinic.domain.Investigation
import com.rajashomoeocare.clinic.domain.displayDate
import com.rajashomoeocare.clinic.domain.shortDate
import com.rajashomoeocare.clinic.ui.components.DateField
import com.rajashomoeocare.clinic.ui.components.EmptyState
import com.rajashomoeocare.clinic.ui.components.ErrorBanner
import com.rajashomoeocare.clinic.ui.components.LabeledField
import com.rajashomoeocare.clinic.ui.components.SectionCard
import com.rajashomoeocare.clinic.ui.vm.InvestigationSeries
import com.rajashomoeocare.clinic.ui.vm.InvestigationsViewModel
import com.rajashomoeocare.clinic.util.newCameraTarget
import com.rajashomoeocare.clinic.util.stageForUpload
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigationsScreen(
    viewModel: InvestigationsViewModel,
    onBack: () -> Unit,
    onCompare: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pages = remember { mutableListOf<Pair<File, String>>().toMutableStateList() }
    var showDatePicker by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<File?>(null) }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        cameraTarget?.let { file ->
            if (success) pages.add(file to "image/jpeg") else file.delete()
        }
        cameraTarget = null
    }

    val scope = rememberCoroutineScope()
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { stageForUpload(context, uri)?.let(pages::add) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.investigations_title)) },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    pages.clear()
                    viewModel.openForm()
                },
                icon = { Icon(Icons.Outlined.AddAPhoto, contentDescription = null) },
                text = { Text(stringResource(R.string.investigations_add)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { message ->
                item { ErrorBanner(message = message, onRetry = viewModel::refresh) }
            }

            if (state.series.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.investigations_empty),
                        subtitle = stringResource(R.string.investigations_empty_hint),
                    )
                }
            }

            items(state.series, key = { it.title }) { series ->
                SeriesCard(
                    series = series,
                    fileUrl = viewModel::fileUrl,
                    onAddToSeries = {
                        pages.clear()
                        viewModel.openForm(prefillTitle = series.title)
                    },
                    onCompare = { onCompare(series.title) },
                )
            }
        }
    }

    if (state.formOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeForm,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.investigations_new),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.investigations_title_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LabeledField(
                    value = state.title,
                    onValueChange = { v -> viewModel.edit { it.copy(title = v) } },
                    label = stringResource(R.string.investigations_report_title),
                )
                DateField(
                    date = state.takenOn,
                    onDateChange = { v -> viewModel.edit { it.copy(takenOn = v) } },
                    label = stringResource(R.string.investigations_taken_on),
                    showDialog = showDatePicker,
                    onShowDialogChange = { showDatePicker = it },
                )
                LabeledField(
                    value = state.note,
                    onValueChange = { v -> viewModel.edit { it.copy(note = v) } },
                    label = stringResource(R.string.investigations_note),
                    singleLine = false,
                    minLines = 2,
                    keyboardType = KeyboardType.Text,
                )

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
                            pickFile.launch(
                                PickVisualMediaRequest(
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

                if (pages.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pages, key = { it.first.absolutePath }) { page ->
                            Box {
                                Image(
                                    painter = rememberAsyncImagePainter(page.first),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                IconButton(
                                    onClick = {
                                        page.first.delete()
                                        pages.remove(page)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.save(pages.toList()) },
                    enabled = !state.uploading && pages.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.form_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(
    series: InvestigationSeries,
    fileUrl: (String) -> String,
    onAddToSeries: () -> Unit,
    onCompare: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.investigations_count, series.entries.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (series.canCompare) {
                TextButton(onClick = onCompare) {
                    Text(stringResource(R.string.visit_compare))
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(series.entries, key = { it.id }) { entry ->
                ReportThumb(entry = entry, fileUrl = fileUrl)
            }
        }

        TextButton(onClick = onAddToSeries) {
            Text(stringResource(R.string.investigations_add_followup))
        }
    }
}

@Composable
private fun ReportThumb(entry: Investigation, fileUrl: (String) -> String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val first = entry.files.firstOrNull()
            if (first != null) {
                Image(
                    painter = rememberAsyncImagePainter(fileUrl(first.id)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Outlined.Description, contentDescription = null)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry.takenOn.shortDate(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Earliest against latest by default — the comparison that actually matters
 * when a patient comes back three months later (spec §4.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigationCompareScreen(
    viewModel: InvestigationsViewModel,
    title: String,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val series = state.series.firstOrNull { it.title == title }
    val entries = series?.entries.orEmpty()

    var leftIndex by remember(entries.size) { mutableStateOf(0) }
    var rightIndex by remember(entries.size) {
        mutableStateOf((entries.size - 1).coerceAtLeast(0))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        if (entries.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.investigations_empty),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ComparePane(
                    entry = entries.getOrNull(leftIndex),
                    label = stringResource(R.string.photo_previous),
                    fileUrl = viewModel::fileUrl,
                    modifier = Modifier.weight(1f),
                )
                ComparePane(
                    entry = entries.getOrNull(rightIndex),
                    label = stringResource(R.string.photo_latest),
                    fileUrl = viewModel::fileUrl,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))
            DateStrip(
                entries = entries,
                selectedIndex = leftIndex,
                label = stringResource(R.string.photo_previous),
                fileUrl = viewModel::fileUrl,
                onSelect = { leftIndex = it },
            )
            Spacer(Modifier.height(16.dp))
            DateStrip(
                entries = entries,
                selectedIndex = rightIndex,
                label = stringResource(R.string.photo_latest),
                fileUrl = viewModel::fileUrl,
                onSelect = { rightIndex = it },
            )
        }
    }
}

@Composable
private fun ComparePane(
    entry: Investigation?,
    label: String,
    fileUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            entry?.files?.firstOrNull()?.let { file ->
                Image(
                    painter = rememberAsyncImagePainter(fileUrl(file.id)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        entry?.let {
            Text(
                text = it.takenOn.displayDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun DateStrip(
    entries: List<Investigation>,
    selectedIndex: Int,
    label: String,
    fileUrl: (String) -> String,
    onSelect: (Int) -> Unit,
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries.size) { index ->
                val selected = index == selectedIndex
                Card(
                    modifier = Modifier.clickable { onSelect(index) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                ) {
                    Text(
                        text = entries[index].takenOn.shortDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
