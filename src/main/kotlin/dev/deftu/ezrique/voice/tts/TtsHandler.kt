package dev.deftu.ezrique.voice.tts

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.deftu.ezrique.config
import dev.deftu.ezrique.handleError
import dev.deftu.ezrique.voice.*
import dev.deftu.ezrique.voice.events.VoiceChannelJoinEvent
import dev.deftu.ezrique.voice.audio.GuildAudioManager
import dev.deftu.ezrique.voice.audio.lavaplayer.ByteArrayAudioSourceManager
import dev.deftu.ezrique.voice.events.VoiceChannelLeaveEvent
import dev.deftu.ezrique.voice.sql.ChannelLink
import dev.deftu.ezrique.voice.sql.GuildConfig
import dev.deftu.ezrique.voice.sql.MemberConfig
import dev.deftu.ezrique.voice.utils.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private const val DEFAULT_TEXT_BYPASS = ";"

object TtsHandler {

    private val LOGGER = LoggerFactory.getLogger("${EzriqueVoice.NAME} TTS Handler")

    private val guildManagers = mutableMapOf<Snowflake, GuildAudioManager<TtsTrackScheduler>>()
    private val playerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(ByteArrayAudioSourceManager())
        configure()
    }

    fun setup(kord: Kord) {
        setupVoiceListeners(kord)
        setupMessageListener(kord)
    }

    private fun setupVoiceListeners(kord: Kord) {
        kord.on<VoiceChannelJoinEvent> {
            handleJoin(playerManager, guildManagers) { player ->
                TtsTrackScheduler(player)
            }
        }

        kord.on<VoiceChannelLeaveEvent> {
            handleLeave(guildManagers)
        }
    }

    private fun setupMessageListener(kord: Kord) {
        val messageCoroutineScope = CoroutineScope(kord.coroutineContext)

        kord.on<MessageCreateEvent> {
            val author = message.author ?: return@on
            val guild = getGuildOrNull() ?: return@on
            if (message.author?.isBot == true) return@on

            if (
                !GuildConfig.isTtsEnabled(guild.id.value.toLong()) ||
                !MemberConfig.isTtsEnabled(author.id.value.toLong()) ||
                message.content.startsWith(textBypass())
            ) return@on

            val voiceState = member?.getVoiceStateOrNull() ?: return@on
            val channel = voiceState.getChannelOrNull() ?: return@on

            if (
                !VoiceConnectionManager.isConnected(guild.id) ||
                (!ChannelLink.isPresent(
                    message.channelId.value.toLong(),
                    channel.id.value.toLong()
                ) &&
                channel.id != message.channelId)
            ) return@on

            LOGGER.debug("TTS Message - Channel is linked to user's voice channel.")

            val voice = MemberConfig.getVoice(author.id.value.toLong())
            val manager = guildManagers[guild.id] ?: return@on

            LOGGER.debug("TTS Message - Player is ready.")

            val parsedText = parseMessage(guild, message.content)
            val chunkedText = parsedText.chunked(300)

            LOGGER.debug("TTS Message - Text is parsed and has been split into ${chunkedText.size} chunks.")

            chunkedText.forEach { chunk ->
                val data = try {
                    Weilnet.getAudioData(chunk, voice)
                } catch (t: Throwable) {
                    val message = handleError(t, VoiceErrorCode.READ_TTS, message)

                    // Delete the above message after 15 seconds
                    messageCoroutineScope.run {
                        delay(15_000)
                        message.delete()
                    }

                    return@forEach
                }

                playerManager.loadItem(data, TtsAudioLoadResultHandler(messageCoroutineScope, message, manager.scheduler))
            }
        }
    }

    suspend fun getPlayer(
        guildId: Long,
        response: DeferredEphemeralMessageInteractionResponseBehavior
    ) = guildManagers.getPlayer(guildId, response)

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