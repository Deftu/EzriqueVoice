package dev.deftu.ezrique.voice

import dev.deftu.ezrique.voice.sql.ChannelLink
import dev.deftu.ezrique.voice.sql.MemberConfig
import dev.deftu.ezrique.voice.tts.Voice
import dev.kord.common.Color
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.count

object CommandHandler {
    suspend fun setup() {
        kord.createGlobalChatInputCommand("toggle", "Toggles the TTS feature for the current user.") {
            boolean("enabled", "Whether to enable or disable TTS.")
        }

        kord.createGlobalChatInputCommand("link", "Links a text channel to a voice channel for TTS.") {
            channel("text_channel", "The text channel to link.") {
                required = true
                channelTypes = listOf(ChannelType.GuildText)
            }

            channel("voice_channel", "The voice channel to link.") {
                required = true
                channelTypes = listOf(ChannelType.GuildVoice)
            }
        }

        kord.createGlobalChatInputCommand("join", "Makes the bot join the current voice channel. Only works on one channel per server!") {
            channel("voice_channel", "The voice channel to join. This option is only available to moderators.") {
                channelTypes = listOf(ChannelType.GuildVoice)
            }
        }

        kord.createGlobalChatInputCommand("leave", "Makes the bot leave the current voice channel. Only works on one channel per server!")
        kord.createGlobalChatInputCommand("stop", "Stops the current track.")
        kord.createGlobalChatInputCommand("skip", "Skips the current track.")
        kord.createGlobalChatInputCommand("clear", "Clears the queue.")
        kord.createGlobalChatInputCommand("voice", "Selects the voice to use for TTS.") {
            string("voice", "The voice to use.") {
                required = true
                for (value in Voice.values()) {
                    choice(value.desc, value.code)
                }

            }
        }

        kord.createGlobalChatInputCommand("about", "Shows information about the bot.")

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            val commandName = interaction.command.rootName
            val guild = interaction.getGuildOrNull() ?: return@on

            when (commandName) {
                "toggle" -> handleToggleCommand(guild, this)
                "link" -> handleLinkCommand(guild, this)
                "join" -> handleJoinCommand(guild, this)
                "leave" -> handleLeaveCommand(guild, this)
                "stop" -> handleStopCommand(guild, this)
                "skip" -> handleSkipCommand(guild, this)
                "clear" -> handleClearCommand(guild, this)
                "voice" -> handleVoiceCommand(guild, this)
                "about" -> handleAboutCommand(guild, this)
            }
        }
    }

    private suspend fun handleToggleCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.deferEphemeralResponse()

        val userId = event.interaction.user.id.value.toLong()
        val enabled = event.interaction.command.booleans["enabled"] ?: !MemberConfig.isEnabled(userId)
        MemberConfig.setEnabled(userId, enabled)

        message.respond {
            embed {
                title = "TTS ${if (enabled) "Enabled" else "Disabled"}"
                color = if (enabled) Color(0x038700) else Color(0x8C0317)
            }
        }
    }

    private suspend fun handleLinkCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.deferEphemeralResponse()
        if (event.interaction.user.asMember().permissions?.contains(Permission.ManageChannels) != true) {
            message.respond {
                content = "You need the Manage Channels permission to use this command!"
            }

            return
        }

        val textChannel = event.interaction.command.channels["text_channel"]?.asChannelOrNull() ?: return
        val voiceChannel = event.interaction.command.channels["voice_channel"]?.asChannelOrNull() ?: return
        ChannelLink.setupLink(textChannel.id.value.toLong(), voiceChannel.id.value.toLong())
        message.respond {
            content = "Linked channels!"
        }
    }

    @OptIn(KordVoice::class)
    private suspend fun handleJoinCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.deferEphemeralResponse()

        var channel = event.interaction.command.channels["voice_channel"]?.asChannelOf<VoiceChannel>()
        if (channel == null || event.interaction.user.asMember().permissions?.contains(Permission.ManageChannels) != true) {
            channel = event.interaction.user.getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel

            if (channel == null) {
                message.respond {
                    content = "You have to be in a voice channel to use this command!"
                }

                return
            }
        }

        if (channel.guildId != guild.id) {
            message.respond {
                content = "You have to select a voice channel in this server to use this command!"
            }

            return
        }

        val connection = VoiceHandler.getVoiceConnection(guild.id)
        if (connection != null) {
            message.respond {
                content = "Already in a voice channel!"
            }

            return
        }

        VoiceHandler.setVoiceChannel(guild.id, channel)
        message.respond {
            content = "Joined channel!"
        }
    }

    private suspend fun handleLeaveCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.deferEphemeralResponse()

        val channel = event.interaction.user.getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel
        var flag = true
        if (channel == null) {
            val member = event.interaction.user.asMember(guild.id)
            if (member.permissions?.contains(Permission.ManageChannels) == true) {
                flag = false
            } else {
                message.respond {
                    content = "You have to be in a voice channel to use this command!"
                }

                return
            }
        }

        if (channel?.guildId != guild.id && flag) {
            message.respond {
                content = "You have to be in a voice channel in this server to use this command!"
            }

            return
        }

        val didLeave = VoiceHandler.leaveVoiceChannel(guild.id)
        message.respond {
            content = if (didLeave) "Left channel!" else "Not in a voice channel!"
        }
    }

    private suspend fun handleStopCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val player = VoiceHandler.getPlayer(guild.id)
        if (player == null) {
            event.interaction.respondEphemeral {
                content = "Not in a voice channel!"
            }

            return
        }

        player.scheduler.stop()
        event.interaction.respondEphemeral {
            content = "Stopped track!"
        }
    }

    private suspend fun handleSkipCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val player = VoiceHandler.getPlayer(guild.id)
        if (player == null) {
            event.interaction.respondEphemeral {
                content = "Not in a voice channel!"
            }

            return
        }

        player.scheduler.skip()
        event.interaction.respondEphemeral {
            content = "Skipped track!"
        }
    }

    private suspend fun handleClearCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val player = VoiceHandler.getPlayer(guild.id)
        if (player == null) {
            event.interaction.respondEphemeral {
                content = "Not in a voice channel!"
            }

            return
        }

        player.scheduler.clear()
        event.interaction.respondEphemeral {
            content = "Cleared queue!"
        }
    }

    private suspend fun handleVoiceCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        val voiceCode = event.interaction.command.strings["voice"] ?: return
        val voice = Voice.fromCode(voiceCode)
        MemberConfig.setVoice(event.interaction.user.id.value.toLong(), voice)
        event.interaction.respondEphemeral {
            content = "Set voice to ${voice.desc}!"
        }
    }

    private suspend fun handleAboutCommand(guild: Guild, event: GuildChatInputCommandInteractionCreateEvent) {
        event.interaction.respondEphemeral {
            embed {
                title = "Ezrique Voice"
                color = Color(0xC33F3F)
                description = "Hello! I'm **Ezrique**, more specifically, **Ezrique Voice**. I was made to help you with your voice channels. I was made by **@deftu**."

                field {
                    name = "Version"
                    value = VERSION
                    inline = true
                }

                field {
                    name = "Guild Count"
                    value = "${kord.guilds.count()}"
                    inline = true
                }
            }
        }
    }
}
