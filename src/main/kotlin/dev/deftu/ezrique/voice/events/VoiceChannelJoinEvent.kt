package dev.deftu.ezrique.voice.events

import dev.kord.common.entity.Snowflake

data class VoiceChannelJoinEvent(
    val guildId: Snowflake,
    val channelId: Snowflake
)
