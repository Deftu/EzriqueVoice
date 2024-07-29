package dev.deftu.ezrique.voice.sql

import dev.deftu.ezrique.voice.tts.Voice
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object MemberConfigTable : LongIdTable("member_config") {

    val tts = bool("tts").default(true)
    val voice = varchar("voice", 70).default(Voice.DEFAULT.code)

}

class MemberConfig(
    id: EntityID<Long>
) : LongEntity(id) {

    companion object : LongEntityClass<MemberConfig>(MemberConfigTable) {

        suspend fun isTtsEnabled(memberId: Long): Boolean {
            return newSuspendedTransaction {
                findById(memberId)?.tts ?: true
            }
        }

        suspend fun setTtsEnabled(memberId: Long, enabled: Boolean): Boolean {
            return newSuspendedTransaction {
                val config = findById(memberId) ?: new(memberId) {}
                config.tts = enabled

                config.tts
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

    var tts by MemberConfigTable.tts
    var voice by MemberConfigTable.voice

}
