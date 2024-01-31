//package dev.deftu.ezrique.voice.audio.lavaplayer.spotify
//
//import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
//import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
//import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable
//import com.sedmelluq.discord.lavaplayer.track.AudioItem
//import com.sedmelluq.discord.lavaplayer.track.AudioReference
//import com.sedmelluq.discord.lavaplayer.track.AudioTrack
//import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
//import org.apache.http.client.config.RequestConfig
//import org.apache.http.impl.client.HttpClientBuilder
//import java.io.DataInput
//import java.io.DataOutput
//import java.util.function.Consumer
//import java.util.function.Function
//
//class SpotifyAudioSouceManager : AudioSourceManager, HttpConfigurable {
//    override fun getSourceName() = "spotify"
//
//    override fun loadItem(
//        manager: AudioPlayerManager,
//        reference: AudioReference
//    ): AudioItem {
//
//    }
//
//    override fun isTrackEncodable(track: AudioTrack) = true
//
//    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
//        // no-op
//    }
//
//    override fun decodeTrack(trackInfo: AudioTrackInfo?, input: DataInput?): AudioTrack {
//        TODO("Not yet implemented")
//    }
//
//    override fun shutdown() {
//        TODO("Not yet implemented")
//    }
//
//    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>?) {
//        TODO("Not yet implemented")
//    }
//
//}
