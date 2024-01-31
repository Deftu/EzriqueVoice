package dev.deftu.ezrique.voice.utils

import dev.deftu.ezrique.voice.audio.GuildPlayer
import dev.deftu.ezrique.voice.audio.TrackScheduler
import dev.deftu.ezrique.voice.sql.GuildConfig
import dev.kord.common.Color
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.embed

const val SUCCESS_COLOR = 0x038700
const val ERROR_COLOR = 0x8C0317

suspend fun Guild?.checkGuild(
    response: DeferredEphemeralMessageInteractionResponseBehavior? = null
): Boolean {
    if (this == null) {
        response?.respond {
            errorEmbed {
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
            errorEmbed {
                description = "You are not this server's DJ!"
            }
        }

        return false
    }

    return true
}

fun MessageBuilder.successEmbed(
    builder: EmbedBuilder.() -> Unit
) {
    embed {
        title = "Success!"
        color = Color(SUCCESS_COLOR)
        builder()
    }
}

fun MessageBuilder.errorEmbed(
    builder: EmbedBuilder.() -> Unit
) {
    embed {
        title = "Error!"
        color = Color(ERROR_COLOR)
        builder()
    }
}

suspend fun <T : TrackScheduler> Map<Snowflake, GuildPlayer<T>>.getPlayer(
    guildId: Long,
    response: DeferredEphemeralMessageInteractionResponseBehavior? = null
): GuildPlayer<T>? {
    val player = this[Snowflake(guildId)]
    if (player == null && response != null) {
        response.respond {
            embed {
                title = "Not Connected"
                color = Color(ERROR_COLOR)
                description = "The bot is not connected to a voice channel!"
            }
        }
    }

    return player
}
