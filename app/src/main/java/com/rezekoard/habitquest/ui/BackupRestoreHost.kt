package com.rezekoard.habitquest.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.data.BackupSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreHost(onRead: suspend (Uri) -> BackupSummary, onRestore: suspend () -> Unit, content: @Composable (() -> Unit) -> Unit) {
    var summary by remember { mutableStateOf<BackupSummary?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try { summary = onRead(uri) }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { message = "File backup tidak valid, terlalu besar, atau tidak dapat dibaca. Data perangkat tidak diubah." }
                finally { busy = false }
            }
        }
    }
    content { if (!busy) picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
    if (summary != null) {
        val preview = checkNotNull(summary)
        AlertDialog(onDismissRequest = { if (!busy) summary = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Pulihkan backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil: ${preview.name}", style = MaterialTheme.typography.titleMedium)
                    Text("${preview.habits} habit (${preview.archived} di arsip)\n${preview.records} catatan harian")
                    Text("Data di perangkat ini akan diganti dengan isi backup. Simpan backup data saat ini terlebih dahulu jika masih diperlukan.", color = Muted)
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try { onRestore(); summary = null; message = "Backup berhasil dipulihkan." }
                    catch (error: CancellationException) { throw error }
                    catch (_: Exception) { message = "Backup belum berhasil dipulihkan. Data sebelumnya tetap tersimpan." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Memulihkan…" else "Ganti dan pulihkan") } },
            dismissButton = { TextButton(onClick = { summary = null }, enabled = !busy) { Text("Batal") } })
    } else if (busy) {
        AlertDialog(onDismissRequest = {}, title = { Text("Membaca backup…") }, text = { LinearProgressIndicator(Modifier.fillMaxWidth()) }, confirmButton = {})
    }
    if (message != null) AlertDialog(onDismissRequest = { message = null }, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Backup offline") }, text = { Text(checkNotNull(message)) }, confirmButton = { TextButton(onClick = { message = null }) { Text("Mengerti") } })
}
