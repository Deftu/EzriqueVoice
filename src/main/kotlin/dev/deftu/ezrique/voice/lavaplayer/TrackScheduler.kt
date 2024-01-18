package dev.deftu.ezrique.voice.lavaplayer

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(
    val player: AudioPlayer,
    var queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()
) : AudioEventAdapter() {
    fun play(track: AudioTrack) {
        player.startTrack(track, false)
    }

    fun queue(track: AudioTrack) {
        if (!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }

    fun skip() {
        player.startTrack(queue.poll(), false)
    }

    fun clear() {
        player.stopTrack()
        queue.clear()
    }

    fun stop() {
        player.stopTrack()
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            skip()
        }
    }
}
