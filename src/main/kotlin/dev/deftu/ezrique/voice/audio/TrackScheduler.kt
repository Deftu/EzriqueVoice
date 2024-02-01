package dev.deftu.ezrique.voice.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

abstract class TrackScheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {
    fun isPlaying(): Boolean = player.playingTrack != null

    abstract fun play(track: AudioTrack)
    abstract fun queue(track: AudioTrack)
    abstract fun skip()
    abstract fun clear()
    abstract fun setPaused(paused: Boolean)
    abstract fun seekTo(position: Long)

    abstract fun getTrack(index: Int): AudioTrack?
    abstract fun removeTrack(index: Int): AudioTrack?
    abstract fun getQueueItems(): List<AudioTrack>
    abstract fun getQueueSize(): Int

    open fun getCurrentTrack(): AudioTrack? = null
    open fun loop(): Boolean = false
}

class DefaultTrackScheduler(
    private val player: AudioPlayer,
    private val queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()
) : TrackScheduler(player) {
    private var looping = false

    override fun play(track: AudioTrack) {
        player.startTrack(track, false)
    }

    override fun queue(track: AudioTrack) {
        if (!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }

    override fun skip() {
        player.startTrack(queue.poll(), false)
    }

    override fun clear() {
        player.stopTrack()
        queue.clear()
    }

    override fun setPaused(paused: Boolean) {
        player.isPaused = paused
    }

    override fun seekTo(position: Long) {
        player.playingTrack?.position = position * 1000
    }

    override fun getTrack(index: Int): AudioTrack? {
        return queue.elementAtOrNull(index)
    }

    override fun removeTrack(index: Int): AudioTrack? {
        val track = queue.elementAtOrNull(index)
        if (track != null) {
            if (player.playingTrack == track) {
                skip()
            }

            queue.remove(track)
        }

        return track
    }

    override fun getQueueItems(): List<AudioTrack> {
        return queue.toList()
    }

    override fun getQueueSize() = queue.size

    override fun getCurrentTrack(): AudioTrack? {
        return player.playingTrack
    }

    override fun loop(): Boolean {
        looping = !looping
        return looping
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            if (looping) {
                play(track.makeClone())
            } else {
                skip()
            }
        }
    }
}
