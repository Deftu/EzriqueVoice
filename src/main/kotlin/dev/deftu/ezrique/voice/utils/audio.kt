package dev.deftu.ezrique.voice.utils

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import java.nio.ByteOrder

fun DefaultAudioPlayerManager.configure() {
    configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH
    configuration.outputFormat = when (ByteOrder.nativeOrder()) {
        ByteOrder.BIG_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_LE
        ByteOrder.LITTLE_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_BE
        else -> throw IllegalStateException("Unknown byte order: ${ByteOrder.nativeOrder()}")
    }
}
