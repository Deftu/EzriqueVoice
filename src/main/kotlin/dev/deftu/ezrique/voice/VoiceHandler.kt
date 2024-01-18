@file:OptIn(KordVoice::class)

package dev.deftu.ezrique.voice

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.deftu.ezrique.voice.lavaplayer.ByteArrayAudioSourceManager
import dev.deftu.ezrique.voice.lavaplayer.PlayerInstance
import dev.deftu.ezrique.voice.lavaplayer.TrackScheduler
import dev.deftu.ezrique.voice.sql.ChannelLink
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
    val playerManager = DefaultAudioPlayerManager().apply {
        AudioSourceManagers.registerRemoteSources(this)
        AudioSourceManagers.registerLocalSource(this)
        configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH
        registerSourceManager(ByteArrayAudioSourceManager())
    }
    private val guildPlayers = mutableMapOf<Snowflake, PlayerInstance>()

    fun setup() {
        kord.on<ReadyEvent> {
            handleReady(this)
        }

        kord.on<VoiceStateUpdateEvent> {
            handleChannelUpdate(this)
        }
    }

    fun getVoiceConnection(guildId: Snowflake): VoiceConnection? {
        return connections[guildId]
    }

    suspend fun setVoiceChannel(
        guildId: Snowflake,
        channel: VoiceChannel,
        builder: VoiceConnectionBuilder.() -> Unit = {}
    ) {
        val connection = connections[guildId]
        if (connection == null) {
            val playerInstance = setupPlayer(guildId)
            connections[guildId] = channel.connect {
                audioProvider { AudioFrame.fromData(playerInstance.player.provide()?.data) }
                builder()
            }
        } else connection.move(channel.id)
    }

    suspend fun leaveVoiceChannel(guildId: Snowflake): Boolean {
        val connection = connections[guildId] ?: return false
        connection.shutdown()
        connections.remove(guildId)
        return true
    }

    fun getPlayer(guildId: Snowflake): PlayerInstance? {
        return guildPlayers[guildId]
    }

    private fun setupPlayer(guildId: Snowflake): PlayerInstance {
        if (guildPlayers.containsKey(guildId)) return guildPlayers[guildId]!!

        val player = playerManager.createPlayer()
        val scheduler = TrackScheduler(player)
        player.addListener(scheduler)

        val instance = PlayerInstance(player, scheduler)
        guildPlayers[guildId] = instance
        return instance
    }

    private suspend fun handleReady(event: ReadyEvent) {
        /**
         * Handles an edge-case where the bot's user is still in a voice channel despite having gone offline at some point
         */
        event.getGuilds().collect { guild ->
            val channel = guild.getMember(kord.selfId).getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel ?: return@collect
            setVoiceChannel(guild.id, channel)
        }
    }

    private suspend fun handleChannelUpdate(event: VoiceStateUpdateEvent) {
        val guild = event.state.getGuildOrNull() ?: return
        val ownChannel = guild.getMember(kord.selfId).getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel ?: return

        if (
            ownChannel.voiceStates.count() == 1 &&
            ownChannel.voiceStates.first().userId == kord.selfId
        ) leaveVoiceChannel(guild.id)
    }
}
