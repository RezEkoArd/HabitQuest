package com.rezekoard.habitquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.domain.Habit
import java.time.LocalTime

@Composable
internal fun LogProgressDialog(habit: Habit, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var amount by rememberSaveable(habit.id) { mutableStateOf((habit.target - habit.progress).coerceAtLeast(1).toString()) }
    val number = amount.toIntOrNull()
    val valid = number != null && number in 1..10_000
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Catat progres") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text("${habit.progress} dari ${habit.target} ${habit.unit} sudah dikerjakan.", color = Muted)
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.take(5) },
                    label = { Text("Tambah ${habit.unit}") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amount.isNotEmpty() && !valid,
                    supportingText = { Text(if (amount.isNotEmpty() && !valid) "Isi angka 1–10.000." else "+20 XP saat target harian tercapai.") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10).forEach { value ->
                        OutlinedButton(onClick = { amount = value.toString() }, modifier = Modifier.weight(1f)) { Text("+$value") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { number?.takeIf { valid }?.let(onSave) }, enabled = valid) { Text("Simpan progres") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
internal fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, LocalTime?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Olahraga") }
    var target by rememberSaveable { mutableStateOf("3") }
    var unit by rememberSaveable { mutableStateOf("km") }
    var reminder by rememberSaveable { mutableStateOf(true) }
    var time by rememberSaveable { mutableStateOf("07:00") }
    val targetNumber = target.toIntOrNull()
    val parsedTime = runCatching { LocalTime.parse(time.trim()) }.getOrNull()
    val validTarget = targetNumber != null && targetNumber in 1..10_000
    val valid = name.isNotBlank() && unit.isNotBlank() && validTarget && (!reminder || parsedTime != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Habit baru") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Buat target kecil yang bisa kamu ulangi.", color = Muted)
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(60) },
                    label = { Text("Nama habit") }, placeholder = { Text("Contoh: Lari pagi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Text("Kategori", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Olahraga", "Belajar", "Kreativitas", "Personal").forEach { item ->
                        FilterChip(
                            selected = category == item, label = { Text(item) },
                            onClick = {
                                category = item
                                unit = when (item) { "Olahraga" -> "km"; "Belajar" -> "halaman"; "Kreativitas" -> "konten"; else -> "kali" }
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = target, onValueChange = { target = it.take(5) },
                        label = { Text("Target harian") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = target.isNotEmpty() && !validTarget,
                    )
                    OutlinedTextField(
                        value = unit, onValueChange = { unit = it.take(20) },
                        label = { Text("Satuan") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                Text("Jadwal: setiap hari", style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Jam pengingat", modifier = Modifier.weight(1f))
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }
                if (reminder) {
                    OutlinedTextField(
                        value = time, onValueChange = { time = it.take(5) },
                        label = { Text("Jam (HH:mm)") }, placeholder = { Text("07:00") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = parsedTime == null,
                        supportingText = { Text("Format 24 jam, misalnya 07:00 atau 20:30.") },
                    )
                }
                Text("Mode demo: pengingat belum mengirim notifikasi.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { if (valid) onSave(name.trim(), category, targetNumber!!, unit.trim(), if (reminder) parsedTime else null) },
            ) { Text("Buat habit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
