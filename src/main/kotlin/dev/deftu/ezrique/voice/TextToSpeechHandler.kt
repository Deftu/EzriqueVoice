package dev.deftu.ezrique.voice

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.deftu.ezrique.voice.sql.ChannelLink
import dev.deftu.ezrique.voice.sql.MemberConfig
import dev.deftu.ezrique.voice.tts.Weilnet
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.runBlocking

private const val DEFAULT_TEXT_BYPASS = ";"

object TextToSpeechHandler {
    fun setup() {
        kord.on<MessageCreateEvent> {
            handleMessage(this)
        }
    }

    private suspend fun handleMessage(event: MessageCreateEvent) {
        if (
            event.message.author?.isBot == true ||
            !MemberConfig.isEnabled(event.message.author!!.id.value.toLong()) ||
            event.message.content.startsWith(textBypass())
        ) return

        val guild = event.getGuildOrNull() ?: return
        val channel = event.member?.getVoiceStateOrNull()?.getChannelOrNull() ?: return
        if (!ChannelLink.isPresent(
                event.message.channelId.value.toLong(),
                channel.id.value.toLong()
        )) return

        val voice = MemberConfig.getVoice(event.message.author!!.id.value.toLong())
        val player = VoiceHandler.getPlayer(guild.id) ?: return

        val parsedText = parseMessage(guild, event.message.content)
        parsedText.chunked(300).forEach { chunk ->
            val data = try {
                Weilnet.tts(chunk, voice)
            } catch (e: Exception) {
                event.message.reply {
                    content = "An error occurred while trying to generate TTS audio!"
                }

                return@forEach
            }

            VoiceHandler.playerManager.loadItem(data, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    player.scheduler.queue(track)
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
        if (value == DEFAULT_TEXT_BYPASS) value = validate(config?.get("textBypass")?.asString)

        return value
    }
}
