package com.efetepe.amigos.data

import com.efetepe.amigos.data.models.ChannelType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FriendRepository(private val db: AmigosDatabase) {

    fun addFriend(name: String, notes: String?, desiredFrequencyDays: Long) {
        db.friendQueries.insert(name, notes, desiredFrequencyDays)
    }

    fun getAllFriends() = db.friendQueries.selectAll().executeAsList()

    fun getFriend(id: Long) = db.friendQueries.selectById(id).executeAsOneOrNull()

    fun updateFriend(id: Long, name: String, notes: String?, desiredFrequencyDays: Long) {
        db.friendQueries.update(name, notes, desiredFrequencyDays, id)
    }

    fun deleteFriend(id: Long) {
        db.contactChannelQueries.deleteByFriendId(id)
        db.friendQueries.deleteById(id)
    }

    fun deleteChannelsByFriendId(friendId: Long) {
        db.contactChannelQueries.deleteByFriendId(friendId)
    }

    fun addChannel(friendId: Long, type: ChannelType, address: String, preferred: Boolean) {
        db.contactChannelQueries.insert(friendId, type.name, address, if (preferred) 1L else 0L)
    }

    fun getChannels(friendId: Long) =
        db.contactChannelQueries.selectByFriendId(friendId).executeAsList()

    fun logPrompt(friendId: Long, channelType: ChannelType): Long {
        val now = Clock.System.now().toString()
        db.contactLogQueries.insert(friendId, channelType.name, now, 0L)
        val logs = db.contactLogQueries.selectByFriendId(friendId).executeAsList()
        return logs.first().id
    }

    fun markContacted(logId: Long) {
        val now = Clock.System.now().toString()
        db.contactLogQueries.markContacted(now, logId)
    }

    fun markSkipped(logId: Long) {
        db.contactLogQueries.markSkipped(logId)
    }

    fun getLastContactDate(friendId: Long): Instant? {
        val result = db.contactLogQueries.lastContactForFriend(friendId).executeAsOneOrNull()
        return result?.last_contacted?.let { Instant.parse(it) }
    }

    fun getContactLog(friendId: Long) =
        db.contactLogQueries.selectByFriendId(friendId).executeAsList()

    fun getAllContactLogs() =
        db.contactLogQueries.selectAll().executeAsList()
}
