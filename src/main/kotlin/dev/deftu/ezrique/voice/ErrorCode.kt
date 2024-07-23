package dev.deftu.ezrique.voice

import dev.deftu.ezrique.ErrorCode

enum class ErrorCode(
    private val _code: Int = -1
) : ErrorCode {

    UNKNOWN,

    UNKNOWN_COMMAND,
    UNKNOWN_BUTTON,
    UNKNOWN_MODAL,
    UNKNOWN_SELECTION,

    DATABASE_CONNECTION,
    KORD_SETUP,
    KORD_LOGIN;

    override val code: Int
        get() = if (_code == -1) ordinal else _code

}
