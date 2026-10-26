package com.rezekoard.habitquest.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezekoard.habitquest.R
import com.rezekoard.habitquest.domain.Achievement
import com.rezekoard.habitquest.domain.achievementsFor
import com.rezekoard.habitquest.domain.summarizeProfile

@Composable
fun ProfileScreen(state: HabitUiState, onEditName: () -> Unit = {}, onManageHabits: () -> Unit = {}, onExport: () -> Unit = {}, onRestore: () -> Unit = {}) {
    val summary = remember(state.habits, state.history, state.date) {
        summarizeProfile(state.habits, state.habits.flatMap(state::recordsFor), state.date)
    }
    val badges = achievementsFor(summary)
    var selectedBadgeId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.widthIn(max = 600.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 15.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "identity") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).background(SoftGreen, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(state.initial, color = Forest, fontSize = 30.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(13.dp))
                Text(state.userName, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                Text("Menumbuhkan versi terbaik diri sendiri.", color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        item(key = "journey") { ProfileJourney(state) }
        item(key = "actions") {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Hairline)) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    TextButton(onClick = onEditName, modifier = Modifier.fillMaxWidth()) { Text("Edit nama profil") }
                    TextButton(onClick = onManageHabits, modifier = Modifier.fillMaxWidth()) { Text("Kelola habit dan arsip") }
                    HorizontalDivider(color = Hairline)
                    TextButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Buat backup offline") }
                    TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Pulihkan backup") }
                }
            }
        }
        item(key = "achievements") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Koleksi pencapaian", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${badges.count { it.isUnlocked }} / ${badges.size}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                badges.forEach { badge ->
                    AchievementCard(badge, Modifier.weight(1f)) { selectedBadgeId = badge.id }
                }
            }
        }
        item(key = "statistics") {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Hairline)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp)) {
                    ProfileStat("Hari aktif bulan ini", "${summary.activeDaysThisMonth} hari", icon = { Icon(Icons.Outlined.DateRange, null, tint = Forest) })
                    HorizontalDivider(color = Hairline)
                    ProfileStat("Streak terbaik", "${summary.longestHabitStreak} hari", icon = { Icon(painterResource(R.drawable.ic_flame), null, tint = Forest) })
                    HorizontalDivider(color = Hairline)
                    ProfileStat("Habit aktif", "${summary.habitCount} habit", icon = { Icon(painterResource(R.drawable.ic_sprout), null, tint = Forest) })
                }
            }
        }
        item(key = "encouragement") {
            Text("Tanpa pengurangan XP saat melewatkan hari.\nBesok selalu bisa mulai lagi.", Modifier.fillMaxWidth(), color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        item(key = "local") {
            Text("Statistik dan badge dihitung dari catatanmu. Data disimpan secara lokal di perangkat ini.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }

    badges.find { it.id == selectedBadgeId }?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadgeId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = { BadgeIcon(badge, Modifier.size(32.dp)) },
            title = { Text(badge.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(badge.description)
                    Text(if (badge.isUnlocked) "Badge sudah terbuka. Pertahankan langkah baikmu!" else "Belum terbuka. Sedikit demi sedikit tetap berarti.", color = Muted)
                    Text("${badge.displayedProgress} / ${badge.target} ${badge.unit}", style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(
                        progress = { badge.displayedProgress.toFloat() / badge.target },
                        modifier = Modifier.fillMaxWidth().height(5.dp), color = Forest, trackColor = Hairline,
                        gapSize = 0.dp, drawStopIndicator = {},
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedBadgeId = null }) { Text("Mengerti") } },
        )
    }
}

@Composable
private fun ProfileJourney(state: HabitUiState) {
    val title = when (state.level) { in 1..2 -> "Pemula"; in 3..4 -> "Perintis"; in 5..9 -> "Penumbuh"; else -> "Konsisten" }
    Surface(color = DeepForest, contentColor = Color(0xFFF3F7EB), shape = RoundedCornerShape(23.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("PERJALANANMU", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD3E0CA))
            Spacer(Modifier.height(7.dp))
            Text("Level ${state.level} · $title", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${state.xp} XP terkumpul", style = MaterialTheme.typography.bodyMedium)
                Text("${100 - state.xp % 100} XP lagi", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD3E0CA))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (state.xp % 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp).semantics { contentDescription = "Kemajuan menuju level ${state.level + 1}" },
                color = Color(0xFFC7DEA4), trackColor = Color.White.copy(alpha = .15f), gapSize = 0.dp, drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun AchievementCard(badge: Achievement, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 146.dp).semantics {
            contentDescription = "${badge.title}, ${if (badge.isUnlocked) "terbuka" else "terkunci"}. Lihat syarat badge"
        },
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, Hairline),
    ) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BadgeIcon(badge, Modifier.size(29.dp))
            Spacer(Modifier.height(11.dp))
            Text(badge.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = if (badge.isUnlocked) Ink else Muted)
            Spacer(Modifier.height(5.dp))
            Text(if (badge.isUnlocked) "Terbuka" else "Terkunci", style = MaterialTheme.typography.labelSmall, color = if (badge.isUnlocked) Forest else Muted)
        }
    }
}

@Composable
private fun BadgeIcon(badge: Achievement, modifier: Modifier) {
    val tint = if (badge.isUnlocked) Color(0xFFA76B22) else Muted.copy(alpha = .65f)
    when {
        badge.id == "first" -> Icon(painterResource(R.drawable.ic_sprout), null, modifier, tint = tint)
        badge.id == "week" -> Icon(painterResource(R.drawable.ic_flame), null, modifier, tint = tint)
        !badge.isUnlocked -> Icon(Icons.Outlined.Lock, null, modifier, tint = tint)
        else -> Icon(Icons.Outlined.Star, null, modifier, tint = tint)
    }
}

@Composable
private fun ProfileStat(label: String, value: String, icon: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ProfilePreview() {
    HabitQuestTheme { ProfileScreen(HabitUiState()) }
}
