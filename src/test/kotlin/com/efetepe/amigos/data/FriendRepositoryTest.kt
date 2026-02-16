package com.efetepe.amigos.data

import com.efetepe.amigos.data.models.ChannelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FriendRepositoryTest {
    private val db = DatabaseFactory.createInMemory()
    private val repo = FriendRepository(db)

    @Test
    fun `addFriend and getAllFriends returns the friend`() {
        repo.addFriend("Alice", "College friend", 14)
        val friends = repo.getAllFriends()
        assertEquals(1, friends.size)
        assertEquals("Alice", friends[0].name)
        assertEquals(14, friends[0].desired_frequency_days)
    }

    @Test
    fun `addChannel and getChannels returns channels for friend`() {
        repo.addFriend("Bob", null, 30)
        val bob = repo.getAllFriends().first()
        repo.addChannel(bob.id, ChannelType.WHATSAPP, "+1234567890", preferred = true)
        repo.addChannel(bob.id, ChannelType.EMAIL, "bob@example.com", preferred = false)

        val channels = repo.getChannels(bob.id)
        assertEquals(2, channels.size)
        assertEquals("WHATSAPP", channels[0].channel_type) // preferred first due to ORDER BY
    }

    @Test
    fun `logPrompt and markContacted records contact`() {
        repo.addFriend("Carol", null, 7)
        val carol = repo.getAllFriends().first()
        val logId = repo.logPrompt(carol.id, ChannelType.IMESSAGE)

        assertNotNull(logId)

        repo.markContacted(logId)
        val lastContact = repo.getLastContactDate(carol.id)
        assertNotNull(lastContact)
    }

    @Test
    fun `getLastContactDate returns null for never-contacted friend`() {
        repo.addFriend("Dave", null, 30)
        val dave = repo.getAllFriends().first()
        val lastContact = repo.getLastContactDate(dave.id)
        assertEquals(null, lastContact)
    }

    @Test
    fun `deleteFriend removes friend and cascaded data`() {
        repo.addFriend("Eve", null, 30)
        val eve = repo.getAllFriends().first()
        repo.addChannel(eve.id, ChannelType.SMS, "+999", preferred = true)
        repo.deleteFriend(eve.id)

        assertTrue(repo.getAllFriends().isEmpty())
    }
}
