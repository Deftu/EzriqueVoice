package dev.deftu.ezrique.voice.tts

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.deftu.ezrique.voice.audio.TrackScheduler
import dev.kord.common.entity.Snowflake
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TtsTrackScheduler(private val player: AudioPlayer) : TrackScheduler(player) {
    private var currentEntry: QueueEntry? = null
    private val queue: BlockingQueue<QueueEntry> = LinkedBlockingQueue()

    override fun play(track: AudioTrack) {
        throw UnsupportedOperationException()
    }

    fun play(message: Snowflake, track: AudioTrack) {
        player.playTrack(track)
        currentEntry = QueueEntry(message, track)
    }

    override fun queue(track: AudioTrack) {
        throw UnsupportedOperationException()
    }

    fun queue(message: Snowflake, track: AudioTrack) {
        val isPlaying = player.startTrack(track, true)
        if (!isPlaying) {
            queue.offer(QueueEntry(message, track))
        } else currentEntry = QueueEntry(message, track)
    }

    /**
     * Starts the next message, skipping the current one if it's playing.
     * This message will continuously poll the queue for the next message, because multiple tracks can be from the same message because of the 300-character limit per message.
     */
    override fun skip() {
        val currentMessage = currentEntry?.message
        player.stopTrack()
        currentEntry = null
        var nextEntry = queue.poll()
        while (nextEntry != null && nextEntry.message == currentMessage) {
            nextEntry = queue.poll()
        }

        if (nextEntry != null) {
            player.playTrack(nextEntry.track)
            currentEntry = nextEntry
        }
    }

    override fun clear() {
        player.stopTrack()
        queue.clear()
    }

    override fun setPaused(paused: Boolean) {
        player.isPaused = paused
    }

    override fun seekTo(position: Long) {
        throw UnsupportedOperationException()
    }

    override fun getTrack(index: Int): AudioTrack? {
        return queue.elementAtOrNull(index)?.track
    }

    override fun removeTrack(index: Int): AudioTrack? {
        val entry = queue.elementAtOrNull(index) ?: return null
        if (entry == currentEntry) {
            skip()
        }

        queue.remove(entry)
        return entry.track
    }

    override fun getQueueItems(): List<AudioTrack> {
        return queue.map { it.track }
    }

    override fun getQueueSize(): Int {
        return queue.size
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            val nextEntry = queue.poll() ?: return
            play(nextEntry.message, nextEntry.track)
        }
    }

    private data class QueueEntry(
        val message: Snowflake,
        val track: AudioTrack
    )
}
