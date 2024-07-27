package dev.deftu.ezrique.voice

import dev.deftu.ezrique.ErrorCode

enum class VoiceErrorCode(
    override val code: Int
) : ErrorCode {

    UNKNOWN(1),

    UNKNOWN_COMMAND(2),
    UNKNOWN_BUTTON(3),
    UNKNOWN_MODAL(4),
    UNKNOWN_SELECTION(5),

    DATABASE_CONNECTION(6),
    KORD_SETUP(7),
    KORD_LOGIN(8),

    // TTS
    SET_TTS_TOGGLE(9),
    SET_TTS_TOGGLE_GUILD(10),
    SET_TTS_VOICE(11),
    SET_TTS_LINK(12),
    SET_TTS_UNLINK(13),
    READ_TTS(14),
    TTS_LOAD_FAILED(15),

    // Music
    SET_MUSIC_TOGGLE_GUILD(16),
    SET_MUSIC_DJ_ONLY_GUILD(17),
    SET_MUSIC_DJ_ROLE_GUILD(18),
    REMOVE_MUSIC_TRACK_GUILD(19),
    LOAD_AND_PLAY_MUSIC(20),
    SET_MUSIC_VOLUME_GUILD(21),;

}
