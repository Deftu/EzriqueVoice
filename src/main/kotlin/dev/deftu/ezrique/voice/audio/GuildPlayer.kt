package dev.deftu.ezrique.voice.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer

data class GuildPlayer<T : TrackScheduler>(
    val player: AudioPlayer,
    val scheduler: T
)
