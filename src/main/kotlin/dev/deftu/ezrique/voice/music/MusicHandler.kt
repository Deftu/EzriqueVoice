package dev.deftu.ezrique.voice.music

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.deftu.ezrique.voice.audio.DefaultTrackScheduler
import dev.deftu.ezrique.voice.audio.TrackManager
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.events.VoiceChannelLeaveEvent
import dev.deftu.ezrique.voice.utils.configure
import dev.deftu.ezrique.voice.utils.getPlayer
import dev.deftu.ezrique.voice.utils.handleJoin
import dev.deftu.ezrique.voice.utils.handleLeave
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.on
import dev.lavalink.youtube.YoutubeAudioSourceManager

object MusicHandler {

    private val guildPlayers = mutableMapOf<Snowflake, TrackManager<DefaultTrackScheduler>>()
    private val playerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
        configure()
    }

    fun setup(kord: Kord) {
        kord.on<VoiceChannelJoinEvent> {
            handleJoin(playerManager, guildPlayers) { player ->
                DefaultTrackScheduler(player)
            }
        }

        kord.on<VoiceChannelLeaveEvent> {
            handleLeave(guildPlayers)
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
