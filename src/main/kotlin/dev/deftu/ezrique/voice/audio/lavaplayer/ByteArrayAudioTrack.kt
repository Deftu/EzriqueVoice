package dev.deftu.ezrique.voice.audio.lavaplayer

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import java.util.Base64

class ByteArrayAudioTrack(
    trackInfo: AudioTrackInfo,
    val factory: MediaContainerDescriptor,
    private val sourceManager: ByteArrayAudioSourceManager
) : DelegatedAudioTrack(trackInfo) {

    private val bytes: ByteArray = createBufferedByteArray()

    private fun createBufferedByteArray(): ByteArray {
        val decodedTrack = Base64.getDecoder().decode(trackInfo.identifier)
        val paddingSize = 6
        val buffer = ByteArray(decodedTrack.size + paddingSize)
        System.arraycopy(decodedTrack, 0, buffer, paddingSize, decodedTrack.size - paddingSize)
        return buffer
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        ByteArraySeekableInputStream(bytes).use { stream ->
            processDelegate(factory.createTrack(trackInfo, stream) as InternalAudioTrack, executor)
        }
    }

    override fun makeShallowClone(): AudioTrack = ByteArrayAudioTrack(trackInfo, factory, sourceManager)
    override fun getSourceManager(): AudioSourceManager = sourceManager

}
