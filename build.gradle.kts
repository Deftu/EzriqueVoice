import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version("2.0.0")
    val dgt = "2.2.3"
    id("dev.deftu.gradle.tools") version(dgt)
    id("dev.deftu.gradle.tools.blossom") version(dgt)
    id("dev.deftu.gradle.tools.shadow") version(dgt)
}

repositories {
    maven("https://maven.deftu.dev/internal-exposed")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    shade(implementation("dev.deftu:ezrique-core:${libs.versions.core.get()}")!!)
    shade(implementation("dev.kord:kord-core-voice:${libs.versions.kord.get()}")!!)
    shade(implementation("dev.kord:kord-voice:${libs.versions.kord.get()}")!!)
    shade(implementation("com.sedmelluq:lavaplayer:${libs.versions.lavaplayer.get()}")!!)

    shade(implementation("io.ktor:ktor-server-core:${libs.versions.ktor.get()}")!!)
    shade(implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")!!)
}

tasks {
    jar {
        enabled = false
        manifest.attributes(
            "Main-Class" to "dev.deftu.ezrique.voice.EzriqueVoiceKt"
        )
    }

    withType<ShadowJar> {
        archiveFileName.set("${projectData.name}.jar")
        archiveClassifier.set("")
    }
}
