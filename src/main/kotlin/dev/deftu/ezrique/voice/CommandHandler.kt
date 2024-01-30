package dev.deftu.ezrique.voice

import dev.deftu.ezrique.voice.sql.ChannelLink
import dev.deftu.ezrique.voice.sql.MemberConfig
import dev.deftu.ezrique.voice.tts.Voice
import dev.kord.common.Color
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.count

object CommandHandler {
    @OptIn(KordVoice::class)
    suspend fun setup() {
        kord.createGlobalApplicationCommands {
            input("join", "Joins the voice channel you're in.") {
                channel("channel", "The voice channel to join.") {
                    channelTypes = listOf(ChannelType.GuildVoice)
                }
            }

            input("leave", "Leaves the voice channel.")
            input("about", "Shows information about the bot.")

            TextToSpeechHandler.setupCommands(this)
        }

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            val commandName = interaction.command.rootName
            val guild = interaction.getGuildOrNull() ?: return@on
            val response = interaction.deferEphemeralResponse()

            when (commandName) {
                "join" -> {
                    var channel = interaction.command.channels["channel"]?.asChannelOf<VoiceChannel>()
                    if (channel == null || interaction.user.asMember().permissions?.contains(Permission.ManageChannels) == false) {
                        channel = interaction.user.getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel

                        if (channel == null) {
                            response.respond {
                                embed {
                                    title = "Not in a Voice Channel"
                                    color = Color(0x8C0317)
                                    description = "You have to be in a voice channel to use this command!"
                                }
                            }

                            return@on
                        }
                    }

                    if (channel.guildId != guild.id) {
                        response.respond {
                            embed {
                                title = "Not in a Voice Channel"
                                color = Color(0x8C0317)
                                description = "You have to be in a voice channel **in this server** to use this command!"
                            }
                        }

                        return@on
                    }

                    val connection = VoiceHandler.getVoiceConnection(guild.id)
                    if (connection != null) {
                        response.respond {
                            embed {
                                title = "Already in a Voice Channel"
                                color = Color(0x8C0317)
                                description = "I'm already in a voice channel! Use `/leave` to make me leave."
                            }
                        }

                        return@on
                    }

                    VoiceHandler.setVoiceChannel(guild.id, channel)
                    response.respond {
                        embed {
                            title = "Joined Voice Channel"
                            color = Color(0x038700)
                            description = "Joined ${channel.mention}!"
                        }
                    }
                }

                "leave" -> {
                    val channel = interaction.user.getVoiceStateOrNull()?.getChannelOrNull() as? VoiceChannel
                    var flag = true
                    if (channel == null) {
                        val member = interaction.user.asMember(guild.id)
                        if (member.permissions?.contains(Permission.ManageChannels) == true) {
                            flag = false
                        } else {
                            response.respond {
                                embed {
                                    title = "Not in a Voice Channel"
                                    color = Color(0x8C0317)
                                    description = "You have to be in a voice channel to use this command!"
                                }
                            }

                            return@on
                        }
                    }

                    if (channel?.guildId != guild.id && flag) {
                        response.respond {
                            embed {
                                title = "Not in a Voice Channel"
                                color = Color(0x8C0317)
                                description = "You have to be in a voice channel **in this server** to use this command!"
                            }
                        }

                        return@on
                    }

                    val didLeave = VoiceHandler.leaveVoiceChannel(guild.id)
                    response.respond {
                        embed {
                            title = "Left Voice Channel"
                            color = Color(0x8C0317)
                            description = if (didLeave) {
                                "Left ${channel?.mention ?: "the voice channel"}!"
                            } else {
                                "I'm not in a voice channel!"
                            }
                        }
                    }
                }

                "about" -> {
                    response.respond {
                        embed {
                            title = "Ezrique Voice"
                            color = Color(0xC33F3F)
                            description = "Hello! I'm **Ezrique**, more specifically, **Ezrique Voice**. I was made to help you with your voice channels. I was made by **@deftu**. If I'm not doing what you want, you can find help [in this Discord server](https://discord.gg/AzcAYDUru9)."

                            field {
                                name = "Version"
                                value = VERSION
                                inline = true
                            }

                            field {
                                name = "Guild Count"
                                value = "${dev.deftu.ezrique.voice.kord.guilds.count()}"
                                inline = true
                            }
                        }
                    }
                }
            }
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
}
