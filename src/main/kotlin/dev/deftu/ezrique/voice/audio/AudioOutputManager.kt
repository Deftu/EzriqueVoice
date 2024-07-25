package dev.deftu.ezrique.voice.audio

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import dev.deftu.pcm.PcmAudioMixer
import dev.kord.common.entity.Snowflake
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object AudioOutputManager {

    private val encoder = StandardAudioDataFormats.DISCORD_OPUS.createEncoder(AudioConfiguration())
    private val managers = ConcurrentHashMap<Snowflake, MutableSet<TrackManager<*>>>()

    fun createOutputFor(guildId: Snowflake): ByteArray? {
        val players = managers[guildId] ?: return null
        val frames = players.map(TrackManager<*>::player).mapNotNull(AudioPlayer::provide)
        if (frames.isEmpty()) {
            return null
        }

        return try {
            val mixer = PcmAudioMixer()
            frames.map(AudioFrame::getData).forEach(mixer::addFrame)
            mixer.mix { input, output ->
                encoder.encode(input, output)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun registerPlayer(guildId: Snowflake, player: TrackManager<*>) {
        managers.computeIfAbsent(guildId) {
            mutableSetOf()
        }.add(player)
    }

    fun unregisterPlayer(guildId: Snowflake, player: TrackManager<*>) {
        managers[guildId]?.remove(player)
    }

}
