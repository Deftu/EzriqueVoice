package dev.deftu.ezrique.voice.onboarding

import dev.deftu.ezrique.voice.EzriqueVoice
import dev.deftu.ezrique.voice.sql.GuildConfig
import dev.deftu.ezrique.voice.utils.*
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.entity.Guild
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed

object OnboardingInteractionHandler : InteractionHandler {

    private val messages = mutableMapOf(
        OnboardingStep.SETUP_FEATURES to "Welcome to the onboarding process! This will guide you through setting up ${EzriqueVoice.NAME} for your server. First up, we need to pick out which features you want to use.",
        OnboardingStep.TTS_SETUP to "You've chosen to use the text-to-speech features. You can now optionally decide to link a text channel to a voice channel for TTS messages, otherwise messages which are sent in the voice channel will be read out. Both channels' text contents can be read out at the same time, so you can have a dedicated TTS channel or use the voice channel's text channel.\nYou can link channels using the `/tts settings link` command.",
        OnboardingStep.MUSIC_SETUP to "Now that you've chosen to use music features, you can now optionally pick a DJ role and only allows DJs to use music commands.",
        OnboardingStep.DONE to "You're all set up! Enjoy using ${EzriqueVoice.NAME}!"
    )

    override val name = "onboarding"

    override fun setupCommands(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input(name, "Guides a server owner through the onboarding process.") {
            defaultMemberPermissions = Permissions(Permission.ManageGuild)
            dmPermission = false
        }
    }

    override suspend fun handleCommand(
        event: ChatInputCommandInteractionCreateEvent,
        guild: Guild?,
        commandName: String,
        subCommandName: String?,
        groupName: String?
    ) {
        val response = event.interaction.deferEphemeralResponse()
        if (
            commandName != name ||
            !guild.checkGuild(response)
        ) return

        val member = event.interaction.user.asMember(guild!!.id)
        if (!member.checkPermissions(Permissions(Permission.ManageGuild), response)) return

        val step = GuildConfig.getOnboardingStep(guild.id.get())
        response.respond {
            setForStep(guild, step)
        }
    }

    override suspend fun handleButton(event: ButtonInteractionCreateEvent, guild: Guild?, customId: String) {
        val member = event.interaction.user.asMember(guild!!.id)
        if (!member.checkPermissions(Permissions(Permission.ManageGuild))) return

        val realId = customId.removePrefix("${name}_")
        val isMusicEnabled = GuildConfig.isMusicEnabled(guild.id.get())
        val isTtsEnabled = GuildConfig.isTtsEnabled(guild.id.get())

        event.interaction.updateEphemeralMessage {
            when (realId) {
                "next" -> {
                    val step = GuildConfig.getOnboardingStep(guild.id.get())
                    val nextStep = step.nextStep(isTtsEnabled, isMusicEnabled)
                    GuildConfig.setOnboardingStep(guild.id.get(), nextStep)
                    components = mutableListOf()
                    setForStep(guild, nextStep)
                }

                "enable_tts", "enable_music" -> {
                    when (realId) {
                        "enable_tts" -> GuildConfig.setTtsEnabled(guild.id.get(), !isTtsEnabled)
                        "enable_music" -> GuildConfig.setMusicEnabled(guild.id.get(), !isMusicEnabled)
                    }

                    val newTtsEnabled = GuildConfig.isTtsEnabled(guild.id.get())
                    val newMusicEnabled = GuildConfig.isMusicEnabled(guild.id.get())

                    setForSetupFeatures(newTtsEnabled, newMusicEnabled)
                    setupEmbed(OnboardingStep.SETUP_FEATURES)
                }

                "music_setup", "music_dj_only" -> {
                    val isDjOnly = GuildConfig.isDjOnly(guild.id.get())
                    if (realId == "music_dj_only") GuildConfig.setDjOnly(guild.id.get(), !isDjOnly)

                    val newDjOnly = GuildConfig.isDjOnly(guild.id.get())
                    val djRole = GuildConfig.getDjRole(guild.id.get())

                    setForMusicSetup(newDjOnly, djRole)
                    setupEmbed(OnboardingStep.MUSIC_SETUP)
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    override suspend fun handleSelectMenu(event: SelectMenuInteractionCreateEvent, guild: Guild?, customId: String) {
        val member = event.interaction.user.asMember(guild!!.id)
        if (!member.checkPermissions(Permissions(Permission.ManageGuild))) return

        val realId = customId.removePrefix("${name}_")
        if (realId != "music_dj_role") return

        val role = event.interaction.values.map { it.toLongOrNull() }.firstOrNull()
        GuildConfig.setDjRole(guild.id.get(), role)
        event.interaction.deferEphemeralMessageUpdate()
    }

    override suspend fun handleModal(event: ModalSubmitInteractionCreateEvent, guild: Guild?, customId: String) {

    }

    private suspend fun MessageBuilder.setupEmbed(step: OnboardingStep) {
        embed {
            title = "Onboarding"
            color = Color(0xC33F3F)
            description = messages[step]
            footer {
                text = "Currently ${step.readableName}"
            }
        }
    }

    private suspend fun MessageBuilder.setForStep(guild: Guild, step: OnboardingStep) {
        when (step) {
            OnboardingStep.SETUP_FEATURES -> {
                val isMusicEnabled = GuildConfig.isMusicEnabled(guild.id.get())
                val isTtsEnabled = GuildConfig.isTtsEnabled(guild.id.get())
                setForSetupFeatures(isTtsEnabled, isMusicEnabled)
            }

            OnboardingStep.MUSIC_SETUP -> {
                val isDjOnly = GuildConfig.isDjOnly(guild.id.get())
                val djRole = GuildConfig.getDjRole(guild.id.get())
                setForMusicSetup(isDjOnly, djRole)
            }

            OnboardingStep.TTS_SETUP -> setForTtsSetup()
            else -> {  } // no-op
        }

        setupEmbed(step)
    }

    private fun MessageBuilder.setForSetupFeatures(
        isTtsEnabled: Boolean,
        isMusicEnabled: Boolean
    ) {
        actionRow {
            interactionButton(isTtsEnabled.toButtonStyle(), "onboarding_enable_tts") {
                label = "${if (isTtsEnabled) "Disable" else "Enable"} text-to-speech"
            }

            interactionButton(isMusicEnabled.toButtonStyle(), "onboarding_enable_music") {
                label = "${if (isMusicEnabled) "Disable" else "Enable"} music"
            }

            interactionButton(ButtonStyle.Secondary, "onboarding_next") {
                label = "Next"
            }
        }
    }

    private fun MessageBuilder.setForTtsSetup() {
        actionRow {
            interactionButton(ButtonStyle.Secondary, "onboarding_next") {
                label = "Next"
            }
        }
    }

    private fun MessageBuilder.setForMusicSetup(
        isDjOnly: Boolean,
        djRole: Long?
    ) {
        actionRow {
            interactionButton(isDjOnly.toButtonStyle(), "onboarding_music_dj_only") {
                label = "${if (isDjOnly) "Disable" else "Enable"} DJ only mode"
            }

            interactionButton(ButtonStyle.Secondary, "onboarding_next") {
                label = "Next"
            }
        }

        actionRow {
            roleSelect("onboarding_music_dj_role") {
                placeholder = "Select DJ role"
                allowedValues = 0..1
                if (djRole != null) {
                    defaultRoles.add(Snowflake(djRole))
                }
            }
        }
    }

}
