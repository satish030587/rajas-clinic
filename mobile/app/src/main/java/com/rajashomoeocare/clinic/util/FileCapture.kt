package com.rajashomoeocare.clinic.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Staging area for photographed reports. Files live in the cache because the
 * server is the record — these are deleted as soon as the upload succeeds.
 */
private fun stagingDir(context: Context): File =
    File(context.cacheDir, "capture").apply { mkdirs() }

fun newCameraTarget(context: Context): Pair<File, Uri> {
    val file = File(stagingDir(context), "${UUID.randomUUID()}.jpg")
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file,
    )
    return file to uri
}

/** Copies a picked file into staging so the upload has a stable path. */
suspend fun stageForUpload(context: Context, source: Uri): Pair<File, String>? =
    withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(source) ?: "image/jpeg"
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> "jpg"
        }
        val target = File(stagingDir(context), "${UUID.randomUUID()}.$extension")
        runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use(input::copyTo)
            } ?: return@runCatching null
            target to mime
        }.getOrNull()
    }

fun clearStaged(file: File) {
    runCatching { file.delete() }
}
