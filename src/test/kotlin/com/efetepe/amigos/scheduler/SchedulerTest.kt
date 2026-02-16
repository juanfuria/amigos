package com.efetepe.amigos.scheduler

import com.efetepe.amigos.data.DatabaseFactory
import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository
import com.efetepe.amigos.data.models.ChannelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SchedulerTest {
    private val db = DatabaseFactory.createInMemory()
    private val friendRepo = FriendRepository(db)
    private val settingsRepo = SettingsRepository(db)

    @Test
    fun `buildCandidates returns all friends with their staleness data`() {
        friendRepo.addFriend("Alice", null, 14)
        friendRepo.addFriend("Bob", null, 30)
        val alice = friendRepo.getAllFriends().first { it.name == "Alice" }
        friendRepo.addChannel(alice.id, ChannelType.WHATSAPP, "+1234", preferred = true)

        val candidates = Scheduler.buildCandidates(friendRepo)
        assertEquals(2, candidates.size)
    }

    @Test
    fun `getPreferredChannel returns preferred channel`() {
        friendRepo.addFriend("Alice", null, 14)
        val alice = friendRepo.getAllFriends().first()
        friendRepo.addChannel(alice.id, ChannelType.EMAIL, "a@b.com", preferred = false)
        friendRepo.addChannel(alice.id, ChannelType.WHATSAPP, "+1234", preferred = true)

        val channel = Scheduler.getPreferredChannel(friendRepo, alice.id)
        assertNotNull(channel)
        assertEquals("WHATSAPP", channel.channel_type)
    }
}
