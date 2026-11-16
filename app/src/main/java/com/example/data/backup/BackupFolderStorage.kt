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
        return runCatching { Uri.parse(raw) }
            .getOrNull()
            ?.takeIf(::isUsableTreeUri)
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

    fun hasWritableSelectedFolder(context: Context): Boolean {
        val treeUri = selectedFolderUri(context) ?: return false
        val permissions = context.contentResolver.persistedUriPermissions
        val hasReadGrant = permissions.any { it.uri == treeUri && it.isReadPermission }
        val hasWriteGrant = permissions.any { it.uri == treeUri && it.isWritePermission }
        if (!hasReadGrant || !hasWriteGrant) return false
        return runCatching {
            listDocuments(context, treeUri)
            true
        }.getOrDefault(false)
    }

    private fun isUsableTreeUri(uri: Uri): Boolean =
        uri.scheme == "content" &&
            !uri.authority.isNullOrBlank() &&
            DocumentsContract.isTreeUri(uri) &&
            runCatching { DocumentsContract.getTreeDocumentId(uri).isNotBlank() }.getOrDefault(false)

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
            ?: throw IOException("پوشه ذخیره پشتیبان معتبر نیست؛ لطفاً دوباره پوشه را انتخاب کنید.")
        val hasWriteGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission
        }
        if (!hasWriteGrant) {
            throw IOException("دسترسی نوشتن به پوشه پشتیبان لغو شده است؛ پوشه را دوباره انتخاب کنید.")
        }

        val resolver = context.contentResolver
        val temporaryName = ".$filename.tmp-${System.currentTimeMillis()}"
        // createDocument expects a document URI for the parent, not the raw tree URI.
        // Convert the selected tree to its root document URI while retaining the tree grant.
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val temporaryUri = DocumentsContract.createDocument(
            resolver, parentDocumentUri, "application/octet-stream", temporaryName
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
                // Retention is best-effort: a listing/provider failure must never turn a
                // verified and renamed backup into a failed save or delete the new file.
                runCatching {
                    pruneBackups(context, treeUri, retentionPrefix, keepCount)
                }.onFailure { error ->
                    android.util.Log.w(
                        "BackupFolderStorage",
                        "Backup was saved, but retention cleanup failed.",
                        error
                    )
                }
            }
            return finalUri
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, temporaryUri) }
            throw error
        }
    }

    /**
     * Replaces one automatic backup in place while retaining a recoverable copy of the
     * previous file until the new document has been verified after rename.
     */
    fun saveOrReplaceAutomaticBackup(
        context: Context,
        filename: String,
        payload: ByteArray,
        retentionPrefix: String
    ): Uri {
        require(filename.endsWith(".darino", ignoreCase = true)) {
            "پسوند فایل پشتیبان باید .darino باشد."
        }
        require(filename.startsWith(retentionPrefix)) {
            "نام فایل پشتیبان با محدوده نگهداری آن سازگار نیست."
        }
        val treeUri = selectedFolderUri(context)
            ?: throw IOException("پوشه ذخیره پشتیبان معتبر نیست؛ لطفاً دوباره پوشه را انتخاب کنید.")
        val resolver = context.contentResolver
        if (resolver.persistedUriPermissions.none { it.uri == treeUri && it.isWritePermission }) {
            throw IOException("دسترسی نوشتن به پوشه پشتیبان لغو شده است؛ پوشه را دوباره انتخاب کنید.")
        }

        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        // Recover a previous file if the process stopped between moving the old document
        // aside and promoting the verified temporary document.
        val existingDocuments = listDocuments(context, treeUri)
        val rollbackDocuments = existingDocuments
            .filter { it.name.startsWith(".$filename.rollback-") }
            .sortedByDescending { it.lastModified }
        val currentDocument = existingDocuments.firstOrNull { it.name == filename }
        if (currentDocument == null && rollbackDocuments.isNotEmpty()) {
            val recovered = DocumentsContract.renameDocument(resolver, rollbackDocuments.first().uri, filename)
                ?: throw IOException("بازیابی نسخه قبلی پشتیبان پس از توقف عملیات ناموفق بود.")
            if (resolver.openInputStream(recovered)?.use { it.readBytes().isNotEmpty() } != true) {
                throw IOException("نسخه قبلی پشتیبان پس از بازیابی قابل خواندن نیست.")
            }
        } else if (currentDocument != null) {
            // The new file was promoted only after its temporary contents were verified.
            // A leftover rollback is therefore safe to discard on the next run.
            rollbackDocuments.forEach { document ->
                runCatching { DocumentsContract.deleteDocument(resolver, document.uri) }
            }
        }

        val temporaryName = ".$filename.tmp-${System.currentTimeMillis()}"
        val temporaryUri = DocumentsContract.createDocument(
            resolver, parentDocumentUri, "application/octet-stream", temporaryName
        ) ?: throw IOException("ساخت فایل موقت در پوشه پشتیبان ناموفق بود.")
        var rollbackUri: Uri? = null
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
            val stagedBytes = resolver.openInputStream(temporaryUri)?.use { it.readBytes() }
                ?: throw IOException("بررسی فایل موقت پشتیبان ناموفق بود.")
            check(stagedBytes.contentEquals(payload)) {
                "بررسی صحت فایل پشتیبان موقت ناموفق بود."
            }

            val oldDocument = listDocuments(context, treeUri).firstOrNull { it.name == filename }
            if (oldDocument != null) {
                val rollbackName = ".$filename.rollback-${System.currentTimeMillis()}"
                rollbackUri = DocumentsContract.renameDocument(resolver, oldDocument.uri, rollbackName)
                    ?: throw IOException("انتقال امن نسخه قبلی پشتیبان ناموفق بود.")
            }

            val finalUri = DocumentsContract.renameDocument(resolver, temporaryUri, filename)
                ?: throw IOException("پوشه انتخاب‌شده امکان نهایی‌کردن نام فایل پشتیبان را ندارد.")
            val finalBytes = resolver.openInputStream(finalUri)?.use { it.readBytes() }
                ?: throw IOException("خواندن فایل پشتیبان پس از جایگزینی ناموفق بود.")
            check(finalBytes.contentEquals(payload)) {
                "بررسی صحت فایل پشتیبان پس از جایگزینی ناموفق بود."
            }

            // The replacement is now verified. Only now may the previous copy be removed.
            rollbackUri?.let { oldUri ->
                runCatching { DocumentsContract.deleteDocument(resolver, oldUri) }
                    .onFailure { error ->
                        android.util.Log.w("BackupFolderStorage", "Could not remove verified backup rollback file.", error)
                    }
            }

            // Remove only dated automatic backups belonging to this user and this frequency.
            // The stable destination is deliberately excluded; manual/emergency files do not match.
            runCatching {
                listDocuments(context, treeUri)
                    .filter {
                        (it.name.startsWith(retentionPrefix) &&
                            it.name != filename &&
                            it.name.endsWith(".darino", ignoreCase = true)) ||
                            it.name.startsWith(".$filename.tmp-") ||
                            it.name.startsWith(".$filename.rollback-")
                    }
                    .forEach { old ->
                        if (!runCatching { DocumentsContract.deleteDocument(resolver, old.uri) }.getOrDefault(false)) {
                            android.util.Log.w("BackupFolderStorage", "Could not remove superseded automatic backup: ${old.name}")
                        }
                    }
            }.onFailure { error ->
                android.util.Log.w("BackupFolderStorage", "Automatic backup saved, but old-version cleanup failed.", error)
            }
            return finalUri
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, temporaryUri) }
            val previous = rollbackUri
            if (previous != null) {
                runCatching {
                    listDocuments(context, treeUri)
                        .firstOrNull { it.name == filename }
                        ?.let { DocumentsContract.deleteDocument(resolver, it.uri) }
                    DocumentsContract.renameDocument(resolver, previous, filename)
                        ?: throw IOException("بازگرداندن نسخه قبلی پشتیبان ناموفق بود.")
                }.onFailure { restoreError ->
                    error.addSuppressed(restoreError)
                    android.util.Log.e("BackupFolderStorage", "Failed to restore the previous automatic backup.", restoreError)
                }
            }
            throw error
        }
    }

    /**
     * Returns the newest document for each requested automatic-backup prefix.
     * Keeping one candidate per frequency lets the repository compare the backup's
     * embedded createdAt value, so fixed filenames never make "weekly" win by spelling.
     */
    fun latestBackups(
        context: Context,
        namePrefixes: List<String>
    ): List<BackupDocument> {
        val treeUri = selectedFolderUri(context) ?: return emptyList()
        val hasReadGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!hasReadGrant) throw IOException("دسترسی خواندن پوشه پشتیبان لغو شده است؛ پوشه را دوباره انتخاب کنید.")
        val documents = listDocuments(context, treeUri)
            .filter { document ->
                document.name.endsWith(".darino", ignoreCase = true) &&
                    namePrefixes.any(document.name::startsWith)
            }
        return namePrefixes.mapNotNull { prefix ->
            documents.asSequence()
                .filter { it.name.startsWith(prefix) }
                .maxWithOrNull { first, second ->
                    if (first.lastModified > 0L && second.lastModified > 0L) {
                        first.lastModified.compareTo(second.lastModified)
                    } else {
                        first.name.compareTo(second.name)
                    }
                }
        }
    }

    private fun pruneBackups(
        context: Context,
        treeUri: Uri,
        prefix: String,
        keepCount: Int
    ) {
        val expired = listDocuments(context, treeUri)
            .filter { it.name.startsWith(prefix) && it.name.endsWith(".darino", ignoreCase = true) }
            .sortedByDescending { it.name }
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
        if (!isUsableTreeUri(treeUri)) {
            throw IOException("نشانی پوشه پشتیبان معتبر نیست؛ لطفاً پوشه را دوباره انتخاب کنید.")
        }
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
