package dev.deftu.ezrique.voice.tts

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.deftu.ezrique.voice.audio.scheduler.TrackScheduler
import dev.kord.common.entity.Snowflake
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TtsTrackScheduler(private val player: AudioPlayer) : TrackScheduler<TtsTrackScheduler.TtsQueueEntry>(player) {

    class TtsQueueEntry(track: AudioTrack, val message: Snowflake) : TrackScheduler.QueueEntry(track)

    override var current: TtsQueueEntry? = null
    override val queue: BlockingQueue<TtsQueueEntry> = LinkedBlockingQueue()

    fun play(message: Snowflake, track: AudioTrack) {
        player.playTrack(track)
        current = TtsQueueEntry(track, message)
    }

    fun queue(message: Snowflake, track: AudioTrack) {
        val isPlaying = player.startTrack(track, true)
        if (!isPlaying) {
            queue.offer(TtsQueueEntry(track, message))
        } else {
            current = TtsQueueEntry(track, message)
        }
    }

    /**
     * Starts the next message, skipping the current one if it's playing.
     * This message will continuously poll the queue for the next message, because multiple tracks can be from the same message because of the 300-character limit per message.
     */
    override fun skip() {
        val currentMessage = current?.message
        player.stopTrack()
        current = null
        var nextEntry = queue.poll()
        while (nextEntry != null && nextEntry.message == currentMessage) {
            nextEntry = queue.poll()
        }

        if (nextEntry != null) {
            play(nextEntry.message, nextEntry.track)
            current = nextEntry
        }
    }

    override fun seekTo(position: Long) {
        throw UnsupportedOperationException("Cannot seek in TTS tracks.")
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (!endReason.mayStartNext) {
            return
        }

        val nextEntry = queue.poll() ?: return
        play(nextEntry.message, nextEntry.track)
    }

}
