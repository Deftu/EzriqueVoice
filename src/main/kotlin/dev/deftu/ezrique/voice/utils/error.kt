package dev.deftu.ezrique.voice.utils

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message

suspend fun handleError(
    response: DeferredEphemeralMessageInteractionResponseBehavior,
    code: ErrorCode,
    error: Throwable
): Message {
    return response.respond {
        errorEmbed {
            title = "An Error Occurred"
            color = Color(ERROR_COLOR)
            description = "An internal error has occurred! ||**[${code.ordinal}] [${error::class.java.simpleName}]** Report this in my support server! Found under `/about`||"
        }
    }.message
}

suspend fun handleError(
    message: Message,
    code: ErrorCode,
    error: Throwable
): Message {
    return message.reply {
        errorEmbed {
            title = "An Error Occurred"
            color = Color(ERROR_COLOR)
            description = "An internal error has occurred! ||**[${code.ordinal}] [${error::class.java.simpleName}]** Report this in my support server! Found under `/about`||"
        }
    }
}

enum class ErrorCode {
    SET_TTS_TOGGLE,
    SET_TTS_TOGGLE_GUILD,
    SET_TTS_VOICE,
    SET_TTS_LINK,
    SET_TTS_UNLINK,
    READ_TTS,
    SET_MUSIC_TOGGLE_GUILD,
    SET_MUSIC_DJ_ONLY_GUILD,
    SET_MUSIC_DJ_ROLE_GUILD,
    REMOVE_MUSIC_TRACK_GUILD,
    LOAD_AND_PLAY_MUSIC,
    SET_MUSIC_VOLUME_GUILD,
}
