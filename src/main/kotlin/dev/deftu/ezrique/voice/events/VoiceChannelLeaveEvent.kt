package dev.deftu.ezrique.voice.events

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.Event

data class VoiceChannelLeaveEvent @KordPreview constructor(
    override val kord: Kord,
    @KordPreview override val customContext: Any?,
    override val shard: Int,
    val guildId: Snowflake
) : Event
