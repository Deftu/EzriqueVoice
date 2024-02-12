package dev.deftu.ezrique.voice

import dev.deftu.ezrique.ErrorCode

enum class ErrorCode(
    private val _code: Int = -1
) : ErrorCode {
    UNKNOWN_ERROR,
    UNKNOWN_COMMAND_ERROR,
    UNKNOWN_BUTTON_ERROR,
    UNKNOWN_MODAL_ERROR,
    UNKNOWN_SELECTION_ERROR;

    override val code: Int
        get() = if (_code == -1) ordinal else _code
}
