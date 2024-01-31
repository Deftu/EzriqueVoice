package dev.deftu.ezrique.voice.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.deftu.ezrique.voice.audio.DefaultTrackScheduler
import dev.deftu.ezrique.voice.audio.GuildPlayer
import dev.kord.common.Color
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.runBlocking

class YouTubeAudioLoadResultHandler(
    private val player: GuildPlayer<DefaultTrackScheduler>,
    private val response: DeferredEphemeralMessageInteractionResponseBehavior
) : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {
        player.scheduler.queue(track)
        runBlocking {
            response.respond {
                embed {
                    title = "YouTube"
                    color = Color(0xFF0000)
                    description = "Added [${track.info.title}](${track.info.uri}) to the queue."
                }
            }
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        val tracks = playlist.tracks

        if (tracks.size == 1 || playlist.isSearchResult) {
            trackLoaded(tracks[0])
        } else {
            tracks.forEach { player.scheduler.queue(it) }
            runBlocking {
                response.respond {
                    embed {
                        title = "YouTube"
                        color = Color(0xFF0000)
                        description = "Added ${tracks.size} tracks to the queue."
                    }
                }
            }
        }
    }

    override fun noMatches() {
        runBlocking {
            response.respond {
                embed {
                    title = "YouTube"
                    color = Color(0xFF0000)
                    description = "No matches found."
                }
            }
        }
    }

    override fun loadFailed(exception: FriendlyException) {
        exception.printStackTrace()

        runBlocking {
            response.respond {
                embed {
                    title = "YouTube"
                    color = Color(0xFF0000)
                    description = "Failed to load track."
                }
            }
        }
    }
}
