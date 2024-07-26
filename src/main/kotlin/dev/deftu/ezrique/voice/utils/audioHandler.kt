package dev.deftu.ezrique.voice.utils

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.deftu.ezrique.voice.audio.AudioOutputManager
import dev.deftu.ezrique.voice.audio.TrackManager
import dev.deftu.ezrique.voice.audio.TrackScheduler
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.events.VoiceChannelLeaveEvent
import dev.kord.common.entity.Snowflake

fun <T : TrackScheduler> VoiceChannelJoinEvent.handleJoin(
    playerManager: DefaultAudioPlayerManager,
    guildPlayers: MutableMap<Snowflake, TrackManager<T>>,
    scheduler: (audioPlayer: AudioPlayer) -> T
) {
    val audioPlayer = playerManager.createPlayer()
    val trackScheduler = scheduler(audioPlayer)
    audioPlayer.addListener(trackScheduler)

    val player = TrackManager(audioPlayer, trackScheduler)
    guildPlayers[guildId] = player
    AudioOutputManager.registerPlayer(guildId, player)
}

fun <T : TrackScheduler> VoiceChannelLeaveEvent.handleLeave(guildPlayers: MutableMap<Snowflake, TrackManager<T>>) {
    val player = guildPlayers.remove(guildId) ?: return
    AudioOutputManager.unregisterPlayer(guildId, player)
}
