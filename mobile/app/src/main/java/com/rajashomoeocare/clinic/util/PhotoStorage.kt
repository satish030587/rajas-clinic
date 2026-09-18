package com.rajashomoeocare.clinic.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val PHOTO_DIR = "visit_photos"

private fun photoDir(context: Context): File =
    File(context.filesDir, PHOTO_DIR).apply { mkdirs() }

/** A file plus its shareable Uri, for handing to the camera app. */
fun newCameraTarget(context: Context): Pair<File, Uri> {
    val file = File(photoDir(context), "${UUID.randomUUID()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    return file to uri
}

/** Copies a gallery pick into app storage so the record survives the source being deleted. */
suspend fun importPhoto(context: Context, source: Uri): File? = withContext(Dispatchers.IO) {
    val target = File(photoDir(context), "${UUID.randomUUID()}.jpg")
    runCatching {
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use(input::copyTo)
        } ?: return@runCatching null
        target
    }.getOrNull()
}

fun deletePhotoFile(path: String) {
    runCatching { File(path).delete() }
}
