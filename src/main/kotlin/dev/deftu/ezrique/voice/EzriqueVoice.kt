package dev.deftu.ezrique.voice

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.deftu.ezrique.voice.music.MusicInteractionHandler
import dev.deftu.ezrique.voice.music.MusicHandler
import dev.deftu.ezrique.voice.onboarding.OnboardingInteractionHandler
import dev.deftu.ezrique.voice.onboarding.OnboardingHandler
import dev.deftu.ezrique.voice.sql.ChannelLinkTable
import dev.deftu.ezrique.voice.sql.GuildConfigTable
import dev.deftu.ezrique.voice.sql.MemberConfigTable
import dev.deftu.ezrique.voice.tts.TtsHandler
import dev.deftu.ezrique.voice.tts.TtsInteractionHandler
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.*
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.interaction.*
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.flow.count
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.deftu.enhancedeventbus.bus
import xyz.deftu.enhancedeventbus.invokers.LMFInvoker
import java.io.File
import kotlin.system.exitProcess

const val NAME = "@PROJECT_NAME@"
const val VERSION = "@PROJECT_VERSION@"
val logger = LogManager.getLogger(NAME)
val database = Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC", databaseConfig = DatabaseConfig { useNestedTransactions = true })
val gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

private val token: String
    get() = token()

val eventBus = bus {
    invoker = LMFInvoker()
}

lateinit var kord: Kord
    private set
var config: JsonObject? = null
    private set

suspend fun main() {
    logger.info("Starting $NAME v$VERSION")
    readConfig()
    kord = Kord(token)

    transaction {
        logger.info("Connected to database")
        SchemaUtils.createMissingTablesAndColumns(
            ChannelLinkTable,
            MemberConfigTable,
            GuildConfigTable
        )
    }

    kord.on<ReadyEvent> {
        logger.info("Logged in as ${kord.getSelf().tag}")
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
            val guild = (interaction as? GuildChatInputCommandInteraction)?.getGuildOrNull()
            val command = interaction.command
            val rootName = command.rootName

            val subCommandName = when (command) {
                is RootCommand -> null
                is GroupCommand -> command.name
                is SubCommand -> command.name
            }

            val groupName = when (command) {
                is RootCommand, is SubCommand -> null
                is GroupCommand -> command.groupName
            }

            when (rootName) {
                OnboardingInteractionHandler.name -> OnboardingInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
                TtsInteractionHandler.name -> TtsInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
                MusicInteractionHandler.name -> MusicInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
                else -> BaseInteractionHandler.handleCommand(this, guild, rootName, subCommandName, groupName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    kord.on<ButtonInteractionCreateEvent> {
        try {
            val guild = (interaction as? GuildButtonInteraction)?.getGuildOrNull()

            when {
                interaction.componentId.startsWith(OnboardingInteractionHandler.name) -> OnboardingInteractionHandler.handleButton(this, guild, interaction.componentId)
                interaction.componentId.startsWith(TtsInteractionHandler.name) -> TtsInteractionHandler.handleButton(this, guild, interaction.componentId)
                interaction.componentId.startsWith(MusicInteractionHandler.name) -> MusicInteractionHandler.handleButton(this, guild, interaction.componentId)
                else -> BaseInteractionHandler.handleButton(this, guild, interaction.componentId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    kord.on<ModalSubmitInteractionCreateEvent> {
        try {
            val guild = (interaction as? GuildModalSubmitInteraction)?.getGuildOrNull()

            when {
                interaction.modalId.startsWith(OnboardingInteractionHandler.name) -> OnboardingInteractionHandler.handleModal(this, guild, interaction.modalId)
                interaction.modalId.startsWith(TtsInteractionHandler.name) -> TtsInteractionHandler.handleModal(this, guild, interaction.modalId)
                interaction.modalId.startsWith(MusicInteractionHandler.name) -> MusicInteractionHandler.handleModal(this, guild, interaction.modalId)
                else -> BaseInteractionHandler.handleModal(this, guild, interaction.modalId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    kord.on<SelectMenuInteractionCreateEvent> {
        try {
            val guild = (interaction as? GuildSelectMenuInteraction)?.getGuildOrNull()

            when {
                interaction.componentId.startsWith(OnboardingInteractionHandler.name) -> OnboardingInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
                interaction.componentId.startsWith(TtsInteractionHandler.name) -> TtsInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
                interaction.componentId.startsWith(MusicInteractionHandler.name) -> MusicInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
                else -> BaseInteractionHandler.handleSelectMenu(this, guild, interaction.componentId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
}

private fun readConfig() {
    var fileName = System.getenv("CONFIG")
    fileName = if (fileName == null || fileName.isEmpty()) {
        "config.json"
    } else if (!fileName.endsWith(".json")) "$fileName.json" else fileName

    logger.info("Reading config file $fileName")

    /**
     * Reads a JSON object from the file provided and sets it as the config.
     */
    fun readFrom(file: File) {
        val text = file.readText()
        val json = JsonParser.parseString(text)
        if (!json.isJsonObject) {
            logger.error("Config file $fileName is not a valid JSON object!")
            exitProcess(1)
        }

        config = json.asJsonObject
    }

    val file = File(fileName)
    if (!file.exists()) {
        logger.error("Config file $fileName does not exist!")
        exitProcess(1)
    }

    readFrom(file)
}

private fun token(): String {
    var token = System.getenv("TOKEN")
    if (token == null || token.isEmpty()) {
        token = config?.get("token")?.asString
        if (token == null || token.isEmpty()) {
            logger.error("No token provided!")
            exitProcess(1)
        }
    }

    return token
}
