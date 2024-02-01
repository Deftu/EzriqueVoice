@file:OptIn(KordVoice::class)

package dev.deftu.ezrique.voice

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.format.transcoder.OpusChunkEncoder
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
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
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

object VoiceHandler {
    private val connections = mutableMapOf<Snowflake, VoiceConnection>()
    private val guildPlayers = mutableMapOf<Snowflake, List<GuildPlayer<*>>>()
    private val opusEncoder = StandardAudioDataFormats.DISCORD_OPUS.createEncoder(AudioConfiguration())

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

        val player = guildPlayers[guildId]
        if (player != null) {
            player.forEach { player ->
                player.player.stopTrack()
                player.player.destroy()
            }

            guildPlayers.remove(guildId)
        }

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
        val byteOrder = ByteOrder.nativeOrder()

        val size = pcmFrames.sumOf { it.data.size }
        val output = ByteBuffer.allocateDirect(size)
        if (pcmFrames.size > 1) {
            // There are multiple frames, so we need to mix them together, then encode the resulting PCM
            val frameSize = pcmFrames.maxOf { it.data.size / 2 }
            val shortBuffer = ByteBuffer.allocateDirect(frameSize * 2).order(byteOrder).asShortBuffer()

            pcmFrames.forEach { frame ->
                val buffer = ByteBuffer.wrap(frame.data).asShortBuffer()
                val size = frame.data.size
                for (i in 0 until frameSize) {
                    val sample = if (i < size / 2) buffer[i] else 0

                    var value = 0
                    if (shortBuffer.capacity() > i) value += shortBuffer.get(i)
                    value += sample

                    shortBuffer.put(
                        i,
                        value.toShort()
                    )
                }
            }

            shortBuffer.flip()
            opusEncoder.encode(shortBuffer, output)
        } else if (pcmFrames.isNotEmpty()) {
            // There is only one frame, so we can just encode it directly
            val pcmFrame = pcmFrames.first()
            val shortBuffer = ByteBuffer.allocateDirect(pcmFrame.data.size).order(byteOrder).asShortBuffer()
            shortBuffer.put(ByteBuffer.wrap(pcmFrame.data).asShortBuffer())
            shortBuffer.flip()
            opusEncoder.encode(shortBuffer, output)
        }

        val data = ByteArray(output.remaining())
        output.get(data)
        return data
    }
}
