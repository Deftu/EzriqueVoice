package dev.deftu.ezrique.voice.utils

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.deftu.ezrique.voice.audio.AudioOutputManager
import dev.deftu.ezrique.voice.audio.GuildAudioManager
import dev.deftu.ezrique.voice.audio.scheduler.TrackScheduler
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.events.VoiceChannelLeaveEvent
import dev.kord.common.entity.Snowflake
import java.nio.ByteOrder

fun DefaultAudioPlayerManager.configure() {
    configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH
    configuration.outputFormat = when (ByteOrder.nativeOrder()) {
        ByteOrder.BIG_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_LE
        ByteOrder.LITTLE_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_BE
        else -> throw IllegalStateException("Unknown byte order: ${ByteOrder.nativeOrder()}")
    }
}

fun <T : TrackScheduler<*>> VoiceChannelJoinEvent.handleJoin(
    playerManager: DefaultAudioPlayerManager,
    guildPlayers: MutableMap<Snowflake, GuildAudioManager<T>>,
    scheduler: (audioPlayer: AudioPlayer) -> T
) {
    val audioPlayer = playerManager.createPlayer()
    val trackScheduler = scheduler(audioPlayer)
    audioPlayer.addListener(trackScheduler)

    val manager = GuildAudioManager(audioPlayer, trackScheduler)
    guildPlayers[guildId] = manager
    AudioOutputManager.registerPlayer(guildId, manager.player)
}

fun <T : TrackScheduler<*>> VoiceChannelLeaveEvent.handleLeave(guildPlayers: MutableMap<Snowflake, GuildAudioManager<T>>) {
    val manager = guildPlayers.remove(guildId) ?: return
    AudioOutputManager.unregisterPlayer(guildId, manager.player)
    manager.player.destroy()
}
