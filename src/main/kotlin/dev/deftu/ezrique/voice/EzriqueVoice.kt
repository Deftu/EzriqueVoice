@file:OptIn(KordVoice::class)

package dev.deftu.ezrique.voice

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.deftu.ezrique.voice.sql.ChannelLinkTable
import dev.deftu.ezrique.voice.sql.MemberConfigTable
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
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
import java.io.File
import kotlin.system.exitProcess

const val NAME = "@PROJECT_NAME@"
const val VERSION = "@PROJECT_VERSION@"
val logger = LogManager.getLogger(NAME)
val database = Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC", databaseConfig = DatabaseConfig { useNestedTransactions = true })
val gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

lateinit var kord: Kord
    private set
var config: JsonObject? = null
    private set

suspend fun main() {
    logger.info("Starting $NAME v$VERSION")
    readConfig()
    kord = Kord(token())

    transaction {
        logger.info("Connected to database")
        SchemaUtils.createMissingTablesAndColumns(
            ChannelLinkTable,
            MemberConfigTable
        )
    }

    kord.on<ReadyEvent> {
        logger.info("Logged in as ${kord.getSelf().tag}")
    }

    kord.on<GuildCreateEvent> {
        kord.editPresence {
            listening("your voice channels in ${kord.guilds.count()} guilds")
        }
    }

    kord.on<GuildDeleteEvent> {
        kord.editPresence {
            listening("your voice channels in ${kord.guilds.count()} guilds")
        }
    }

    VoiceHandler.setup()
    TextToSpeechHandler.setup()
    CommandHandler.setup()

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

    val file = File(fileName)
    if (!file.exists()) {
        logger.error("Config file $fileName does not exist!")
        exitProcess(1)
    }

    val text = file.readText()
    val json = JsonParser.parseString(text)
    if (!json.isJsonObject) {
        logger.error("Config file $fileName is not a valid JSON object!")
        exitProcess(1)
    }

    config = json.asJsonObject
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
