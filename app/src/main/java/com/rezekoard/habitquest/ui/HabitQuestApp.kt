package com.rezekoard.habitquest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.R
import java.time.LocalTime
import kotlinx.coroutines.launch

private enum class Destination(val label: String) {
    TODAY("Hari Ini"), PROGRESS("Progres"), PROFILE("Profil"),
}

@Composable
fun HabitQuestApp(
    state: HabitUiState,
    onAdvance: (Long, Int) -> Int,
    onAddHabit: (String, String, Int, String, LocalTime?) -> Unit,
) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.TODAY.name) }
    val destination = Destination.valueOf(destinationName)
    var progressHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var selectedProgressHabit by rememberSaveable { mutableStateOf(2L) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pageStates = rememberSaveableStateHolder()

    BackHandler(enabled = destination != Destination.TODAY) { destinationName = Destination.TODAY.name }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 22.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_sprout), null, tint = Forest, modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(7.dp))
                Text("habitquest", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(9.dp))
                Text("DEMO", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(
                    onClick = { destinationName = Destination.PROFILE.name },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) { Text("R", fontWeight = FontWeight.SemiBold) }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = Hairline)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        Destination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destinationName = item.name },
                                icon = {
                                    when (item) {
                                        Destination.TODAY -> Icon(Icons.Outlined.Home, null)
                                        Destination.PROGRESS -> Icon(painterResource(R.drawable.ic_chart), null)
                                        Destination.PROFILE -> Icon(Icons.Outlined.Person, null)
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Forest, selectedTextColor = Forest,
                                    indicatorColor = SoftGreen, unselectedIconColor = Muted, unselectedTextColor = Muted,
                                ),
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            pageStates.SaveableStateProvider(destination.name) {
                when (destination) {
                    Destination.TODAY -> TodayScreen(
                        state = state,
                        onLog = { progressHabitId = it },
                        onCreate = { showCreate = true },
                    )
                    Destination.PROGRESS -> ProgressScreen(
                        state = state,
                        selectedHabitId = selectedProgressHabit,
                        onSelectHabit = { selectedProgressHabit = it },
                        onToday = { destinationName = Destination.TODAY.name },
                    )
                    Destination.PROFILE -> ProfileScreen(state)
                }
            }
        }
    }

    state.habits.find { it.id == progressHabitId }?.let { habit ->
        LogProgressDialog(
            habit = habit,
            onDismiss = { progressHabitId = null },
            onSave = { amount ->
                val earnedXp = onAdvance(habit.id, amount)
                progressHabitId = null
                scope.launch {
                    snackbar.showSnackbar(if (earnedXp > 0) "Target tercapai! +$earnedXp XP untuk langkah baikmu." else "Progres tersimpan. Lanjutkan ritmemu!")
                }
            },
        )
    }
    if (showCreate) {
        CreateHabitDialog(
            onDismiss = { showCreate = false },
            onSave = { name, category, target, unit, time ->
                onAddHabit(name, category, target, unit, time)
                showCreate = false
                scope.launch { snackbar.showSnackbar("Habit baru ditambahkan. Selamat memulai!") }
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HabitQuestPreview() {
    HabitQuestTheme { HabitQuestApp(HabitUiState(), { _, _ -> 0 }, { _, _, _, _, _ -> }) }
}
