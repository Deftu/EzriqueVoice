package dev.deftu.ezrique.voice.utils

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake

fun Snowflake.get(): Long =
    this.value.toLong()

fun Boolean.toButtonStyle(): ButtonStyle =
    if (this) ButtonStyle.Success else ButtonStyle.Danger
