package com.rezekoard.habitquest.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitTest {
    @Test fun partialProgressDoesNotAwardXp() {
        val result = advanceHabit(Habit(1, "Baca", "Belajar", 15, "halaman"), 5)
        assertEquals(5, result.habit.progress)
        assertEquals(0, result.earnedXp)
    }

    @Test fun completingHabitAwardsXpOnlyOnce() {
        val completed = advanceHabit(Habit(1, "Baca", "Belajar", 15, "halaman"), 20)
        assertEquals(15, completed.habit.progress)
        assertEquals(20, completed.earnedXp)
        assertEquals(0, advanceHabit(completed.habit).earnedXp)
    }
}
