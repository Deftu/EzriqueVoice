package dev.deftu.ezrique.voice.utils

import dev.kord.common.entity.Snowflake

fun Snowflake.get(): Long =
    this.value.toLong()
