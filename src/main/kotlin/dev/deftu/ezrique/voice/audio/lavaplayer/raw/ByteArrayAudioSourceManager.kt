package dev.deftu.ezrique.voice.audio.lavaplayer.raw

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import java.io.DataInput
import java.io.DataOutput
import java.util.Base64

class ByteArrayAudioSourceManager(
    containerRegistry: MediaContainerRegistry = MediaContainerRegistry.DEFAULT_REGISTRY
) : ProbingAudioSourceManager(containerRegistry) {
    private fun getContainer(reference: AudioReference): MediaContainerDetectionResult {
        try {
            ByteArraySeekableInputStream(Base64.getDecoder().decode(reference.identifier)).use { stream ->
                return MediaContainerDetection(
                    containerRegistry,
                    reference,
                    stream,
                    MediaContainerHints.from(null, null)
                ).detectContainer()
            }
        } catch (e: Exception) {
            throw RuntimeException("Error loading audio from byte array", e)
        }
    }

    override fun getSourceName() = "bytearray"

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem {
        return handleLoadResult(getContainer(reference))
    }

    override fun isTrackEncodable(track: AudioTrack) = true

    override fun createTrack(trackInfo: AudioTrackInfo, descriptor: MediaContainerDescriptor): AudioTrack =
        ByteArrayAudioTrack(trackInfo, descriptor, this)

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        encodeTrackFactory((track as ByteArrayAudioTrack).factory, output)
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        val factory = decodeTrackFactory(input)
        if (factory != null) {
            return ByteArrayAudioTrack(trackInfo, factory, this)
        }

        return null
    }

    override fun shutdown() {
        // Nothing to shut down
    }
}
