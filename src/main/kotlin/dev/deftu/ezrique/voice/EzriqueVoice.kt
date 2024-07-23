package dev.deftu.ezrique.voice

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import dev.deftu.ezrique.*
import dev.deftu.ezrique.voice.music.MusicInteractionHandler
import dev.deftu.ezrique.voice.music.MusicHandler
import dev.deftu.ezrique.voice.onboarding.OnboardingInteractionHandler
import dev.deftu.ezrique.voice.onboarding.OnboardingHandler
import dev.deftu.ezrique.voice.sql.ChannelLinkTable
import dev.deftu.ezrique.voice.sql.GuildConfigTable
import dev.deftu.ezrique.voice.sql.MemberConfigTable
import dev.deftu.ezrique.voice.tts.TtsHandler
import dev.deftu.ezrique.voice.tts.TtsInteractionHandler
import dev.deftu.ezrique.voice.utils.Healthchecks
import dev.deftu.ezrique.voice.utils.isInDocker
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.interaction.*
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import io.sentry.Sentry
import kotlinx.coroutines.flow.count
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

const val NAME = "@PROJECT_NAME@"
const val VERSION = "@PROJECT_VERSION@"
val logger = LogManager.getLogger(NAME)
val gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

private val dbUrl: String
    get() {
        var dbUrl = System.getenv("DATABASE_URL")
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = config.get("database_url")?.asString
            if (dbUrl == null || dbUrl.isEmpty()) error("No DB URL provided!")
        }

        return dbUrl
    }

private val dbPassword: String
    get() {
        var dbPassword = System.getenv("DATABASE_PASSWORD")
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = config.get("database_password")?.asString
            if (dbPassword == null || dbPassword.isEmpty()) error("No DB password provided!")
        }

        return dbPassword
    }

lateinit var kord: Kord
    private set

suspend fun main() {
    logger.info("Starting $NAME v$VERSION")

    logger.info("Setting up Sentry")
    Sentry.init { options ->
        options.dsn = "https://a8bef282a10087aea3e04095ad3e281e@o1228118.ingest.sentry.io/4506714187366400"
        options.release = "${NAME}@${VERSION}"
    }

    try {
        logger.info("Setting up database")
        Database.connect(
            driver = "org.postgresql.Driver",
            databaseConfig = DatabaseConfig {
                useNestedTransactions = true
            },

            url = dbUrl,

            user = "ezrique_voice",
            password = dbPassword
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                ChannelLinkTable,
                MemberConfigTable,
                GuildConfigTable
            )
        }

        logger.info("Connected to database")
    } catch (e: Exception) {
        handleError(e, null)
        return // Don't continue if the database isn't set up
    }

    try {
        logger.info("Setting up Kord")
        kord = Kord(token) {
            setup()
        }
    } catch (e: Exception) {
        handleError(e, null)
        return // Don't continue if Kord isn't set up
    }

    kord.on<ReadyEvent> {
        logger.info("Logged in as ${kord.getSelf().tag}")

        if (isInDocker()) {
            // Set up healthcheck HTTP server
            Healthchecks.start()
        }
    }

    kord.on<GuildCreateEvent> {
        kord.editPresence {
            listening("your voice channels in ${kord.guilds.count()} servers")
        }
    }

    kord.on<GuildDeleteEvent> {
        kord.editPresence {
            listening("your voice channels in ${kord.guilds.count()} servers")
        }
    }

    kord.on<ChatInputCommandInteractionCreateEvent> {
        try {
            val guild = maybeGetGuild()
            val (rootName, subCommandName, groupName) = interaction.command.names

            when (rootName) {
                OnboardingInteractionHandler.name -> OnboardingInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
                TtsInteractionHandler.name -> TtsInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
                MusicInteractionHandler.name -> MusicInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
                else -> BaseInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
            }
        } catch (e: Exception) {
            handleError(e, ErrorCode.UNKNOWN_COMMAND)
        }
    }

    kord.on<ButtonInteractionCreateEvent> {
        try {
            val guild = maybeGetGuild()

            when {
                interaction.componentId.startsWith(OnboardingInteractionHandler.name) -> OnboardingInteractionHandler.handleButton(this, guild, interaction.componentId)
                interaction.componentId.startsWith(TtsInteractionHandler.name) -> TtsInteractionHandler.handleButton(this, guild, interaction.componentId)
                interaction.componentId.startsWith(MusicInteractionHandler.name) -> MusicInteractionHandler.handleButton(this, guild, interaction.componentId)
                else -> BaseInteractionHandler.handleButton(this, guild, interaction.componentId)
            }
        } catch (e: Exception) {
            handleError(e, ErrorCode.UNKNOWN_BUTTON)
        }
    }

    kord.on<ModalSubmitInteractionCreateEvent> {
        try {
            val guild = maybeGetGuild()

            when {
                interaction.modalId.startsWith(OnboardingInteractionHandler.name) -> OnboardingInteractionHandler.handleModal(this, guild, interaction.modalId)
                interaction.modalId.startsWith(TtsInteractionHandler.name) -> TtsInteractionHandler.handleModal(this, guild, interaction.modalId)
                interaction.modalId.startsWith(MusicInteractionHandler.name) -> MusicInteractionHandler.handleModal(this, guild, interaction.modalId)
                else -> BaseInteractionHandler.handleModal(this, guild, interaction.modalId)
            }
        } catch (e: Exception) {
            handleError(e, ErrorCode.UNKNOWN_MODAL)
        }
    }

    kord.on<SelectMenuInteractionCreateEvent> {
        try {
            val guild = maybeGetGuild()

            when {
                interaction.componentId.startsWith(OnboardingInteractionHandler.name) -> OnboardingInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
                interaction.componentId.startsWith(TtsInteractionHandler.name) -> TtsInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
                interaction.componentId.startsWith(MusicInteractionHandler.name) -> MusicInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
                else -> BaseInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
            }
        } catch (e: Exception) {
            handleError(e, ErrorCode.UNKNOWN_SELECTION)
        }
    }

    OnboardingHandler.setup()
    VoiceHandler.setup()
    TtsHandler.setup()
    MusicHandler.initialize()

    kord.createGlobalApplicationCommands {
        BaseInteractionHandler.setupCommands(this)
        OnboardingInteractionHandler.setupCommands(this)
        TtsInteractionHandler.setupCommands(this)
        MusicInteractionHandler.setupCommands(this)
    }

    try {
        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents {
                +Intents.NON_PRIVILEGED
                +Intent.MessageContent
            }

            presence {
                status = PresenceStatus.DoNotDisturb
                listening("your voice channels in ${kord.guilds.count()} guilds")
            }
        }
    } catch (e: Exception) {
        handleError(e, ErrorCode.KORD_LOGIN)
    }
}
