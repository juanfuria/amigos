package com.efetepe.amigos.scheduler

import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

data class FriendCandidate(
    val id: Long,
    val name: String,
    val lastContacted: Instant?,
    val desiredFrequencyDays: Long
)

object StalenessScorer {
    private const val THRESHOLD = 0.8

    fun score(lastContacted: Instant?, desiredFrequencyDays: Long, now: Instant): Double {
        if (lastContacted == null) return Double.MAX_VALUE
        val daysSince = (now - lastContacted).inWholeDays.toDouble()
        return daysSince / desiredFrequencyDays.toDouble()
    }

    fun pickFriend(
        candidates: List<FriendCandidate>,
        lastPickedId: Long?,
        now: Instant,
        random: Random = Random
    ): FriendCandidate? {
        val scored = candidates.map { it to score(it.lastContacted, it.desiredFrequencyDays, now) }
        val due = scored.filter { it.second >= THRESHOLD }

        if (due.isEmpty()) return null

        // Try to avoid picking the same friend consecutively
        val filtered = due.filter { it.first.id != lastPickedId }
        val pool = filtered.ifEmpty { due }

        // Weighted random selection by staleness score
        val totalWeight = pool.sumOf { it.second }
        var roll = random.nextDouble() * totalWeight
        for ((candidate, weight) in pool) {
            roll -= weight
            if (roll <= 0) return candidate
        }
        return pool.last().first
    }
}
