package dev.deftu.ezrique.voice.lavaplayer

import com.sedmelluq.discord.lavaplayer.tools.io.ExtendedBufferedInputStream
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider

class ByteArraySeekableInputStream(
    bytes: ByteArray
) : SeekableInputStream(bytes.size.toLong(), 0) {
    private val internalStream = bytes.inputStream()
    private val bufferedStream = ExtendedBufferedInputStream(internalStream)

    private var position: Long = 0

    override fun available(): Int = bufferedStream.available()
    override fun getPosition(): Long = position
    override fun markSupported(): Boolean = false
    override fun canSeekHard(): Boolean = true
    override fun getTrackInfoProviders(): MutableList<AudioTrackInfoProvider> = mutableListOf()

    override fun read(): Int {
        val read = bufferedStream.read()
        if (read >= 0) {
            position++
        }

        return read
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = bufferedStream.read(buffer, offset, length)
        position += read
        return read
    }

    override fun skip(n: Long): Long {
        val skipped = bufferedStream.skip(n)
        position += skipped
        return skipped
    }

    override fun seekHard(position: Long) {
        this.position = position
        bufferedStream.discardBuffer()
    }
}
