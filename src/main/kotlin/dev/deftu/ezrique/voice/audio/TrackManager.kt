package dev.deftu.ezrique.voice.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer

data class TrackManager<T : TrackScheduler>(
    val player: AudioPlayer,
    val scheduler: T
)
