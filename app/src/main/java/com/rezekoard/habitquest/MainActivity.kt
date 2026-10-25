package com.rezekoard.habitquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rezekoard.habitquest.ui.HabitQuestApp
import com.rezekoard.habitquest.ui.HabitQuestTheme
import com.rezekoard.habitquest.ui.HabitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitQuestTheme {
                val model: HabitViewModel = viewModel()
                val state by model.state.collectAsStateWithLifecycle()
                HabitQuestApp(state, model::advance, model::addHabit)
            }
        }
    }
}
