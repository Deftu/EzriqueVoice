import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version("2.0.0")
    val dgt = "2.13.0"
    id("dev.deftu.gradle.tools") version(dgt)
    id("dev.deftu.gradle.tools.bloom") version(dgt)
    id("dev.deftu.gradle.tools.shadow") version(dgt)
}

repositories {
    maven("https://maven.deftu.dev/internal-exposed")
    maven("https://maven.lavalink.dev/releases")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    shade(implementation("dev.deftu:ezrique-core:${libs.versions.ezrique.core.get()}")!!)

    // Audio management
    shade(implementation("dev.arbjerg:lavaplayer:${libs.versions.lavaplayer.get()}")!!)

    // Kord voice
    shade(implementation("dev.kord:kord-core-voice:${libs.versions.kord.get()}")!!)
    shade(implementation("dev.kord:kord-voice:${libs.versions.kord.get()}")!!)

    // Audio mixing
    shade(implementation("dev.deftu:pcm-audio-utils:${libs.versions.pcm.audio.utils.get()}")!!)

    // YouTube audio playback
    shade(implementation("dev.lavalink.youtube:common:${libs.versions.lavalink.youtube.get()}")!!)

}

tasks {
    jar {
        enabled = false
        manifest.attributes(
            "Main-Class" to "dev.deftu.ezrique.voice.EzriqueVoice"
        )
    }

    withType<ShadowJar> {
        archiveFileName.set("${projectData.name}.jar")
        archiveClassifier.set("")
    }
}
