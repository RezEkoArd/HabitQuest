package com.rezekoard.habitquest.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.domain.comparePeriods
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun PeriodSummaryCard(state: HabitUiState) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val report = remember(state.habits, state.history, state.date, monthly, offset) {
        comparePeriods(state.habits, state.history, state.date, monthly, offset)
    }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")) }
    Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Hairline), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ringkasan semua habit", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !monthly, onClick = { monthly = false; offset = 0 }, label = { Text("Mingguan") })
                FilterChip(selected = monthly, onClick = { monthly = true; offset = 0 }, label = { Text("Bulanan") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { offset++ }, enabled = offset < 120) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text("${report.start.format(formatter)} – ${report.end.format(formatter)}", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { offset-- }, enabled = offset > 0) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }
            if (report.current.scheduled == 0) Text("Belum ada jadwal pada periode ini.", color = Muted)
            else {
                Text("${report.current.percent}% konsisten", style = MaterialTheme.typography.headlineSmall, color = Forest)
                LinearProgressIndicator(progress = { report.current.percent / 100f }, modifier = Modifier.fillMaxWidth(), color = Forest, trackColor = Hairline)
            }
            Text("${report.current.completed} / ${report.current.scheduled} target selesai · ${report.current.activeDays} hari aktif · ${report.current.xp} XP", style = MaterialTheme.typography.bodyMedium)
            val change = report.difference
            Text(if (change == null) "Belum cukup jadwal untuk perbandingan."
                else "${if (change > 0) "+" else ""}$change poin persentase dibanding periode sebelumnya (${report.previous.percent}%).", color = Muted, style = MaterialTheme.typography.bodySmall)
            Text("Periode berjalan dihitung sampai hari ini dan dibandingkan dengan jumlah hari yang sama. Habit arsip tetap masuk riwayat.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
