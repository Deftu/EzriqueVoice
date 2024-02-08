package dev.deftu.ezrique.voice.utils

import dev.kord.core.entity.Guild
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder

interface InteractionHandler {
    val name: String

    fun setupCommands(builder: GlobalMultiApplicationCommandBuilder) {
    }

    suspend fun handleCommand(event: ChatInputCommandInteractionCreateEvent, guild: Guild?, commandName: String, subCommandName: String?, groupName: String?) {
    }

    suspend fun handleButton(event: ButtonInteractionCreateEvent, guild: Guild?, customId: String) {
    }

    suspend fun handleSelectMenu(event: SelectMenuInteractionCreateEvent, guild: Guild?, customId: String) {
    }

    suspend fun handleModal(event: ModalSubmitInteractionCreateEvent, guild: Guild?, customId: String) {
    }

}
