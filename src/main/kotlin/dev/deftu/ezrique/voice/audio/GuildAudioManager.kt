package dev.deftu.ezrique.voice.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.deftu.ezrique.voice.audio.scheduler.TrackScheduler

data class GuildAudioManager<T : TrackScheduler<*>>(
    val player: AudioPlayer,
    val scheduler: T
)
