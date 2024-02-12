package dev.deftu.ezrique.voice.tts

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.deftu.ezrique.config
import dev.deftu.ezrique.voice.*
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.audio.GuildPlayer
import dev.deftu.ezrique.voice.audio.lavaplayer.raw.ByteArrayAudioSourceManager
import dev.deftu.ezrique.voice.sql.ChannelLink
import dev.deftu.ezrique.voice.sql.GuildConfig
import dev.deftu.ezrique.voice.sql.MemberConfig
import dev.deftu.ezrique.voice.utils.ErrorCode
import dev.deftu.ezrique.voice.utils.getPlayer
import dev.deftu.ezrique.voice.utils.handleError
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val DEFAULT_TEXT_BYPASS = ";"
private val messageCoroutineScope = CoroutineScope(kord.coroutineContext)

object TtsHandler {
    private val guildPlayers = mutableMapOf<Snowflake, GuildPlayer<TtsTrackScheduler>>()
    private val playerManager = DefaultAudioPlayerManager().apply {
        AudioSourceManagers.registerRemoteSources(this)
        registerSourceManager(ByteArrayAudioSourceManager())
        configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH
        configuration.outputFormat = when (ByteOrder.nativeOrder()) {
            ByteOrder.BIG_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_LE
            ByteOrder.LITTLE_ENDIAN -> StandardAudioDataFormats.DISCORD_PCM_S16_BE
            else -> throw IllegalStateException("Unknown byte order: ${ByteOrder.nativeOrder()}")
        }
    }

    suspend fun setup() {
        kord.on<VoiceChannelJoinEvent> {
            val audioPlayer = playerManager.createPlayer()
            val scheduler = TtsTrackScheduler(audioPlayer)
            audioPlayer.addListener(scheduler)

            val player = GuildPlayer(audioPlayer, scheduler)
            guildPlayers[guildId] = player
            VoiceHandler.registerPlayer(guildId, player)
        }

        kord.on<MessageCreateEvent> {
            val guild = getGuildOrNull() ?: return@on

            if (
                message.author?.isBot == true ||
                !GuildConfig.isTtsEnabled(guild.id.value.toLong()) ||
                !MemberConfig.isTtsEnabled(message.author!!.id.value.toLong()) ||
                message.content.startsWith(textBypass())
            ) return@on

            val channel = member?.getVoiceStateOrNull()?.getChannelOrNull() ?: return@on

            if (!VoiceHandler.isVoiceConnected(guild.id)) return@on
            if (!ChannelLink.isPresent(
                    message.channelId.value.toLong(),
                    channel.id.value.toLong()
                ) && channel.id != message.channelId) return@on

            val voice = MemberConfig.getVoice(message.author!!.id.value.toLong())
            val player = guildPlayers[guild.id] ?: return@on

            val parsedText = parseMessage(guild, message.content)
            parsedText.chunked(300).forEach { chunk ->
                val data = try {
                    Weilnet.tts(chunk, voice)
                } catch (e: Exception) {
                    val message = handleError(message, ErrorCode.READ_TTS, e)

                    // Delete the above message after 15 seconds
                    messageCoroutineScope.run {
                        delay(15_000)
                        message.delete()
                    }

                    return@forEach
                }

                playerManager.loadItem(data, object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        player.scheduler.queue(message.id, track)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist?) {
                        throw UnsupportedOperationException("Not supported")
                    }

                    override fun noMatches() {
                        throw UnsupportedOperationException("Not supported")
                    }

                    override fun loadFailed(exception: FriendlyException) {
                        throw UnsupportedOperationException("Not supported")
                    }
                })
            }
        }
    }

    suspend fun getPlayer(
        guildId: Long,
        response: DeferredEphemeralMessageInteractionResponseBehavior
    ) = guildPlayers.getPlayer(guildId, response)

    private fun parseMessage(
        guild: Guild,
        input: String
    ): String {
        val urlRegex = Regex("http\\S+", RegexOption.MULTILINE)
        val nonAsciiRegex = Regex("[^(\\x00-\\xFF)]+(?:\$|\\s*)", RegexOption.MULTILINE)
        val nonAlphanumericRegex = Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", RegexOption.MULTILINE)
        val mentionRegex = Regex("@(\\d+)", RegexOption.MULTILINE)

        var output = urlRegex.replace(input, "link")
        output = nonAsciiRegex.replace(output, "")
        output = nonAlphanumericRegex.replace(output, "")
        output = mentionRegex.replace(output) { match ->
            val id = match.groupValues[1].toLongOrNull() ?: return@replace match.value
            val user = runBlocking { guild.getMemberOrNull(Snowflake(id)) } ?: return@replace match.value
            user.effectiveName
        }

        return output
            .replace("pls", "please")
            .replace("\n", ", ")
            .replace("&", "and")
            .replace("%", "percent")
            .replace("+", "plus")
            .replace("*", "")
    }

    private fun textBypass(): String {
        fun validate(input: String?): String {
            return if (input.isNullOrEmpty()) {
                DEFAULT_TEXT_BYPASS
            } else if (input.length > 1) {
                input.substring(0, 1)
            } else input
        }

        var value = validate(System.getenv("TEXT_BYPASS"))
        if (value == DEFAULT_TEXT_BYPASS) value = validate(config.get("textBypass")?.asString)

        return value
    }
}