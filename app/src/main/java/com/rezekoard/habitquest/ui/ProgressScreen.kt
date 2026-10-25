package com.rezekoard.habitquest.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.R
import com.rezekoard.habitquest.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ProgressLocale = Locale.forLanguageTag("id-ID")
private val FullDate = DateTimeFormatter.ofPattern("d MMMM yyyy", ProgressLocale)

@Composable
fun ProgressScreen(state: HabitUiState, selectedHabitId: Long, onSelectHabit: (Long) -> Unit, onToday: () -> Unit, onReminder: (Long) -> Unit = {}, notificationsAllowed: Boolean = true) {
    val habit = state.habits.find { it.id == selectedHabitId } ?: state.habits.firstOrNull()
    if (habit == null) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_chart), null, Modifier.size(48.dp), tint = Forest)
            Spacer(Modifier.height(18.dp))
            Text("Belum ada progres", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Mulai satu kebiasaan untuk melihat progresmu.", color = Muted, textAlign = TextAlign.Center)
            TextButton(onClick = onToday) { Text("Buka Hari Ini") }
        }
        return
    }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(state.date).toString()) }
    val month = minOf(YearMonth.parse(monthKey), YearMonth.from(state.date))
    var selectedDayKey by rememberSaveable(habit.id, month.toString()) {
        mutableStateOf(if (month == YearMonth.from(state.date)) state.date.toString() else month.atEndOfMonth().toString())
    }
    val records = remember(habit, state.history, state.date) { state.recordsFor(habit) }
    val byDate = remember(records) { records.associateBy { it.date } }
    val summary = remember(habit, records, month, state.date) { summarizeMonth(habit, records, month, state.date) }
    val selectedDate = LocalDate.parse(selectedDayKey)
    val allCompleted = records.count { it.isComplete && !it.date.isAfter(state.date) && !it.date.isBefore(habit.startedOn) }
    val streak = bestStreak(habit, records, state.date)

    LazyColumn(
        Modifier.widthIn(max = 600.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 15.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "heading") {
            HabitSelector(state.habits, habit, onSelectHabit)
        }
        item(key = "schedule") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("Setiap hari", calendar = true)
                InfoPill("Mulai ${habit.startedOn.format(DateTimeFormatter.ofPattern("d MMM yyyy", ProgressLocale))}")
            }
        }
        item(key = "calendar") {
            MonthCalendar(
                habit = habit, month = month, today = state.date, byDate = byDate,
                selectedDate = selectedDate, summary = summary,
                onSelectDate = { selectedDayKey = it.toString() },
                onPrevious = { monthKey = month.minusMonths(1).toString() },
                onNext = { monthKey = month.plusMonths(1).toString() },
            )
        }
        item(key = "metrics") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Target tercapai", "${summary.completionPercent}%", "${summary.completedDays} dari ${summary.scheduledDays} hari", Modifier.weight(1f))
                MetricCard("Total ${habit.unit}", "${summary.totalAmount}", habit.unit, Modifier.weight(1f))
            }
            if (summary.scheduledDays == 0) {
                Text("Habit belum dimulai pada bulan ini.", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 9.dp))
            }
        }
        item(key = "milestone") { MilestoneCard(allCompleted, streak) }
        item(key = "reminder") {
            ProgressCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Notifications, null, tint = Forest)
                    Column(Modifier.weight(1f)) {
                        Text(
                            habit.reminderTime?.let { "Jadwal pukul ${it.format(DateTimeFormatter.ofPattern("HH.mm"))}" } ?: "Tanpa pengingat",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(if (habit.reminderTime == null) "Belum ada jam pengingat untuk habit ini." else if (notificationsAllowed) "Setiap hari · mengikuti jam lokal" else "Notifikasi belum diizinkan", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                TextButton(onClick = { onReminder(habit.id) }) { Text("Ubah pengingat") }
            }
        }
        item(key = "source") {
            Text(
                "Progres dihitung sejak habit dibuat dan disimpan secara lokal di perangkat ini.",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
        }
    }
}

@Composable
private fun HabitSelector(habits: List<Habit>, habit: Habit, onSelectHabit: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("PROGRES HABIT", style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClickLabel = "Pilih habit") { expanded = true }
                        .semantics { contentDescription = "Pilih habit: ${habit.name}" }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(habit.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f, fill = false), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Outlined.ArrowDropDown, null, tint = Forest)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = MaterialTheme.colorScheme.surface) {
                    habits.forEach { item ->
                        DropdownMenuItem(
                            text = { Column { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(item.category, style = MaterialTheme.typography.bodySmall, color = Muted) } },
                            onClick = { onSelectHabit(item.id); expanded = false },
                            trailingIcon = { if (item.id == habit.id) Text("Dipilih", style = MaterialTheme.typography.labelSmall, color = Forest) },
                        )
                    }
                }
            }
            Box(Modifier.size(44.dp).background(SoftGreen, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                val icon = when (habit.category) { "Olahraga" -> R.drawable.ic_workout; "Belajar" -> R.drawable.ic_book; "Kreativitas" -> R.drawable.ic_content; else -> R.drawable.ic_sprout }
                Icon(painterResource(icon), null, tint = Forest)
            }
        }
        Text("${habit.category} · Target ${habit.target} ${habit.unit} / hari", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(8.dp))
        Text("Kemajuanmu layak untuk dilihat.", style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun InfoPill(text: String, calendar: Boolean = false) {
    Surface(color = SoftGreen, contentColor = Forest, shape = CircleShape) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (calendar) Icon(Icons.Outlined.DateRange, null, Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProgressCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Hairline)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}

@Composable
private fun MonthCalendar(
    habit: Habit, month: YearMonth, today: LocalDate, byDate: Map<LocalDate, DailyHabitProgress>,
    selectedDate: LocalDate, summary: MonthlyHabitSummary,
    onSelectDate: (LocalDate) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit,
) {
    ProgressCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Bulan sebelumnya") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", ProgressLocale)).replaceFirstChar { it.titlecase(ProgressLocale) }, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("${summary.completedDays} / ${summary.scheduledDays} hari selesai", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            IconButton(onClick = onNext, enabled = month < YearMonth.from(today)) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Bulan berikutnya") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "S", "R", "K", "J", "S", "M").forEach { day ->
                Text(day, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val weeks = (offset + month.lengthOfMonth() + 6) / 7
        repeat(weeks) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { weekday ->
                    val number = week * 7 + weekday - offset + 1
                    if (number !in 1..month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).height(44.dp))
                    } else {
                        val date = month.atDay(number)
                        CalendarDay(
                            date = date, status = dayStatus(habit, date, today, byDate[date]),
                            selected = selectedDate == date, modifier = Modifier.weight(1f),
                            onClick = { onSelectDate(date) },
                        )
                    }
                }
            }
            if (week < weeks - 1) Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CalendarLegend("Selesai", DayStatus.COMPLETE)
            CalendarLegend("Sebagian", DayStatus.PARTIAL)
            CalendarLegend("Terlewat", DayStatus.MISSED)
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Hairline)
        Spacer(Modifier.height(12.dp))
        val record = byDate[selectedDate]
        val status = dayStatus(habit, selectedDate, today, record)
        Text(selectedDate.format(FullDate), style = MaterialTheme.typography.labelLarge)
        Text(
            when (status) {
                DayStatus.FUTURE, DayStatus.NOT_STARTED -> status.label
                else -> "${record?.amount ?: 0} / ${record?.target ?: habit.target} ${habit.unit} · ${status.label}"
            },
            style = MaterialTheme.typography.bodySmall, color = Muted,
        )
    }
}

@Composable
private fun CalendarDay(date: LocalDate, status: DayStatus, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val inactive = status == DayStatus.FUTURE || status == DayStatus.NOT_STARTED
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (status == DayStatus.COMPLETE) SoftGreen else Canvas,
        border = when { selected -> BorderStroke(2.dp, Forest); status == DayStatus.PARTIAL -> BorderStroke(1.dp, Forest); else -> null },
    ) {
        Box(
            Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = "${date.format(FullDate)}, ${status.label}" },
            contentAlignment = Alignment.Center,
        ) {
            Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodySmall,
                color = if (inactive) Muted.copy(alpha = .5f) else if (status == DayStatus.COMPLETE || status == DayStatus.PARTIAL) Forest else Ink,
                fontWeight = if (status == DayStatus.COMPLETE) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (status == DayStatus.MISSED) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(3.dp).background(Muted, CircleShape))
        }
    }
}

@Composable
private fun CalendarLegend(label: String, status: DayStatus) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(
            Modifier.size(10.dp), shape = RoundedCornerShape(3.dp),
            color = if (status == DayStatus.COMPLETE) SoftGreen else Canvas,
            border = if (status == DayStatus.PARTIAL) BorderStroke(1.dp, Forest) else null,
        ) {
            if (status == DayStatus.MISSED) Box(contentAlignment = Alignment.Center) { Box(Modifier.size(3.dp).background(Muted, CircleShape)) }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun MetricCard(label: String, value: String, detail: String, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Hairline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun MilestoneCard(completed: Int, bestStreak: Int) {
    ProgressCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Star, null, Modifier.size(21.dp), tint = Forest)
            Text(if (completed >= 30) "30 hari tercapai!" else "Menuju 30 hari", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${completed.coerceAtMost(30)} / 30", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { completed.coerceAtMost(30) / 30f }, modifier = Modifier.fillMaxWidth().height(5.dp),
            color = Forest, trackColor = Hairline, gapSize = 0.dp, drawStopIndicator = {},
        )
        Spacer(Modifier.height(10.dp))
        Text(if (completed >= 30) "$completed hari selesai sejak mulai. Teruskan langkah baikmu." else "Satu hari demi satu hari. Badge berikutnya menantimu.", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(6.dp))
        Text("Streak terbaik: $bestStreak hari", style = MaterialTheme.typography.labelSmall, color = Forest)
    }
}
