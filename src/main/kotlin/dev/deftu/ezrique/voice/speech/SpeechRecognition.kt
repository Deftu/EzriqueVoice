package dev.deftu.ezrique.voice.speech

import com.google.gson.JsonParser
import dev.deftu.ezrique.handleError
import org.vosk.Model
import org.vosk.Recognizer

object SpeechRecognition {

    private const val SAMPLE_RATE = 16_000

    private val recognizers = mutableSetOf<Recognizer>()

    suspend fun loadModel(path: String) {
        val recognizer = try {
            Recognizer(Model(path), SAMPLE_RATE.toFloat())
        } catch (t: Throwable) {
            handleError(t, null)
            return
        }

        recognizers.add(recognizer)
    }

    fun getText(data: ByteArray): String? {
        for (recognizer in recognizers) {
            if (recognizer.acceptWaveForm(data, data.size)) {
                val text = recognizer.readText()
                if (text != null) {
                    return text
                }
            } else {
                continue
            }
        }

        return null
    }

    private fun Recognizer.readText(): String? {
        val json = JsonParser.parseString(result) ?: return null
        val obj = json.asJsonObject
        return obj.get("text")?.asString
    }

}
