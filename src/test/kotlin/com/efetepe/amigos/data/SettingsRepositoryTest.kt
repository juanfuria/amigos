package com.efetepe.amigos.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private val db = DatabaseFactory.createInMemory()
    private val repo = SettingsRepository(db)

    @Test
    fun `get returns default when key not set`() {
        assertEquals(3, repo.nudgesPerWeek)
    }

    @Test
    fun `set and get nudgesPerWeek`() {
        repo.nudgesPerWeek = 5
        assertEquals(5, repo.nudgesPerWeek)
    }

    @Test
    fun `set and get quietHours`() {
        repo.quietHoursStart = "10:00"
        repo.quietHoursEnd = "22:00"
        assertEquals("10:00", repo.quietHoursStart)
        assertEquals("22:00", repo.quietHoursEnd)
    }

    @Test
    fun `set and get notificationDays`() {
        repo.notificationDays = listOf("MON", "TUE", "THU")
        assertEquals(listOf("MON", "TUE", "THU"), repo.notificationDays)
    }
}
