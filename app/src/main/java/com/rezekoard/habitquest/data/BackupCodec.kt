package com.rezekoard.habitquest.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(val name: String, val habits: Int, val archived: Int, val records: Int)

object BackupCodec {
    const val MAX_BYTES = 10 * 1024 * 1024
    private fun objects(array: JSONArray, max: Int): List<JSONObject> {
        require(array.length() <= max) { "Isi backup terlalu besar." }
        return List(array.length()) { array.getJSONObject(it) }
    }

    fun encode(data: StoredHabitData): String {
        validate(data)
        val json = JSONObject().put("app", "HabitQuest").put("formatVersion", 2).put("createdAt", Instant.now().toString())
            .put("profileName", checkNotNull(data.profile).displayName)
            .put("habits", JSONArray(data.habits.map { h -> JSONObject().put("id", h.id).put("name", h.name).put("category", h.category)
                .put("target", h.target).put("unit", h.unit).put("reminderTime", h.reminderTime ?: JSONObject.NULL)
                .put("startedOn", h.startedOn).put("archived", h.archived).put("weekdays", h.weekdays).put("sortOrder", h.sortOrder) }))
            .put("plans", JSONArray(data.plans.map { p -> JSONObject().put("habitId", p.habitId).put("effectiveFrom", p.effectiveFrom)
                .put("target", p.target).put("unit", p.unit).put("weekdays", p.weekdays) }))
            .put("progress", JSONArray(data.progress.map { p -> JSONObject().put("habitId", p.habitId).put("date", p.date)
                .put("target", p.target).put("amount", p.amount).put("xpEarned", p.xpEarned) }))
            .put("deliveries", JSONArray(data.deliveries.map { JSONObject().put("habitId", it.habitId).put("date", it.date) }))
            .put("notes", JSONArray(data.notes.map { JSONObject().put("habitId", it.habitId).put("date", it.date).put("body", it.body) }))
        return json.toString(2).also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup melebihi 10 MB." } }
    }

    fun decode(text: String): StoredHabitData {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup melebihi 10 MB." }
        val json = JSONObject(text)
        val version = json.getInt("formatVersion")
        require(json.getString("app") == "HabitQuest" && version in 1..2) { "Format backup tidak didukung." }
        Instant.parse(json.getString("createdAt"))
        val habits = objects(json.getJSONArray("habits"), 300).map { h ->
            require(h.has("reminderTime"))
            HabitEntity(h.getLong("id"), h.getString("name"), h.getString("category"), h.getInt("target"), h.getString("unit"),
                if (h.isNull("reminderTime")) null else h.getString("reminderTime"), h.getLong("startedOn"), h.getBoolean("archived"), h.getInt("weekdays"), if (version >= 2) h.getInt("sortOrder") else 0)
        }
        val plans = objects(json.getJSONArray("plans"), 10_000).map { HabitPlanEntity(it.getLong("habitId"), it.getLong("effectiveFrom"), it.getInt("target"), it.getString("unit"), it.getInt("weekdays")) }
        val progress = objects(json.getJSONArray("progress"), 100_000).map { DailyProgressEntity(it.getLong("habitId"), it.getLong("date"), it.getInt("target"), it.getInt("amount"), it.getInt("xpEarned")) }
        val deliveries = objects(json.getJSONArray("deliveries"), 100_000).map { ReminderDeliveryEntity(it.getLong("habitId"), it.getLong("date")) }
        val notes = if (version >= 2) objects(json.getJSONArray("notes"), 100_000).map { HabitNoteEntity(it.getLong("habitId"), it.getLong("date"), it.getString("body")) } else emptyList()
        return StoredHabitData(UserProfileEntity(displayName = json.getString("profileName")), habits, progress, plans, deliveries, notes).also(::validate)
    }

    fun validate(data: StoredHabitData) {
        val profile = requireNotNull(data.profile) { "Backup tidak memiliki profil." }
        require(profile.id == 1 && profile.displayName.isNotBlank() && profile.displayName.length <= 30)
        require(data.habits.size <= 300 && data.plans.size <= 10_000 && data.progress.size <= 100_000 && data.deliveries.size <= 100_000 && data.notes.size <= 100_000)
        val habits = data.habits.associateBy { it.id }
        require(habits.size == data.habits.size) { "ID habit ganda." }
        val minimumDate = LocalDate.of(1970, 1, 1).toEpochDay()
        val maximumDate = LocalDate.now().plusYears(1).toEpochDay()
        fun dateValid(date: Long) { require(date in minimumDate..maximumDate) { "Tanggal backup tidak valid." } }
        data.habits.forEach { h ->
            require(h.id in 1..Int.MAX_VALUE.toLong() && h.name.isNotBlank() && h.name.length <= 60 && h.unit.isNotBlank() && h.unit.length <= 20)
            require(h.category in listOf("Olahraga", "Belajar", "Kreativitas", "Personal") && h.target in 1..10_000 && h.weekdays in 1..127)
            require(h.sortOrder in 0..1_000_000)
            dateValid(h.startedOn)
            h.reminderTime?.let { require(LocalTime.parse(it).second == 0 && LocalTime.parse(it).nano == 0) }
        }
        require(data.plans.distinctBy { it.habitId to it.effectiveFrom }.size == data.plans.size) { "Versi jadwal ganda." }
        val plans = data.plans.groupBy { it.habitId }.mapValues { (_, list) -> list.sortedBy { it.effectiveFrom } }
        data.plans.forEach { p ->
            val h = requireNotNull(habits[p.habitId])
            dateValid(p.effectiveFrom)
            require(p.effectiveFrom >= h.startedOn && p.target in 1..10_000 && p.unit == h.unit && p.weekdays in 0..127)
        }
        data.habits.forEach { h -> require(plans[h.id]?.firstOrNull()?.effectiveFrom == h.startedOn) { "Jadwal awal habit hilang." } }
        require(data.progress.distinctBy { it.habitId to it.date }.size == data.progress.size) { "Progres harian ganda." }
        data.progress.forEach { p ->
            val h = requireNotNull(habits[p.habitId])
            dateValid(p.date)
            require(p.date >= h.startedOn && p.target in 1..10_000 && p.amount in 0..p.target)
            require(p.xpEarned == if (p.amount == p.target) 20 else 0) { "XP backup tidak konsisten." }
        }
        require(data.deliveries.distinctBy { it.habitId to it.date }.size == data.deliveries.size)
        data.deliveries.forEach { d -> require(d.date >= requireNotNull(habits[d.habitId]).startedOn); dateValid(d.date) }
        require(data.notes.distinctBy { it.habitId to it.date }.size == data.notes.size)
        data.notes.forEach { n ->
            require(n.date >= requireNotNull(habits[n.habitId]).startedOn && n.body.isNotBlank() && n.body.length <= 2_000)
            dateValid(n.date)
        }
    }

    fun summary(data: StoredHabitData) = BackupSummary(checkNotNull(data.profile).displayName, data.habits.size, data.habits.count { it.archived }, data.progress.size)
}
