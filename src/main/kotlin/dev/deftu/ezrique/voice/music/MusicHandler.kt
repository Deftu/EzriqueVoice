package dev.deftu.ezrique.voice.music

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.deftu.ezrique.voice.audio.GuildAudioManager
import dev.deftu.ezrique.voice.audio.scheduler.DefaultTrackScheduler
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

    private val guildManagers = mutableMapOf<Snowflake, GuildAudioManager<DefaultTrackScheduler>>()
    private val playerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
        configure()
    }

    fun setup(kord: Kord) {
        kord.on<VoiceChannelJoinEvent> {
            handleJoin(playerManager, guildManagers) { player ->
                DefaultTrackScheduler(player)
            }
        }

        kord.on<VoiceChannelLeaveEvent> {
            handleLeave(guildManagers)
        }
    }

    fun playFromYouTube(
        input: String,
        player: GuildAudioManager<DefaultTrackScheduler>,
        response: DeferredEphemeralMessageInteractionResponseBehavior
    ) = playerManager.loadItem(input, YouTubeAudioLoadResultHandler(player, response))

    suspend fun getPlayer(
        guildId: Long,
        response: DeferredEphemeralMessageInteractionResponseBehavior? = null
    ) = guildManagers.getPlayer(guildId, response)

}
