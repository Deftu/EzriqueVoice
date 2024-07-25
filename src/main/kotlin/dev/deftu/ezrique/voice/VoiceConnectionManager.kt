@file:OptIn(KordVoice::class)

package dev.deftu.ezrique.voice

import dev.deftu.ezrique.events
import dev.deftu.ezrique.voice.audio.AudioOutputManager
import dev.kord.core.Kord
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.events.VoiceChannelLeaveEvent
import dev.kord.common.annotation.KordPreview
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import dev.kord.voice.AudioFrame
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first

object VoiceConnectionManager {

    /**
     * Stores all open voice connections by their guild ID.
     */
    private val connections = mutableMapOf<Snowflake, VoiceConnection>()

    fun setup(kord: Kord) {
        /**
         * Handles an edge-case where the bot's user is still in a voice channel despite having gone offline at some point
         */
        kord.on<ReadyEvent> {
            getGuilds().collect { guild ->
                val channel = guild.getMember(kord.selfId).getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel ?: return@collect
                connectTo(kord, shard, guild.id, channel)
            }
        }

        /**
         * Makes the bot leave the voice channel if everyone else has left
         */
        kord.on<VoiceStateUpdateEvent> {
            val guild = state.getGuildOrNull() ?: return@on
            val ownChannel = guild.getMember(kord.selfId).getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel ?: return@on

            if (
                ownChannel.voiceStates.count() == 1 &&
                ownChannel.voiceStates.first().userId == kord.selfId
            ) {
                leave(kord, shard, guild.id)
            }
        }
    }

    fun isConnected(guildId: Snowflake): Boolean {
        return connections.containsKey(guildId)
    }

    fun getConnection(guildId: Snowflake): VoiceConnection? {
        return connections[guildId]
    }

    @OptIn(KordPreview::class)
    suspend fun connectTo(
        kord: Kord,
        shardId: Int = -1,
        guildId: Snowflake,
        channel: VoiceChannel,
    ) {
        val connection = connections[guildId]
        if (connection == null) {
            events.emit(VoiceChannelJoinEvent(kord, null, shardId, guildId))
            connections[guildId] = channel.connect {
                selfDeaf = true
                audioProvider {
                    AudioFrame.fromData(AudioOutputManager.createOutputFor(guildId))
                }
            }
        } else {
            connection.move(channel.id)
        }
    }

    @OptIn(KordPreview::class)
    suspend fun leave(
        kord: Kord,
        shardId: Int = -1,
        guildId: Snowflake
    ): Boolean {
        val connection = connections[guildId] ?: return false
        connection.shutdown()
        connections.remove(guildId)
        events.emit(VoiceChannelLeaveEvent(kord, null, shardId, guildId))
        return true
    }

//    private fun createAudioOutput(guildId: Snowflake): ByteArray? {
//        val players = guildPlayers[guildId] ?: return null
//        val frames = players.mapNotNull { it.player.provide() }
//        if (frames.isEmpty()) {
//            return null
//        }
//
//        val mixer = PcmAudioMixer()
//        frames.map { frame -> frame.data }.forEach(mixer::addFrame)
//        return mixer.mix { input, output ->
//            opusEncoder.encode(input, output)
//        }
//    }

}
