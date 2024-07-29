package dev.deftu.ezrique.voice

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
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
import dev.deftu.ezrique.voice.utils.scheduleAtFixedRate
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.gateway.ResumedEvent
import dev.kord.core.event.interaction.*
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.builder.PresenceBuilder
import io.sentry.Sentry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.count
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

object EzriqueVoice {

    const val NAME = "@PROJECT_NAME@"
    const val VERSION = "@PROJECT_VERSION@"
    private val LOGGER: Logger = LogManager.getLogger(NAME)
    val GSON: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val presenceScope = Dispatchers.Default + SupervisorJob()

    private val sentryUrl: String?
        get() {
            var sentryUrl = System.getenv("SENTRY_URL")
            if (sentryUrl == null || sentryUrl.isEmpty()) {
                sentryUrl = config.get("sentry_url")?.asString
            }

            return sentryUrl
        }

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

    private lateinit var kord: Kord

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        LOGGER.info("Starting $NAME v$VERSION")

        initializeSentry()
        if (!initializeDatabase()) return@runBlocking
        if (!initializeKord()) return@runBlocking

        setupHealthchecks()
        setupKordListeners()
        setupInteractionListeners()

        VoiceConnectionManager.setup(kord)
        OnboardingHandler.setup(kord)
        TtsHandler.setup(kord)
        MusicHandler.setup(kord)

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
            }
        } catch (e: Exception) {
            handleError(e, VoiceErrorCode.KORD_LOGIN)
        }
    }

    private fun initializeSentry() {
        LOGGER.info("Setting up Sentry")
        sentryUrl?.let { url -> setupSentry(url, NAME, VERSION) }
    }

    private suspend fun initializeDatabase(): Boolean {
        return try {
            LOGGER.info("Setting up database (url: '$dbUrl', passed url: 'jdbc:postgresql://$dbUrl', pass: '${dbPassword.take(4) + "*".repeat(dbPassword.length - 4)}')")
            Database.connect(
                driver = "org.postgresql.Driver",
                databaseConfig = DatabaseConfig {
                    useNestedTransactions = true
                },

                url = "jdbc:postgresql://$dbUrl",

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

            LOGGER.info("Connected to database")
            true
        } catch (e: Exception) {
            handleError(e, null)
            false // Don't continue if the database isn't set up
        }
    }

    private suspend fun initializeKord(): Boolean {
        return try {
            LOGGER.info("Setting up Kord")
            kord = Kord(token) {
                setup()
            }

            true
        } catch (e: Exception) {
            handleError(e, null)
            false // Don't continue if Kord isn't set up
        }
    }

    private fun setupHealthchecks() {
        kord.on<ReadyEvent> {
            if (isInDocker()) {
                LOGGER.info("Started healthcheck server")

                // Set up healthcheck HTTP server
                Healthchecks.start()
            }
        }

        kord.on<ResumedEvent> {
            if (isInDocker()) {
                LOGGER.info("Resumed connection to Discord - starting healthcheck server")

                // Set up healthcheck HTTP server
                Healthchecks.start()
            }
        }

        kord.on<DisconnectEvent> {
            if (isInDocker()) {
                LOGGER.info("Stopped healthcheck server - Kord disconnect")

                // Close healthcheck HTTP server
                Healthchecks.close()
            }
        }
    }

    private fun setupKordListeners() {
        /**
         * Responsible for printing the bot's tag to the console when it logs in
         */
        kord.on<ReadyEvent> {
            LOGGER.info("Logged in as ${kord.getSelf().tag}")

        }

        kord.on<ResumedEvent> {
            LOGGER.info("Resumed connection to Discord")
        }

        /**
         * Responsible for printing the bot's tag to the console when it logs out
         */
        kord.on<DisconnectEvent> {
            LOGGER.error("Disconnected from Discord")
        }

        /**
         * Responsible for managing the bot's presence
         */
        kord.on<ReadyEvent> {
            val possiblePresences = setOf<suspend PresenceBuilder.(kord: Kord) -> Unit>(
                { kord ->
                    // Guild count
                    var count = 0
                    kord.guilds.collect { count++ }

                    listening("your voice channels in about $count guilds")
                },
                { kord ->
                    // User count
                    var count = 0
                    kord.guilds.collect { guild ->
                        count += guild.memberCount ?: guild.members.count() // If we don't know the actual member count, just approximate it from the members we know of
                    }

                    watching("over approximately $count people's evil plans!")
                }
            )

            var lastPresenceIndex = 0

            CoroutineScope(presenceScope).launch {
                scheduleAtFixedRate(TimeUnit.SECONDS, 0, 30) {
                    runBlocking {
                        val presenceIndex = (lastPresenceIndex + 1) % possiblePresences.size
                        lastPresenceIndex = presenceIndex

                        val presence = possiblePresences.elementAt(presenceIndex)
                        kord.editPresence {
                            // Apply the rotated presence
                            presence(kord)

                            // Every presence change, flip between ONLINE and DND
                            status = if (presenceIndex % 2 == 0) PresenceStatus.Online else PresenceStatus.DoNotDisturb
                        }
                    }
                }
            }
        }
    }

    private fun setupInteractionListeners() {
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
                handleError(e, VoiceErrorCode.UNKNOWN_COMMAND)
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
                handleError(e, VoiceErrorCode.UNKNOWN_BUTTON)
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
                handleError(e, VoiceErrorCode.UNKNOWN_MODAL)
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
                handleError(e, VoiceErrorCode.UNKNOWN_SELECTION)
            }
        }
    }

}
