package dev.deftu.ezrique.voice.lavaplayer

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer

data class PlayerInstance(
    val player: AudioPlayer,
    val scheduler: TrackScheduler
)
