package dev.deftu.ezrique.voice.tts

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.deftu.ezrique.handleError
import dev.deftu.ezrique.voice.VoiceErrorCode
import dev.kord.core.entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class TtsAudioLoadResultHandler(
    private val coroutineScope: CoroutineScope,
    private val message: Message,
    private val scheduler: TtsTrackScheduler
) : AudioLoadResultHandler {

    override fun trackLoaded(track: AudioTrack) {
        scheduler.play(message.id, track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist?) {
        throw UnsupportedOperationException("TTS does not support playlists.")
    }

    override fun noMatches() {
        throw UnsupportedOperationException("TTS has no need for match checks.")
    }

    override fun loadFailed(exception: FriendlyException) {
        runBlocking {
            val errorMessage = handleError(exception, VoiceErrorCode.TTS_LOAD_FAILED, message)

            // Delete the above message after 15 seconds
            coroutineScope.run {
                delay(15_000)
                errorMessage.delete()
            }
        }
    }

}
