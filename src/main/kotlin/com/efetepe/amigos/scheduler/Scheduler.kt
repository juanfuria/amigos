package com.efetepe.amigos.scheduler

import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository
import com.efetepe.amigos.data.models.ChannelType
import com.efetepe.amigos.notification.MacNotifier
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

data class NudgeResult(
    val friendId: Long,
    val friendName: String,
    val channelType: ChannelType,
    val address: String,
    val logId: Long
)

class Scheduler(
    private val friendRepo: FriendRepository,
    private val settingsRepo: SettingsRepository,
    private val onNudge: (NudgeResult) -> Unit
) {
    private var lastPickedId: Long? = null
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (isActive) {
                checkAndNudge()
                delay(15.minutes)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    internal fun checkAndNudge() {
        val now = Clock.System.now()
        if (!isWithinSchedule(now)) return

        val candidates = buildCandidates(friendRepo)
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId, now) ?: return
        val channel = getPreferredChannel(friendRepo, picked.id) ?: return
        val channelType = ChannelType.valueOf(channel.channel_type)

        val logId = friendRepo.logPrompt(picked.id, channelType)
        lastPickedId = picked.id

        MacNotifier.notify(
            title = "Amigos",
            message = "Time to reach out to ${picked.name} via ${channelType.displayName}"
        )

        onNudge(NudgeResult(picked.id, picked.name, channelType, channel.address, logId))
    }

    private fun isWithinSchedule(now: Instant): Boolean {
        val tz = TimeZone.currentSystemDefault()
        val localNow = now.toLocalDateTime(tz)
        val dayName = localNow.dayOfWeek.name.take(3) // MON, TUE, etc.

        if (dayName !in settingsRepo.notificationDays) return false

        val startTime = LocalTime.parse(settingsRepo.activeHoursStart)
        val endTime = LocalTime.parse(settingsRepo.activeHoursEnd)
        return localNow.time in startTime..endTime
    }

    companion object {
        fun buildCandidates(friendRepo: FriendRepository): List<FriendCandidate> {
            return friendRepo.getAllFriends().map { friend ->
                FriendCandidate(
                    id = friend.id,
                    name = friend.name,
                    lastContacted = friendRepo.getLastContactDate(friend.id),
                    desiredFrequencyDays = friend.desired_frequency_days
                )
            }
        }

        fun getPreferredChannel(friendRepo: FriendRepository, friendId: Long) =
            friendRepo.getChannels(friendId).firstOrNull()
    }
}
