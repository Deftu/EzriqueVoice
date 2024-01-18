plugins {
    java
    kotlin("jvm") version("1.9.10")
    val dgt = "1.22.2"
    id("dev.deftu.gradle.tools") version(dgt)
    id("dev.deftu.gradle.tools.blossom") version(dgt)
    id("dev.deftu.gradle.tools.maven-publishing") version(dgt)
    id("dev.deftu.gradle.tools.shadow") version(dgt)
    //id("dev.deftu.gradle.tools.discord.kord") version(dgt)
}

repositories {
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    // Discord
    shade(implementation("dev.kord:kord-core:${libs.versions.kord.get()}")!!)
    shade(implementation("dev.kord:kord-core-voice:${libs.versions.kord.get()}")!!)
    shade(implementation("dev.kord:kord-voice:${libs.versions.kord.get()}")!!)

    // Data handling
    shade(implementation("com.sedmelluq:lavaplayer:${libs.versions.lavaplayer.get()}")!!)
    shade(implementation("com.google.code.gson:gson:${libs.versions.gson.get()}")!!)
    shade(implementation("xyz.deftu:enhancedeventbus:${libs.versions.enhancedeventbus.get()}")!!)
    shade(implementation("com.squareup.okhttp3:okhttp:${libs.versions.okhttp.get()}")!!)

    // SQL
    shade(implementation("org.xerial:sqlite-jdbc:${libs.versions.sqlitejdbc.get()}")!!)
    shade(implementation("org.jetbrains.exposed:exposed-core:${libs.versions.exposed.get()}")!!)
    shade(implementation("org.jetbrains.exposed:exposed-dao:${libs.versions.exposed.get()}")!!)
    shade(implementation("org.jetbrains.exposed:exposed-jdbc:${libs.versions.exposed.get()}")!!)

    // Logging
    shade(implementation("org.apache.logging.log4j:log4j-api:${libs.versions.log4j.get()}")!!)
    shade(implementation("org.apache.logging.log4j:log4j-core:${libs.versions.log4j.get()}")!!)
    shade(implementation("org.apache.logging.log4j:log4j-slf4j-impl:${libs.versions.log4j.get()}")!!)
}
