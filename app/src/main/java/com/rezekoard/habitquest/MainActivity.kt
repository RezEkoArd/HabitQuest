package com.rezekoard.habitquest

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rezekoard.habitquest.ui.HabitQuestApp
import com.rezekoard.habitquest.ui.HabitQuestTheme
import com.rezekoard.habitquest.ui.HabitViewModel
import com.rezekoard.habitquest.ui.StartupScreen
import com.rezekoard.habitquest.ui.WelcomeScreen
import com.rezekoard.habitquest.notifications.ACTION_OPEN_REMINDER
import com.rezekoard.habitquest.notifications.EXTRA_HABIT_ID
import com.rezekoard.habitquest.notifications.EXTRA_DATE
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val requestedHabit = MutableStateFlow<Long?>(null)

    private fun handleReminder(incoming: Intent) {
        if (incoming.action != ACTION_OPEN_REMINDER) return
        val id = incoming.getLongExtra(EXTRA_HABIT_ID, -1)
        val date = incoming.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        if (id > 0 && date == LocalDate.now().toEpochDay()) requestedHabit.value = id
        incoming.action = Intent.ACTION_MAIN
        incoming.removeExtra(EXTRA_HABIT_ID)
        incoming.removeExtra(EXTRA_DATE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminder(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleReminder(intent)
        enableEdgeToEdge()
        setContent {
            HabitQuestTheme {
                val model: HabitViewModel = viewModel()
                val state by model.state.collectAsStateWithLifecycle()
                val target by requestedHabit.collectAsStateWithLifecycle()
                when {
                    state.isLoading -> StartupScreen()
                    state.storageError != null -> StartupScreen(state.storageError, model::reload)
                    state.userName.isBlank() -> WelcomeScreen(model::completeOnboarding)
                    else -> HabitQuestApp(
                        state, model::advance, model::addHabit,
                        onUpdateReminder = model::updateReminder, onSyncReminders = model::syncReminders,
                        requestedHabitId = target, onReminderHandled = { requestedHabit.value = null },
                    )
                }
            }
        }
    }
}
