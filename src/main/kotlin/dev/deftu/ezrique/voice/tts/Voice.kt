package dev.deftu.ezrique.voice.tts

enum class Voice(
    val code: String,
    val desc: String
) {
    // Disney Voices
    GHOST_FACE("en_us_ghostface", "Ghost Face (Scream)"),
    //CHEWBACCA("en_us_chewbacca", "Chewbacca (Star Wars) [non-intelligible]"),
    //C3PO("en_us_c3po", "C3PO (Star Wars)"),
    //STITCH("en_us_stitch", "Stitch (Lilo & Stitch)"),
    // Stormtrooper
    DEFAULT("en_us_stormtrooper", "Stormtrooper (Star Wars)"),
    //ROCKET("en_us_rocket", "Rocket (Guardians of the Galaxy)"),

    // English Voices
    METRO("en_au_001", "Metro"), // English AU - Female
    SMOOTH("en_au_002", "Smooth"), // English AU - Male
    NARRATOR("en_uk_001", "Narrator"), // English UK - Male 1
    UK_MALE("en_uk_003", "English UK - Male"),
    US_FEMALE("en_us_001", "English US - Female (Int. 1)"),
    JESSIE("en_us_002", "Jessie"), // English US - Female (Int. 2)
    JOEY("en_us_006", "Joey"), // English US - Male 1
    PROFESSOR("en_us_007", "Professor"), // English US - Male 2
    SCIENTIST("en_us_009", "Scientist"), // English US - Male 3
    CONFIDENCE("en_us_010", "Confidence"), // English US - Male 4

    // Europe Voices
    //FR_MALE1("fr_001", "French - Male 1"),
    //FR_MALE2("fr_002", "French - Male 2"),
    //DE_MALE1("de_001", "German - Female"),
    //DE_MALE2("de_002", "German - Male"),
    //ES_MALE("es_002", "Spanish - Male"),

    // America Voices
    //ES_MX_MALE("es_mx_002", "Spanish MX - Male"),
    //BR_FEMALE1("br_001", "Portuguese BR - Female 1"),
    //BR_FEMALE2("br_003", "Portuguese BR - Female 2"),
    //BR_FEMALE3("br_004", "Portuguese BR - Female 3"),
    //BR_MALE("br_005", "Portuguese BR - Male"),

    // Asia Voices
    //ID_FEMALE("id_001", "Indonesian - Female"),
    //JP_FEMALE1("jp_001", "Japanese - Female 1"),
    //JP_FEMALE2("jp_003", "Japanese - Female 2"),
    //JP_FEMALE3("jp_005", "Japanese - Female 3"),
    //JP_MALE("jp_006", "Japanese - Male"),
    //KR_MALE1("kr_002", "Korean - Male 1"),
    //KR_FEMALE("kr_003", "Korean - Female"),
    //KR_MALE2("kr_004", "Korean - Male 1"),

    // Singing Voices
    //COTTAGECORE("en_female_f08_salut_damour", "Cottagecore"),
    //JINGLE("en_male_m03_lobby", "Jingle"),
    //OPEN_MIC("en_female_f08_warmy_breeze", "Open Mic"),
    //TOON_BEAT("en_male_m03_sunshine_soon", "Toon Beat"),

    // Other
    STORRY_TELLER("en_male_narration", "Story Teller"),
    WACKY("en_male_funny", "Wacky"),
    PEACEFUL("en_female_emotional", "Peaceful"),

    TRICKSTER("en_male_grinch", "Trickster"),
    MAGICIAN("en_male_wizard", "Magician"),
    MADAME_LEOTA("en_female_madam_leota", "Madame Leota"),
    GHOST_HOST("en_male_ghosthost", "Ghost Host"),
    PIRATE("en_male_pirate", "Pirate"),
    EMPATHETIC("en_female_samc", "Empathetic"),
    SERIOUS("en_male_cody", "Serious"),
    WARM("es_mx_002", "Warm");

    companion object {
        @Suppress("EnumValuesSoftDeprecate")
        fun fromCode(code: String): Voice {
            return values().firstOrNull { it.code == code } ?: DEFAULT
        }
    }
}
