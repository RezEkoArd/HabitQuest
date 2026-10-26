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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.domain.Habit
import java.time.LocalTime
import java.time.LocalDate
import com.rezekoard.habitquest.domain.DailyHabitProgress
import com.rezekoard.habitquest.domain.weekdayLabel

@Composable
internal fun LogProgressDialog(habit: Habit, onDismiss: () -> Unit, saving: Boolean = false, onSave: (Int) -> Unit) {
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
        confirmButton = { TextButton(onClick = { number?.takeIf { valid }?.let(onSave) }, enabled = valid && !saving) { Text(if (saving) "Menyimpan…" else "Simpan progres") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } },
    )
}

@Composable
internal fun CreateHabitDialog(
    onDismiss: () -> Unit,
    saving: Boolean = false,
    habit: Habit? = null,
    onArchive: () -> Unit = {},
    onSave: (String, String, Int, String, LocalTime?, Int) -> Unit,
) {
    var name by rememberSaveable(habit?.id) { mutableStateOf(habit?.name ?: "") }
    var category by rememberSaveable(habit?.id) { mutableStateOf(habit?.category ?: "Olahraga") }
    var target by rememberSaveable(habit?.id) { mutableStateOf(habit?.configuredTarget?.toString() ?: "3") }
    var unit by rememberSaveable(habit?.id) { mutableStateOf(habit?.unit ?: "km") }
    var weekdays by rememberSaveable(habit?.id) { mutableStateOf(habit?.configuredWeekdays ?: 127) }
    var reminder by rememberSaveable { mutableStateOf(true) }
    var time by rememberSaveable { mutableStateOf("07:00") }
    val targetNumber = target.toIntOrNull()
    val parsedTime = runCatching { LocalTime.parse(time.trim()) }.getOrNull()
    val validTarget = targetNumber != null && targetNumber in 1..10_000
    val valid = name.isNotBlank() && unit.isNotBlank() && validTarget && weekdays in 1..127 && (habit != null || !reminder || parsedTime != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (habit == null) "Habit baru" else "Edit habit") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(if (habit == null) "Buat target kecil yang bisa kamu ulangi." else "Nama langsung diperbarui. Target dan jadwal baru berlaku mulai besok; riwayat sebelumnya tetap.", color = Muted)
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
                                if (habit == null) unit = when (item) { "Olahraga" -> "km"; "Belajar" -> "halaman"; "Kreativitas" -> "konten"; else -> "kali" }
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
                        readOnly = habit != null,
                    )
                }
                if (habit != null) Text("Satuan tetap agar riwayat dapat dijumlahkan. Jam pengingat diubah lewat ikon lonceng.", style = MaterialTheme.typography.bodySmall, color = Muted)
                WeekdayPicker(weekdays, enabled = !saving, onChange = { weekdays = it })
                if (habit == null) {
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
                Text("Satu pengingat per hari jika target belum selesai. Waktu dapat bergeser karena penghemat baterai.", style = MaterialTheme.typography.bodySmall, color = Muted)
                } else {
                    TextButton(onClick = onArchive, enabled = !saving) { Text(if (habit.isArchived) "Pulihkan habit" else "Arsipkan habit") }
                    if (habit.isArchived) Text("Habit sedang diarsipkan. Target dan jadwal digunakan saat dipulihkan.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !saving,
                onClick = { if (valid) onSave(name.trim(), category, targetNumber!!, unit.trim(), if (habit != null) habit.reminderTime else if (reminder) parsedTime else null, weekdays) },
            ) { Text(if (saving) "Menyimpan…" else if (habit == null) "Buat habit" else "Simpan perubahan") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } },
    )
}

@Composable
private fun WeekdayPicker(mask: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hari pelaksanaan", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { onChange(127) }, enabled = enabled) { Text("Setiap hari") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEachIndexed { index, day ->
                val bit = 1 shl index
                FilterChip(selected = mask and bit != 0, onClick = { onChange(mask xor bit) }, enabled = enabled, label = { Text(day) })
            }
        }
        Text(if (mask == 0) "Pilih minimal satu hari." else weekdayLabel(mask), style = MaterialTheme.typography.bodySmall, color = if (mask == 0) MaterialTheme.colorScheme.error else Muted)
    }
}

@Composable
internal fun CorrectProgressDialog(habit: Habit, date: LocalDate, record: DailyHabitProgress?, saving: Boolean, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val target = record?.target ?: habit.planOn(date).target
    var amount by rememberSaveable(habit.id, date.toString()) { mutableStateOf((record?.amount ?: 0).toString()) }
    val parsed = amount.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Koreksi progres") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text(date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"))), color = Muted)
                OutlinedTextField(value = amount, onValueChange = { amount = it.take(5) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Jumlah akhir (${habit.unit})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = parsed == null || parsed !in 0..target, supportingText = { Text("Isi 0–$target. Ini jumlah akhir, bukan tambahan.") })
                Text("XP dan level disesuaikan dengan hasil koreksi. Jika target sebelumnya selesai menjadi belum selesai, 20 XP hari itu dikurangi.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        },
        confirmButton = { TextButton(onClick = { parsed?.let(onSave) }, enabled = !saving && parsed != null && parsed in 0..target) { Text(if (saving) "Menyimpan…" else "Simpan koreksi") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } },
    )
}

@Composable
internal fun EditNameDialog(currentName: String, saving: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Edit nama profil") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, label = { Text("Nama panggilan") }, singleLine = true, enabled = !saving, supportingText = { Text("Maksimal 30 karakter.") }) },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank() && !saving) { Text("Simpan nama") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } })
}

@Composable
internal fun ManageHabitsDialog(habits: List<Habit>, onDismiss: () -> Unit, onEdit: (Long) -> Unit, onArchive: (Long) -> Unit = {}, saving: Boolean = false, onMove: (Long, Int) -> Unit = { _, _ -> }) {
    var archived by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Kelola habit") },
        text = {
            Column(Modifier.heightIn(max = 440.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !archived, onClick = { archived = false }, label = { Text("Aktif") })
                    FilterChip(selected = archived, onClick = { archived = true }, label = { Text("Arsip") })
                }
                androidx.compose.foundation.lazy.LazyColumn {
                    val list = habits.filter { it.isArchived == archived }
                    if (list.isEmpty()) item { Text(if (archived) "Belum ada habit di arsip." else "Belum ada habit aktif.", color = Muted, modifier = Modifier.padding(vertical = 20.dp)) }
                    list.forEachIndexed { index, habit -> item(key = habit.id) {
                        Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onEdit(habit.id) }, modifier = Modifier.weight(1f)) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${habit.configuredTarget} ${habit.unit} · ${weekdayLabel(habit.configuredWeekdays)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                                }
                            }
                            TextButton(onClick = { onArchive(habit.id) }, modifier = Modifier.semantics {
                                contentDescription = "${if (habit.isArchived) "Pulihkan" else "Arsipkan"} ${habit.name}"
                            }) { Text(if (habit.isArchived) "Pulihkan" else "Arsipkan") }
                        }
                        if (!archived) Row {
                            TextButton(onClick = { onMove(habit.id, -1) }, enabled = !saving && index > 0,
                                modifier = Modifier.semantics { contentDescription = "Naikkan ${habit.name}" }) { Text("↑ Naik") }
                            TextButton(onClick = { onMove(habit.id, 1) }, enabled = !saving && index < list.lastIndex,
                                modifier = Modifier.semantics { contentDescription = "Turunkan ${habit.name}" }) { Text("↓ Turun") }
                        }
                        }
                    } }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } })
}

@Composable
internal fun HabitNoteDialog(name: String, date: LocalDate, initial: String, saving: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var body by rememberSaveable(name, date.toString()) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Catatan habit") }, containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"))), color = Muted)
                OutlinedTextField(value = body, onValueChange = { body = it.take(2_000) }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 7, label = { Text("Catatan aktivitas") },
                    placeholder = { Text("Buku yang dibaca, latihan hari ini, atau hal yang ingin diingat…") },
                    supportingText = { Text("${body.length}/2000 · Kosongkan untuk menghapus. Catatan tidak menambah XP.") })
            }
        }, confirmButton = { TextButton(onClick = { onSave(body) }, enabled = !saving) { Text("Simpan catatan") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } })
}

@Composable
internal fun ReminderDialog(habit: Habit, saving: Boolean, onDismiss: () -> Unit, onSave: (LocalTime?) -> Unit) {
    var enabled by rememberSaveable(habit.id) { mutableStateOf(habit.reminderTime != null) }
    var time by rememberSaveable(habit.id) { mutableStateOf(habit.reminderTime?.toString() ?: "07:00") }
    val parsed = runCatching { LocalTime.parse(time.trim()) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Pengingat habit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ingatkan setiap hari", Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !saving)
                }
                if (enabled) {
                    OutlinedTextField(value = time, onValueChange = { time = it.take(5) }, label = { Text("Jam (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving, isError = parsed == null,
                        supportingText = { Text("Format 24 jam, misalnya 07:00.") })
                }
                Text("Dilewati jika target hari ini sudah selesai. Jadwal mengikuti jam lokal perangkat dan dapat bergeser karena penghemat baterai.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(if (enabled) parsed else null) }, enabled = !saving && (!enabled || parsed != null)) { Text(if (saving) "Menyimpan…" else "Simpan pengingat") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } },
    )
}
