package dev.deftu.ezrique.voice.music

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.deftu.ezrique.voice.audio.AudioOutputManager
import dev.deftu.ezrique.voice.audio.DefaultTrackScheduler
import dev.deftu.ezrique.voice.audio.TrackManager
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.events.VoiceChannelLeaveEvent
import dev.deftu.ezrique.voice.utils.getPlayer
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.on
import dev.lavalink.youtube.YoutubeAudioSourceManager
import io.ktor.utils.io.core.*

object MusicHandler {

    private val guildPlayers = mutableMapOf<Snowflake, TrackManager<DefaultTrackScheduler>>()
    private val playerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())

        configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH
        configuration.outputFormat = when (ByteOrder.nativeOrder()) {
            ByteOrder.BIG_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_LE
            ByteOrder.LITTLE_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_BE
            else -> throw IllegalStateException("Unknown byte order: ${ByteOrder.nativeOrder()}")
        }
    }

    fun setup(kord: Kord) {
        kord.on<VoiceChannelJoinEvent> {
            val audioPlayer = playerManager.createPlayer()
            val scheduler = DefaultTrackScheduler(audioPlayer)
            audioPlayer.addListener(scheduler)

            val player = TrackManager(audioPlayer, scheduler)
            guildPlayers[guildId] = player
            AudioOutputManager.registerPlayer(guildId, player)
        }

        kord.on<VoiceChannelLeaveEvent> {
            val player = guildPlayers.remove(guildId) ?: return@on
            AudioOutputManager.unregisterPlayer(guildId, player)
        }
    }

    fun playFromYouTube(
        input: String,
        player: TrackManager<DefaultTrackScheduler>,
        response: DeferredEphemeralMessageInteractionResponseBehavior
    ) = playerManager.loadItem(input, YouTubeAudioLoadResultHandler(player, response))

    suspend fun getPlayer(
        guildId: Long,
        response: DeferredEphemeralMessageInteractionResponseBehavior? = null
    ) = guildPlayers.getPlayer(guildId, response)

}
