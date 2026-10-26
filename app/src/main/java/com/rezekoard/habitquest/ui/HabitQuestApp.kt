package com.rezekoard.habitquest.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.R
import java.time.LocalTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rezekoard.habitquest.notifications.AndroidReminderNotifications
import java.time.LocalDate

private enum class Destination(val label: String) {
    TODAY("Hari Ini"), PROGRESS("Progres"), PROFILE("Profil"),
}

@Composable
fun HabitQuestApp(
    state: HabitUiState,
    onAdvance: suspend (Long, Int) -> Int,
    onAddHabit: suspend (String, String, Int, String, LocalTime?, Int) -> Unit,
    onUpdateReminder: suspend (Long, LocalTime?) -> Unit = { _, _ -> },
    onSyncReminders: suspend () -> Unit = {},
    requestedHabitId: Long? = null,
    onReminderHandled: () -> Unit = {},
    onEditHabit: suspend (Long, String, String, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onArchiveHabit: suspend (Long, Boolean) -> Unit = { _, _ -> },
    onCorrectProgress: suspend (Long, LocalDate, Int) -> Int = { _, _, _ -> 0 },
    onEditName: suspend (String) -> Unit = {},
    onExportBackup: suspend (Uri) -> Unit = {},
    onRequestRestore: () -> Unit = {},
    onSaveNote: suspend (Long, LocalDate, String) -> Unit = { _, _, _ -> },
    onMoveHabit: suspend (Long, Int) -> Unit = { _, _ -> },
) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.TODAY.name) }
    val destination = Destination.valueOf(destinationName)
    var progressHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var reminderHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedProgressHabit by rememberSaveable { mutableStateOf(2L) }
    var saving by remember { mutableStateOf(false) }
    var editHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var archiveHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var correctHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var correctDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showManage by rememberSaveable { mutableStateOf(false) }
    var showEditName by rememberSaveable { mutableStateOf(false) }
    var noteHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var noteDate by rememberSaveable { mutableStateOf(state.date.toString()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pageStates = rememberSaveableStateHolder()
    fun mutate(block: suspend () -> String) {
        if (saving) return
        saving = true
        scope.launch {
            val message = try { block() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { "Perubahan belum tersimpan. Silakan coba lagi." }
            finally { saving = false }
            snackbar.showSnackbar(message)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) mutate { onExportBackup(uri); "Backup berhasil disimpan." }
    }
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    val notificationAccess = remember(context, preview) { if (preview) null else AndroidReminderNotifications(context.applicationContext) }
    var notificationsAllowed by remember { mutableStateOf(preview || notificationAccess?.enabled() == true) }
    val openNotificationSettings = {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = notificationAccess?.enabled() == true
        scope.launch {
            onSyncReminders()
            if (!notificationsAllowed) {
                val result = snackbar.showSnackbar("Habit tetap tersimpan. Izinkan notifikasi untuk menerima pengingat.", actionLabel = "Pengaturan")
                if (result == SnackbarResult.ActionPerformed) openNotificationSettings()
            }
        }
    }
    val requestNotificationAccess = {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!preview) {
            notificationsAllowed = notificationAccess?.enabled() == true
            scope.launch { onSyncReminders() }
        }
    }
    LaunchedEffect(requestedHabitId, state.date, state.habits) {
        if (requestedHabitId != null && state.date == LocalDate.now()) {
            destinationName = Destination.TODAY.name
            reminderHabitId = null
            progressHabitId = state.habits.find { it.id == requestedHabitId && !it.isComplete && !it.isArchived && it.isScheduledOn(state.date) }?.id
            onReminderHandled()
        }
    }

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
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(
                    onClick = { destinationName = Destination.PROFILE.name },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) { Text(state.initial, fontWeight = FontWeight.SemiBold) }
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
                        onReminder = { reminderHabitId = it }, notificationsAllowed = notificationsAllowed,
                        onRequestNotifications = { requestNotificationAccess() },
                        onEdit = { editHabitId = it },
                        onCorrect = { correctHabitId = it; correctDate = state.date.toString() },
                        onNote = { noteHabitId = it; noteDate = state.date.toString() },
                    )
                    Destination.PROGRESS -> ProgressScreen(
                        state = state,
                        selectedHabitId = selectedProgressHabit,
                        onSelectHabit = { selectedProgressHabit = it },
                        onToday = { destinationName = Destination.TODAY.name },
                        onReminder = { reminderHabitId = it }, notificationsAllowed = notificationsAllowed,
                        onEdit = { editHabitId = it },
                        onCorrect = { id, date -> correctHabitId = id; correctDate = date.toString() },
                        onNote = { id, date -> noteHabitId = id; noteDate = date.toString() },
                    )
                    Destination.PROFILE -> ProfileScreen(state,
                        onEditName = { showEditName = true }, onManageHabits = { showManage = true },
                        onExport = { if (!saving) exportLauncher.launch("habitquest-backup-${state.date}.json") },
                        onRestore = { if (!saving) onRequestRestore() })
                }
            }
        }
    }

    state.habits.find { it.id == progressHabitId }?.let { habit ->
        LogProgressDialog(
            habit = habit,
            onDismiss = { if (!saving) progressHabitId = null },
            saving = saving,
            onSave = { amount ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        val message = try {
                            val earnedXp = onAdvance(habit.id, amount)
                            progressHabitId = null
                            if (earnedXp > 0) "Target tercapai! +$earnedXp XP untuk langkah baikmu." else "Progres tersimpan. Lanjutkan ritmemu!"
                        } catch (error: CancellationException) { throw error }
                        catch (_: Exception) { "Progres belum tersimpan. Silakan coba lagi." }
                        finally { saving = false }
                        snackbar.showSnackbar(message)
                    }
                }
            },
        )
    }
    if (showCreate) {
        CreateHabitDialog(
            onDismiss = { if (!saving) showCreate = false },
            saving = saving,
            onSave = { name, category, target, unit, time, weekdays ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        val message = try {
                            onAddHabit(name, category, target, unit, time, weekdays)
                            showCreate = false
                            if (time != null && !notificationsAllowed) requestNotificationAccess()
                            "Habit baru ditambahkan. Selamat memulai!"
                        } catch (error: CancellationException) { throw error }
                        catch (_: Exception) { "Habit belum tersimpan. Silakan coba lagi." }
                        finally { saving = false }
                        snackbar.showSnackbar(message)
                    }
                }
            },
        )
    }
    state.habits.find { it.id == reminderHabitId }?.let { habit ->
        ReminderDialog(
            habit, saving, onDismiss = { if (!saving) reminderHabitId = null },
            onSave = { time ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        val message = try {
                            onUpdateReminder(habit.id, time)
                            reminderHabitId = null
                            if (time != null && !notificationsAllowed) requestNotificationAccess()
                            if (time == null) "Pengingat dimatikan." else "Jam pengingat tersimpan."
                        } catch (error: CancellationException) { throw error }
                        catch (_: Exception) { "Pengingat belum tersimpan. Silakan coba lagi." }
                        finally { saving = false }
                        snackbar.showSnackbar(message)
                    }
                }
            },
        )
    }
    if (showManage) ManageHabitsDialog(state.habits, onDismiss = { showManage = false }, onEdit = { showManage = false; editHabitId = it }, onArchive = { showManage = false; archiveHabitId = it }, saving = saving, onMove = { id, direction -> mutate { onMoveHabit(id, direction); "Urutan habit tersimpan." } })
    state.habits.find { it.id == editHabitId }?.let { habit ->
        CreateHabitDialog(onDismiss = { if (!saving) editHabitId = null }, saving = saving, habit = habit,
            onArchive = { archiveHabitId = habit.id }, onSave = { name, category, target, _, _, weekdays ->
                mutate { onEditHabit(habit.id, name, category, target, weekdays); editHabitId = null; "Habit diperbarui. Target dan jadwal baru mulai besok." }
            })
    }
    state.habits.find { it.id == archiveHabitId }?.let { habit ->
        AlertDialog(onDismissRequest = { if (!saving) archiveHabitId = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(if (habit.isArchived) "Pulihkan habit?" else "Arsipkan habit?") },
            text = { Text(if (habit.isArchived) "Habit kembali aktif mulai besok sesuai jadwalnya. Riwayat dan XP tetap tersimpan." else "Habit berhenti muncul di Hari Ini dan pengingat dihentikan. Riwayat dan XP tetap tersimpan.") },
            confirmButton = { TextButton(enabled = !saving, onClick = {
                mutate { onArchiveHabit(habit.id, !habit.isArchived); archiveHabitId = null; editHabitId = null; if (habit.isArchived) "Habit dipulihkan. Jadwal mulai besok." else "Habit diarsipkan." }
            }) { Text(if (habit.isArchived) "Pulihkan" else "Arsipkan") } },
            dismissButton = { TextButton(onClick = { archiveHabitId = null }, enabled = !saving) { Text("Batal") } })
    }
    state.habits.find { it.id == correctHabitId }?.let { habit ->
        val date = LocalDate.parse(correctDate)
        val record = state.recordsFor(habit).find { it.date == date }
        CorrectProgressDialog(habit, date, record, saving, onDismiss = { if (!saving) correctHabitId = null }, onSave = { amount ->
            mutate {
                val delta = onCorrectProgress(habit.id, date, amount)
                correctHabitId = null
                "Progres dikoreksi." + if (delta == 0) " XP tetap." else " XP disesuaikan ${if (delta > 0) "+" else ""}$delta."
            }
        })
    }
    if (showEditName) EditNameDialog(state.userName, saving, onDismiss = { if (!saving) showEditName = false }, onSave = { name ->
        mutate { onEditName(name); showEditName = false; "Nama profil diperbarui." }
    })
    state.habits.find { it.id == noteHabitId }?.let { habit ->
        val date = LocalDate.parse(noteDate)
        val note = state.notes.find { it.habitId == habit.id && it.date == date }?.body.orEmpty()
        HabitNoteDialog(habit.name, date, note, saving, onDismiss = { if (!saving) noteHabitId = null }, onSave = { body ->
            mutate { onSaveNote(habit.id, date, body); noteHabitId = null; if (body.isBlank()) "Catatan dihapus." else "Catatan tersimpan." }
        })
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HabitQuestPreview() {
    HabitQuestTheme { HabitQuestApp(HabitUiState(), { _, _ -> 0 }, { _, _, _, _, _, _ -> }) }
}
