package com.efetepe.amigos.scheduler

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class StalenessScorerTest {

    @Test
    fun `never contacted friend has max staleness`() {
        val score = StalenessScorer.score(
            lastContacted = null,
            desiredFrequencyDays = 30,
            now = Clock.System.now()
        )
        assertEquals(Double.MAX_VALUE, score)
    }

    @Test
    fun `friend contacted today has zero staleness`() {
        val now = Clock.System.now()
        val score = StalenessScorer.score(
            lastContacted = now,
            desiredFrequencyDays = 30,
            now = now
        )
        assertEquals(0.0, score, 0.01)
    }

    @Test
    fun `friend contacted 15 days ago with 30-day frequency is 0_5 stale`() {
        val now = Clock.System.now()
        val score = StalenessScorer.score(
            lastContacted = now - 15.days,
            desiredFrequencyDays = 30,
            now = now
        )
        assertEquals(0.5, score, 0.01)
    }

    @Test
    fun `friend contacted 30 days ago with 30-day frequency is 1_0 stale`() {
        val now = Clock.System.now()
        val score = StalenessScorer.score(
            lastContacted = now - 30.days,
            desiredFrequencyDays = 30,
            now = now
        )
        assertEquals(1.0, score, 0.01)
    }

    @Test
    fun `pickFriend returns null when no friends are due`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = null, now = now)
        assertNull(picked)
    }

    @Test
    fun `pickFriend returns overdue friend`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now - 60.days, desiredFrequencyDays = 30),
            FriendCandidate(id = 2, name = "Bob", lastContacted = now, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = null, now = now)
        assertNotNull(picked)
        assertEquals(1L, picked.id)
    }

    @Test
    fun `pickFriend avoids last picked friend when alternatives exist`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now - 60.days, desiredFrequencyDays = 30),
            FriendCandidate(id = 2, name = "Bob", lastContacted = now - 45.days, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = 1L, now = now)
        assertNotNull(picked)
        assertEquals(2L, picked.id)
    }

    @Test
    fun `pickFriend picks last picked if they are the only one due`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now - 60.days, desiredFrequencyDays = 30),
            FriendCandidate(id = 2, name = "Bob", lastContacted = now, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = 1L, now = now)
        assertNotNull(picked)
        assertEquals(1L, picked.id)
    }
}
