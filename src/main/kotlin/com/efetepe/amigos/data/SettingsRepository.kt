package com.efetepe.amigos.data

class SettingsRepository(private val db: AmigosDatabase) {

    private fun get(key: String, default: String): String =
        db.appSettingsQueries.selectByKey(key).executeAsOneOrNull() ?: default

    private fun set(key: String, value: String) {
        db.appSettingsQueries.upsert(key, value)
    }

    var nudgesPerWeek: Int
        get() = get("nudges_per_week", "3").toInt()
        set(value) = set("nudges_per_week", value.toString())

    var activeHoursStart: String
        get() = get("active_hours_start", "09:00")
        set(value) = set("active_hours_start", value)

    var activeHoursEnd: String
        get() = get("active_hours_end", "21:00")
        set(value) = set("active_hours_end", value)

    var notificationDays: List<String>
        get() = get("notification_days", "MON,WED,FRI").split(",")
        set(value) = set("notification_days", value.joinToString(","))
}
