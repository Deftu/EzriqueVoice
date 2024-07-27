package dev.deftu.ezrique.voice.audio.scheduler

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

abstract class TrackScheduler<E : TrackScheduler.QueueEntry>(
    private val player: AudioPlayer
) : AudioEventAdapter() {

    /**
     * The base class for queue entries. All entries must provide an audio track.
     */
    open class QueueEntry(val track: AudioTrack)

    protected abstract val queue: BlockingQueue<E>

    /**
     * The currently playing queue entry.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open var current: E? = null
        protected set

    /**
     * Whether the player is currently playing a track.
     *
     * @since 0.3.0
     * @author Deftu
     */
    val isPlaying: Boolean
        get() = player.playingTrack != null

    /**
     * Skips the currently playing queue entry.
     *
     * @since 0.3.0
     * @author Deftu
     */
    abstract fun skip()

    /**
     * Clears the queue of all entries.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun clear() {
        player.stopTrack()
        current = null
        queue.clear()
    }

    /**
     * Pauses the player.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun setPaused(isPaused: Boolean) {
        player.isPaused = isPaused
    }

    /**
     * Seeks to a specific position in the currently playing track.
     *
     * @param position The position to seek to in milliseconds.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun seekTo(position: Long) {
        current?.track?.position = position
    }

    /**
     * Gets the queue entry at the specified index.
     *
     * @param index The index of the queue entry.
     * @return The queue entry at the specified index.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun get(index: Int): E? {
        return queue.elementAtOrNull(index)
    }

    /**
     * Removes the queue entry at the specified index.
     *
     * @param index The index of the queue entry.
     * @return The queue entry that was removed.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun removeAt(index: Int): E? {
        val entry = queue.elementAtOrNull(index) ?: return null
        if (entry == current) {
            skip()
        }

        queue.remove(entry)
        return entry
    }

    /**
     * Gets all queue entries.
     *
     * @return A list of all queue entries.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun getQueueItems(): Set<E> {
        return queue.toSet()
    }

    /**
     * Gets the size of the queue.
     *
     * @return The size of the queue.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun getQueueSize(): Int {
        return queue.size
    }

    /**
     * Gets the duration of the currently playing track.
     *
     * @param timeUnit The time unit to convert the duration to.
     * @return The duration of the currently playing track.
     *
     * @since 0.3.0
     * @author Deftu
     */
    open fun getDuration(timeUnit: TimeUnit): Long {
        return current?.track?.duration?.let { duration ->
            timeUnit.convert(duration, TimeUnit.MILLISECONDS)
        } ?: 0
    }

}
