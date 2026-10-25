package com.rezekoard.habitquest.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.R
import com.rezekoard.habitquest.domain.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val Indonesian = Locale.forLanguageTag("id-ID")

@Composable
fun TodayScreen(state: HabitUiState, onLog: (Long) -> Unit, onCreate: () -> Unit) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 15.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "greeting") {
            Column {
                Text(
                    state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Indonesian))
                        .replaceFirstChar { it.titlecase(Indonesian) },
                    style = MaterialTheme.typography.bodySmall, color = Muted,
                )
                Spacer(Modifier.height(5.dp))
                Text("${greeting()}, Rezek.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(5.dp))
                Text("Sedikit lebih baik dari kemarin.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        item(key = "week") { WeekStrip(state.date) }
        item(key = "xp") { XpSummary(state) }
        item(key = "section") {
            Row(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rencana hari ini", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${state.habits.size} habit", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        items(state.habits, key = { it.id }) { habit -> HabitCard(habit, onLog = { onLog(habit.id) }) }
        item(key = "create") {
            OutlinedButton(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Hairline),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buat habit baru")
            }
        }
        item(key = "demo") {
            Text(
                "Mode demo · Data tersimpan selama sesi aplikasi. Pengingat belum aktif.",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

@Composable
private fun WeekStrip(today: LocalDate) {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(7) { index ->
            val day = monday.plusDays(index.toLong())
            val selected = day == today
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Forest else Color.Transparent)
                    .padding(vertical = 10.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Indonesian)) + if (selected) ", hari ini" else ""
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(listOf("S", "S", "R", "K", "J", "S", "M")[index], style = MaterialTheme.typography.labelSmall, color = if (selected) Color.White.copy(alpha = .8f) else Muted)
                Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = if (selected) Color.White else Ink)
                Box(Modifier.size(4.dp).background(if (selected) Color.White else Color.Transparent, CircleShape))
            }
        }
    }
}

@Composable
private fun XpSummary(state: HabitUiState) {
    Surface(color = DeepForest, contentColor = Color(0xFFF3F7EB), shape = RoundedCornerShape(23.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Icon(
                painterResource(R.drawable.ic_sprout), null,
                modifier = Modifier.size(112.dp).align(Alignment.TopEnd).padding(4.dp),
                tint = Color.White.copy(alpha = .09f),
            )
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("${state.completedCount} dari ${state.habits.size} habit selesai", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD3E0CA))
                Spacer(Modifier.height(6.dp))
                Text(if (state.completedCount == state.habits.size) "Hari ini, kamu berhasil." else "Jaga ritme baikmu.", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Transparent, contentColor = Color(0xFFF3F7EB),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .2f)), shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("LEVEL ${state.level}", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${state.xp % 100} / 100 XP", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD3E0CA))
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (state.xp % 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = Color(0xFFC7DEA4), trackColor = Color.White.copy(alpha = .15f),
                    gapSize = 0.dp, drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun HabitCard(habit: Habit, onLog: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, Hairline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(42.dp).background(SoftGreen, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val drawable = when (habit.category) {
                        "Olahraga" -> R.drawable.ic_workout
                        "Belajar" -> R.drawable.ic_book
                        "Kreativitas" -> R.drawable.ic_content
                        else -> R.drawable.ic_sprout
                    }
                    Icon(painterResource(drawable), null, Modifier.size(22.dp), tint = Forest)
                }
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${habit.category} · ${if (habit.isComplete) "Selesai hari ini" else "Target harian"}",
                        style = MaterialTheme.typography.labelSmall, color = Muted,
                    )
                }
                if (habit.isComplete) {
                    Box(Modifier.size(48.dp).background(SoftGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, "${habit.name} selesai", tint = Forest)
                    }
                } else {
                    OutlinedIconButton(
                        onClick = onLog, modifier = Modifier.size(48.dp),
                        border = BorderStroke(1.dp, Hairline),
                    ) { Icon(Icons.Outlined.Add, "Catat progres ${habit.name}", tint = Forest) }
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("${habit.progress} / ${habit.target} ${habit.unit}", style = MaterialTheme.typography.bodySmall)
                if (habit.isComplete) {
                    Text("+20 XP diperoleh", style = MaterialTheme.typography.labelSmall, color = Muted)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Outlined.Notifications, null, Modifier.size(13.dp), tint = Muted)
                        Text(habit.reminderTime?.format(DateTimeFormatter.ofPattern("HH.mm")) ?: "Tanpa pengingat", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { habit.progress.toFloat() / habit.target },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Forest, trackColor = Hairline, gapSize = 0.dp, drawStopIndicator = {},
            )
        }
    }
}
