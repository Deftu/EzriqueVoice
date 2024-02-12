import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version("1.9.10")
    val dgt = "1.22.4"
    id("dev.deftu.gradle.tools") version(dgt)
    id("dev.deftu.gradle.tools.blossom") version(dgt)
    id("dev.deftu.gradle.tools.maven-publishing") version(dgt)
    id("dev.deftu.gradle.tools.shadow") version(dgt)
}

repositories {
    maven("https://maven.deftu.dev/internal") {
        fun getPublishingUsername(): String? {
            val property = project.findProperty("deftu.publishing.username")
            return property?.toString() ?: System.getenv("DEFTU_PUBLISHING_USERNAME")
        }

        fun getPublishingPassword(): String? {
            val property = project.findProperty("deftu.publishing.password")
            return property?.toString() ?: System.getenv("DEFTU_PUBLISHING_PASSWORD")
        }

        val publishingUsername = getPublishingUsername()
        val publishingPassword = getPublishingPassword()
        if (publishingUsername != null && publishingPassword != null) {
            credentials {
                username = publishingUsername
                password = publishingPassword
            }
        }
    }

    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    shade(implementation("dev.deftu:ezrique-core:${libs.versions.core.get()}")!!)
    shade(implementation("dev.kord:kord-core-voice:${libs.versions.kord.get()}")!!)
    shade(implementation("dev.kord:kord-voice:${libs.versions.kord.get()}")!!)
    shade(implementation("com.sedmelluq:lavaplayer:${libs.versions.lavaplayer.get()}")!!)
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
