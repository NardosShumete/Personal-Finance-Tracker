package com.portfolio.financetracker.core.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies an image from a content:// URI (gallery / camera) into the app's
 * private files directory so it persists permanently.
 *
 * Why this is needed:
 * - A content:// URI is a temporary permission grant from the gallery.
 * - After the app restarts (or the gallery revokes the grant) the URI
 *   becomes unreadable — the image appears "lost".
 * - Copying to internal storage gives us a stable absolute file path
 *   that survives restarts, app updates, and gallery changes.
 *
 * @return The absolute path of the saved file, or null if copying failed.
 */
object FileHelper {

    suspend fun copyImageToAppStorage(
        context: Context,
        sourceUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Resolve the MIME type to pick the right extension
            val mimeType  = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png",  ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }

            // Receipts folder inside app-private files — never needs storage permission
            val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
            val destFile    = File(receiptsDir, "receipt_${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Return the absolute path only if the file was actually written
            if (destFile.exists() && destFile.length() > 0) destFile.absolutePath else null

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a previously saved receipt file when a transaction is deleted.
     * Safe to call with null or a non-existent path.
     */
    fun deleteReceiptFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
