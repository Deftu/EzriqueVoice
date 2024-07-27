package dev.deftu.ezrique.voice.audio.scheduler

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class DefaultTrackScheduler(private val player: AudioPlayer) : TrackScheduler<TrackScheduler.QueueEntry>(player) {

    override val queue: BlockingQueue<QueueEntry> = LinkedBlockingQueue()

    var isLooping = false

    fun play(track: AudioTrack) {
        player.startTrack(track, false)
        current = QueueEntry(track)
    }

    fun queue(track: AudioTrack) {
        val isPlaying = player.startTrack(track, true)
        if (!isPlaying) {
            queue.offer(QueueEntry(track))
        } else {
            current = QueueEntry(track)
        }
    }

    override fun skip() {
        val nextEntry = queue.poll() ?: return
        play(nextEntry.track)
    }

    /**
     * Either replays the current track if we're looping, or forcibly skips to the next track if not.
     */
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (!endReason.mayStartNext) {
            return
        }

        if (isLooping) {
            play(track.makeClone())
        } else {
            skip()
        }
    }

}
