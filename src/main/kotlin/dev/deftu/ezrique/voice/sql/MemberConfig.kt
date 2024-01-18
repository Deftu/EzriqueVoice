package dev.deftu.ezrique.voice.sql

import dev.deftu.ezrique.voice.tts.Voice
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object MemberConfigTable : LongIdTable("member_config") {
    val enabled = bool("enabled").default(true)
    val voice = varchar("voice", 70).default(Voice.DEFAULT.code)
}

class MemberConfig(
    id: EntityID<Long>
) : LongEntity(id) {
    companion object : LongEntityClass<MemberConfig>(MemberConfigTable) {
        suspend fun isEnabled(memberId: Long): Boolean {
            return newSuspendedTransaction {
                findById(memberId)?.enabled ?: true
            }
        }

        suspend fun setEnabled(memberId: Long, enabled: Boolean) {
            newSuspendedTransaction {
                val config = findById(memberId) ?: new(memberId) {}
                config.enabled = enabled
            }
        }

        suspend fun getVoice(memberId: Long): Voice {
            return newSuspendedTransaction {
                findById(memberId)?.voice?.let {
                    Voice.fromCode(it)
                }
            } ?: Voice.DEFAULT
        }

        suspend fun setVoice(memberId: Long, voice: Voice) {
            newSuspendedTransaction {
                val config = findById(memberId) ?: new(memberId) {}
                config.voice = voice.code
            }
        }
    }

    var enabled by MemberConfigTable.enabled
    var voice by MemberConfigTable.voice
}
