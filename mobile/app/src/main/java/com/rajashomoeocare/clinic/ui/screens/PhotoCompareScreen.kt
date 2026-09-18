package com.rajashomoeocare.clinic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.local.PhotoEntity
import com.rajashomoeocare.clinic.ui.components.EmptyState
import com.rajashomoeocare.clinic.ui.vm.PatientDetailViewModel
import java.io.File
import java.time.ZoneId

/**
 * Spec §3: previous photos shown alongside the new one for comparison.
 * Defaults to oldest vs newest, which is the comparison that actually matters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCompareScreen(
    viewModel: PatientDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = state.photos

    var leftIndex by remember(photos.size) { mutableStateOf(0) }
    var rightIndex by remember(photos.size) { mutableStateOf((photos.size - 1).coerceAtLeast(0)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_compare_title)) },
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
        if (photos.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.PhotoLibrary,
                title = stringResource(R.string.photo_none),
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
                    photo = photos.getOrNull(leftIndex),
                    label = stringResource(R.string.photo_previous),
                    modifier = Modifier.weight(1f),
                )
                ComparePane(
                    photo = photos.getOrNull(rightIndex),
                    label = stringResource(R.string.photo_latest),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))

            PhotoStrip(
                photos = photos,
                selectedIndex = leftIndex,
                label = stringResource(R.string.photo_previous),
                onSelect = { leftIndex = it },
            )
            Spacer(Modifier.height(16.dp))
            PhotoStrip(
                photos = photos,
                selectedIndex = rightIndex,
                label = stringResource(R.string.photo_latest),
                onSelect = { rightIndex = it },
            )
        }
    }
}

@Composable
private fun ComparePane(photo: PhotoEntity?, label: String, modifier: Modifier = Modifier) {
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
            if (photo != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(photo.filePath)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (photo != null) {
            Text(
                text = photo.takenAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun PhotoStrip(
    photos: List<PhotoEntity>,
    selectedIndex: Int,
    label: String,
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
            items(photos.size) { index ->
                val selected = index == selectedIndex
                Image(
                    painter = rememberAsyncImagePainter(File(photos[index].filePath)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selected) 2.5.dp else 0.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelect(index) },
                )
            }
        }
    }
}
