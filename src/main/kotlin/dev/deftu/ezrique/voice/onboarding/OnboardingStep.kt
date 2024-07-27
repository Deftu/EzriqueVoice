package dev.deftu.ezrique.voice.onboarding

enum class OnboardingStep(
    val readableName: String
) {

    SETUP_FEATURES("setting up features."),
    TTS_SETUP("setting up TTS."),
    MUSIC_SETUP("setting up music."),
    DONE("finished!");

    fun nextStep(
        hasTtsEnabled: Boolean,
        hasMusicEnabled: Boolean,
    ) = when (this) {
        SETUP_FEATURES -> if (hasTtsEnabled) TTS_SETUP else if (hasMusicEnabled) MUSIC_SETUP else DONE
        TTS_SETUP -> if (hasMusicEnabled) MUSIC_SETUP else DONE
        MUSIC_SETUP -> DONE
        DONE -> DONE
    }

}
