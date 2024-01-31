@file:OptIn(KordVoice::class)

package dev.deftu.ezrique.voice

import dev.deftu.ezrique.voice.audio.GuildPlayer
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import dev.kord.voice.AudioFrame
import dev.kord.voice.VoiceConnection
import dev.kord.voice.VoiceConnectionBuilder
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first

object VoiceHandler {
    private val connections = mutableMapOf<Snowflake, VoiceConnection>()
    private val guildPlayers = mutableMapOf<Snowflake, List<GuildPlayer<*>>>()

    fun setup() {
        kord.on<ReadyEvent> {
            /**
             * Handles an edge-case where the bot's user is still in a voice channel despite having gone offline at some point
             */
            getGuilds().collect { guild ->
                val channel = guild.getMember(kord.selfId).getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel ?: return@collect
                setVoiceChannel(guild.id, channel)
            }
        }

        kord.on<VoiceStateUpdateEvent> {
            val guild = state.getGuildOrNull() ?: return@on
            val ownChannel = guild.getMember(kord.selfId).getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel ?: return@on

            if (
                ownChannel.voiceStates.count() == 1 &&
                ownChannel.voiceStates.first().userId == kord.selfId
            ) leaveVoiceChannel(guild.id)
        }
    }

    fun getVoiceConnection(guildId: Snowflake): VoiceConnection? {
        return connections[guildId]
    }

    fun isVoiceConnected(guildId: Snowflake): Boolean {
        return connections.containsKey(guildId)
    }

    suspend fun setVoiceChannel(
        guildId: Snowflake,
        channel: VoiceChannel
    ) {
        val connection = connections[guildId]
        if (connection == null) {
            eventBus.post(VoiceChannelJoinEvent(guildId, channel.id))
            connections[guildId] = channel.connect {
                selfDeaf = true
                audioProvider { AudioFrame.fromData(createAudioOutput(guildId)) }
            }
        } else connection.move(channel.id)
    }

    suspend fun leaveVoiceChannel(guildId: Snowflake): Boolean {
        val connection = connections[guildId] ?: return false
        connection.shutdown()
        connections.remove(guildId)
        return true
    }

    fun registerPlayer(guildId: Snowflake, player: GuildPlayer<*>) {
        val players = guildPlayers[guildId]?.toMutableList() ?: mutableListOf()
        players.add(player)
        guildPlayers[guildId] = players
    }

    private fun createAudioOutput(guildId: Snowflake): ByteArray? {
        val players = guildPlayers[guildId] ?: return null
        val pcmFrames = players.mapNotNull { it.player.provide() }
        if (pcmFrames.isEmpty()) return null
        if (pcmFrames.size > 15) throw IllegalStateException("Too many players registered for guild $guildId")

        fun mix(
            frame1: ByteArray,
            frame2: ByteArray,
        ): ByteArray {
            val maxLength = maxOf(frame1.size, frame2.size)
            val mixedFrame = ByteArray(maxLength)

            for (i in 0 until maxLength - 1 step 2) { // Ensure not to exceed the array length
                val sample1 = if (i + 1 < frame1.size) {
                    ((frame1[i + 1].toInt() shl 8) or (frame1[i].toInt() and 0xFF))
                } else 0

                val sample2 = if (i + 1 < frame2.size) {
                    ((frame2[i + 1].toInt() shl 8) or (frame2[i].toInt() and 0xFF))
                } else 0

                val mixedSample = sample1 + sample2
                // Normalize and prevent clipping
                val normalizedSample = mixedSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                mixedFrame[i] = (normalizedSample and 0xFF).toByte()
                if (i + 1 < mixedFrame.size) {
                    mixedFrame[i + 1] = (normalizedSample shr 8).toByte()
                }
            }

            return mixedFrame
        }

        return pcmFrames.map { it.data }.reduce { frame1, frame2 -> mix(frame1, frame2) }
    }
}
