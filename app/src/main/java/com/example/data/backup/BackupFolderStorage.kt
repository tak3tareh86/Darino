package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException

/**
 * Shared SAF-backed destination for manual and scheduled backup files.
 * The selected tree URI is persisted by the user; this class never falls back to app-private storage.
 */
object BackupFolderStorage {
    private const val PREFS_NAME = "darino_backup_prefs"
    private const val KEY_FOLDER_URI = "backup_folder_tree_uri"

    data class BackupDocument(
        val uri: Uri,
        val name: String,
        val lastModified: Long
    )

    fun selectedFolderUri(context: Context): Uri? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER_URI, null)
            ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun saveSelectedFolder(context: Context, uri: Uri, grantedFlags: Int) {
        val accessFlags = grantedFlags and
            (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        require(accessFlags and android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) {
            "مجوز نوشتن در پوشه انتخاب‌شده دریافت نشد."
        }
        context.contentResolver.takePersistableUriPermission(uri, accessFlags)
        // Validate access before publishing the new destination in preferences.
        listDocuments(context, uri)
        check(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_FOLDER_URI, uri.toString()).commit()
        ) { "ذخیره پوشه پشتیبان ناموفق بود." }
    }

    fun folderLabel(uri: Uri): String =
        uri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
            ?: "پوشه انتخاب‌شده"

    fun saveBackup(
        context: Context,
        filename: String,
        payload: ByteArray,
        retentionPrefix: String? = null,
        keepCount: Int? = null
    ): Uri {
        require(filename.endsWith(".darino", ignoreCase = true)) {
            "پسوند فایل پشتیبان باید .darino باشد."
        }
        val treeUri = selectedFolderUri(context)
            ?: throw IOException("ابتدا پوشه ذخیره پشتیبان را در تنظیمات انتخاب کنید.")
        val hasWriteGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission
        }
        if (!hasWriteGrant) {
            throw IOException("دسترسی نوشتن به پوشه پشتیبان لغو شده است؛ پوشه را دوباره انتخاب کنید.")
        }

        val resolver = context.contentResolver
        val temporaryName = ".$filename.tmp-${System.currentTimeMillis()}"
        val temporaryUri = DocumentsContract.createDocument(
            resolver, treeUri, "application/octet-stream", temporaryName
        ) ?: throw IOException("ساخت فایل موقت در پوشه پشتیبان ناموفق بود.")
        try {
            val descriptor = resolver.openFileDescriptor(temporaryUri, "w")
                ?: throw IOException("باز کردن فایل موقت برای نوشتن ناموفق بود.")
            descriptor.use { parcel ->
                java.io.FileOutputStream(parcel.fileDescriptor).use { output ->
                    output.write(payload)
                    output.flush()
                    parcel.fileDescriptor.sync()
                }
            }
            val savedBytes = resolver.openInputStream(temporaryUri)?.use { input ->
                input.readBytes()
            } ?: throw IOException("بررسی فایل پشتیبان ذخیره‌شده ناموفق بود.")
            check(savedBytes.contentEquals(payload)) {
                "بررسی صحت فایل پشتیبان پس از ذخیره ناموفق بود."
            }
            val finalUri = DocumentsContract.renameDocument(resolver, temporaryUri, filename)
                ?: throw IOException("پوشه انتخاب‌شده امکان نهایی‌کردن نام فایل پشتیبان را ندارد.")
            if (retentionPrefix != null && keepCount != null) {
                pruneBackups(context, treeUri, retentionPrefix, keepCount)
            }
            return finalUri
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, temporaryUri) }
            throw error
        }
    }

    fun latestBackup(
        context: Context,
        namePrefixes: List<String>
    ): BackupDocument? {
        val treeUri = selectedFolderUri(context) ?: return null
        val hasReadGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!hasReadGrant) throw IOException("دسترسی خواندن پوشه پشتیبان لغو شده است؛ پوشه را دوباره انتخاب کنید.")
        return listDocuments(context, treeUri)
            .filter { document ->
                document.name.endsWith(".darino", ignoreCase = true) &&
                    namePrefixes.any(document.name::startsWith)
            }
            .maxByOrNull { it.lastModified }
    }

    private fun pruneBackups(
        context: Context,
        treeUri: Uri,
        prefix: String,
        keepCount: Int
    ) {
        val expired = listDocuments(context, treeUri)
            .filter { it.name.startsWith(prefix) && it.name.endsWith(".darino", ignoreCase = true) }
            .sortedByDescending { it.lastModified }
            .drop(keepCount)
        expired.forEach { document ->
            if (!runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, document.uri)
                }.getOrDefault(false)
            ) {
                android.util.Log.w("BackupFolderStorage", "Could not remove expired backup: ${document.name}")
            }
        }
    }

    private fun listDocuments(context: Context, treeUri: Uri): List<BackupDocument> {
        val resolver = context.contentResolver
        val parentId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val cursor = resolver.query(childrenUri, projection, null, null, null)
            ?: throw IOException("خواندن فهرست فایل‌های پوشه پشتیبان ممکن نیست.")
        return cursor.use {
            val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedColumn = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val mimeColumn = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            buildList {
                while (it.moveToNext()) {
                    if (mimeColumn >= 0 && it.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    val name = it.getString(nameColumn) ?: continue
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, it.getString(idColumn))
                    add(
                        BackupDocument(
                            uri = documentUri,
                            name = name,
                            lastModified = if (modifiedColumn >= 0) it.getLong(modifiedColumn) else 0L
                        )
                    )
                }
            }
        }
    }
}
