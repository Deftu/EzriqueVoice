package dev.deftu.ezrique.voice.utils

import dev.deftu.ezrique.ERROR_COLOR
import dev.deftu.ezrique.EmbedState
import dev.deftu.ezrique.stateEmbed
import dev.deftu.ezrique.voice.audio.GuildAudioManager
import dev.deftu.ezrique.voice.audio.scheduler.TrackScheduler
import dev.deftu.ezrique.voice.sql.GuildConfig
import dev.kord.common.Color
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.rest.builder.message.embed

suspend fun Guild?.checkGuild(
    response: DeferredEphemeralMessageInteractionResponseBehavior? = null
): Boolean {
    if (this == null) {
        response?.respond {
            stateEmbed(EmbedState.ERROR) {
                description = "This command can only be used in a server!"
            }
        }

        return false
    }

    return true
}

suspend fun Member.checkPermissions(
    permission: Permissions,
    response: DeferredEphemeralMessageInteractionResponseBehavior? = null
): Boolean {
    if (permissions?.contains(permission) == false) {
        response?.respond {
            embed {
                title = "Missing Permissions"
                color = Color(ERROR_COLOR)
                description = buildString {
                    append("You are missing the following permissions:\n")
                    for (value in permission.values) {
                        append("- ${value::class.java.simpleName}\n")
                    }
                }
            }
        }

        return false
    }

    return true
}

suspend fun Member.checkDj(
    guild: Guild,
    response: DeferredEphemeralMessageInteractionResponseBehavior? = null
): Boolean {
    val djRoleId = GuildConfig.getDjRole(guild.id.get()) ?: return true
    if (Snowflake(djRoleId) !in roleIds) {
        response?.respond {
            stateEmbed(EmbedState.ERROR) {
                description = "You are not this server's DJ!"
            }
        }

        return false
    }

    return true
}

suspend fun <T : TrackScheduler<*>> Map<Snowflake, GuildAudioManager<T>>.getPlayer(
    guildId: Long,
    response: DeferredEphemeralMessageInteractionResponseBehavior? = null
): GuildAudioManager<T>? {
    val player = this[Snowflake(guildId)]
    if (player == null && response != null) {
        response.respond {
            stateEmbed(EmbedState.ERROR) {
                title = "Not Connected"
                description = "The bot is not connected to a voice channel!"
            }
        }
    }

    return player
}
