package dev.deftu.ezrique.voice

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.deftu.ezrique.voice.sql.ChannelLink
import dev.deftu.ezrique.voice.sql.MemberConfig
import dev.deftu.ezrique.voice.tts.Voice
import dev.deftu.ezrique.voice.tts.Weilnet
import dev.kord.common.Color
import dev.kord.common.annotation.KordInternal
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.common.java
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val DEFAULT_TEXT_BYPASS = ";"
private val messageCoroutineScope = CoroutineScope(kord.coroutineContext)

object TextToSpeechHandler {
    fun setupCommands(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input("tts", "Handles all Text-to-Speech features.") {
            subCommand("toggle", "Toggles whether the bot will read out your messages.") {
                boolean("enabled", "The new toggle value.")
            }

            subCommand("link", "Links a text channel to a voice channel.") {
                channel("text_channel", "The text channel to link.") {
                    channelTypes = listOf(ChannelType.GuildText)
                }

                channel("voice_channel", "The voice channel to link.") {
                    channelTypes = listOf(ChannelType.GuildVoice)
                }
            }

            subCommand("unlink", "Unlinks a text channel from a voice channel.") {
                channel("text_channel", "The text channel to unlink.") {
                    required = true
                    channelTypes = listOf(ChannelType.GuildText)
                }
            }

            subCommand("voice", "Sets the voice to use for TTS.") {
                string("voice", "The voice to use.") {
                    for (value in Voice.entries) {
                        choice(value.desc, value.code)
                    }
                }
            }

            subCommand("stop", "Stops the bot from speaking. (CLEARS THE QUEUE)")
            subCommand("skip", "Skips the current message.")
        }
    }

    @OptIn(KordInternal::class)
    suspend fun setup() {
        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            val commandName = interaction.command.rootName
            val guild = interaction.getGuildOrNull()
            if (guild == null) {
                interaction.respondEphemeral {
                    embed {
                        title = "Not in a Server"
                        color = Color(0x8C0317)
                        description = "This command can only be used in servers!"
                    }
                }

                return@on
            }

            val guildId = guild.id.value.toLong()
            val member = interaction.user.asMember()
            if (commandName != "tts") return@on

            val command = interaction.command
            val subCommand = command as SubCommand
            val userId = member.id.value.toLong()
            val response = interaction.deferEphemeralResponse()

            when (subCommand.name) {
                "toggle" -> {
                    val enabled = command.booleans["enabled"] ?: !MemberConfig.isEnabled(userId)
                    MemberConfig.setEnabled(userId, enabled)

                    response.respond {
                        embed {
                            description = "Text-to-Speech ${if (enabled) "Enabled" else "Disabled"}"
                            color = if (enabled) Color(0x038700) else Color(0x8C0317)
                        }
                    }
                }

                "link" -> {
                    if (member.permissions?.contains(Permission.ManageChannels) == false) {
                        response.respond {
                            embed {
                                title = "Missing Permissions"
                                color = Color(0x8C0317)
                                description = "You need the Manage Channels permission to use this command!"
                            }
                        }

                        return@on
                    }

                    val textChannel = command.channels["text_channel"]?.asChannelOrNull()
                    val voiceChannel = command.channels["voice_channel"]?.asChannelOrNull()

                    if (textChannel == null || voiceChannel == null) {
                        val links = ChannelLink.getLinks(guildId)
                        response.respond {
                            embed {
                                title = "Linked Channels"
                                color = if (links.isEmpty()) Color(0x8C0317) else Color(0x038700)
                                description = buildString {
                                    if (links.isEmpty()) {
                                        append("No linked channels!")
                                        return@buildString
                                    }

                                    links.forEach { link ->
                                        val text = guild.getChannelOrNull(Snowflake(link.textChannelId)) ?: return@forEach
                                        val voice = guild.getChannelOrNull(Snowflake(link.voiceChannelId)) ?: return@forEach
                                        appendLine("${text.mention} ➡️ ${voice.mention}")
                                    }
                                }
                            }
                        }

                        return@on
                    }

                    try {
                        ChannelLink.createLink(guildId, textChannel.id.value.toLong(), voiceChannel.id.value.toLong())
                        response.respond {
                            embed {
                                description = "Linked ${textChannel.mention} to ${voiceChannel.mention}!"
                                color = Color(0x038700)
                            }
                        }
                    } catch (e: Exception) {
                        response.respond {
                            embed {
                                description = "Failed to create link for ${textChannel.mention} to ${voiceChannel.mention}! ||**[${e::class.java.simpleName}]** Report this in the support server, found under `/about`||"
                                color = Color(0x8C0317)
                            }
                        }
                    }
                }

                "unlink" -> {
                    if (member.permissions?.contains(Permission.ManageChannels) == false) {
                        response.respond {
                            embed {
                                title = "Missing Permissions"
                                color = Color(0x8C0317)
                                description = "You need the Manage Channels permission to use this command!"
                            }
                        }

                        return@on
                    }

                    val textChannel = command.channels["text_channel"]!!.asChannelOrNull()

                    try {
                        ChannelLink.removeLink(textChannel.id.value.toLong())
                        response.respond {
                            embed {
                                description = "Unlinked ${textChannel.mention}!"
                                color = Color(0x038700)
                            }
                        }
                    } catch (e: Exception) {
                        response.respond {
                            embed {
                                description = "Failed to remove link for ${textChannel.mention}! ||**[${e::class.java.simpleName}]** Report this in the support server, found under `/about`||"
                                color = Color(0x8C0317)
                            }
                        }
                    }
                }

                "stop" -> {
                    // TODO
                }

                "skip" -> {
                    // TODO
                }

                "voice" -> {
                    val voiceCode = interaction.command.strings["voice"]!!
                    val voice = Voice.fromCode(voiceCode)
                    MemberConfig.setVoice(userId, voice)
                    response.respond {
                        embed {
                            title = "Voice Changed"
                            color = Color(0x038700)
                            description = "Set your voice to ${voice.desc}!"
                        }
                    }
                }
            }
        }

        kord.on<MessageCreateEvent> {
            handleMessage(this)
        }
    }

    @OptIn(KordInternal::class)
    private suspend fun handleMessage(event: MessageCreateEvent) {
        if (
            event.message.author?.isBot == true ||
            !MemberConfig.isEnabled(event.message.author!!.id.value.toLong()) ||
            event.message.content.startsWith(textBypass())
        ) return

        val guild = event.getGuildOrNull() ?: return
        val channel = event.member?.getVoiceStateOrNull()?.getChannelOrNull() ?: return

        if (!VoiceHandler.isVoiceConnected(guild.id)) return
        if (!ChannelLink.isPresent(
                event.message.channelId.value.toLong(),
                channel.id.value.toLong()
        ) && channel.id != event.message.channelId) return

        val voice = MemberConfig.getVoice(event.message.author!!.id.value.toLong())
        val player = VoiceHandler.getPlayer(guild.id) ?: return

        val parsedText = parseMessage(guild, event.message.content)
        parsedText.chunked(300).forEach { chunk ->
            val data = try {
                Weilnet.tts(chunk, voice)
            } catch (e: Exception) {
                val message = event.message.reply {
                    embed {
                        description = "There was an error while trying to convert your message to speech! ||**[${e::class.java.simpleName}]** Report this in the support server, found under `/about`||"
                        color = Color(0x8C0317)
                    }
                }

                // Delete the above message after 15 seconds
                messageCoroutineScope.run {
                    delay(15_000)
                    message.delete()
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
