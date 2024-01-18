package dev.deftu.ezrique.voice.sql

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object ChannelLinkTable : LongIdTable("channel_link") {
    val textChannelId = long("text_channel_id")
    val voiceChannelId = long("voice_channel_id")
}

class ChannelLink(
    id: EntityID<Long>
) : LongEntity(id) {
    companion object : LongEntityClass<ChannelLink>(ChannelLinkTable) {
        suspend fun isPresent(
            textChannelId: Long,
            voiceChannelId: Long
        ): Boolean {
            return newSuspendedTransaction {
                find {
                    (ChannelLinkTable.textChannelId eq textChannelId) and
                    (ChannelLinkTable.voiceChannelId eq voiceChannelId)
                }.firstOrNull() != null
            }
        }

        suspend fun setupLink(
            textChannelId: Long,
            voiceChannelId: Long
        ) {
            newSuspendedTransaction {
                val link = find {
                    (ChannelLinkTable.textChannelId eq textChannelId) or
                    (ChannelLinkTable.voiceChannelId eq voiceChannelId)
                }.firstOrNull()

                if (link == null) {
                    ChannelLink.new {
                        this.textChannelId = textChannelId
                        this.voiceChannelId = voiceChannelId
                    }
                } else {
                    link.textChannelId = textChannelId
                    link.voiceChannelId = voiceChannelId
                }
            }
        }
    }

    var textChannelId by ChannelLinkTable.textChannelId
    var voiceChannelId by ChannelLinkTable.voiceChannelId
}
