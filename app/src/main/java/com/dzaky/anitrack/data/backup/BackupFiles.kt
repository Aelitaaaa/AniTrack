package com.dzaky.anitrack.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupFiles @Inject constructor(@ApplicationContext private val context: Context, private val repository: BackupRepository) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val bytes = repository.export()
        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "Cannot write to this file." }.use { it.write(bytes) }
    }

    suspend fun restore(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val bytes = checkNotNull(context.contentResolver.openInputStream(uri)) { "Cannot read this file." }.use(BackupCodec::readLimited)
        repository.restore(bytes)
    }
}
