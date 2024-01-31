package dev.deftu.ezrique.voice.utils

import dev.kord.core.entity.Guild
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder

interface CommandHandler {
    val name: String

    fun setup(builder: GlobalMultiApplicationCommandBuilder)
    suspend fun handle(event: ChatInputCommandInteractionCreateEvent, guild: Guild?, commandName: String, subCommandName: String?, groupName: String?)
}
