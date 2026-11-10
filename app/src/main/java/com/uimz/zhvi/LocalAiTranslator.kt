package com.uimz.zhvi

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig

/**
 * Optional, fully on-device Chinese/Vietnamese translation.
 * The model and user text stay on this device; no cloud AI endpoint is called.
 */
object LocalAiTranslator {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedModelPath: String? = null

    @Synchronized
    fun translate(context: Context, modelPath: String, sourceText: String, sourceChinese: Boolean): String {
        if (conversation == null || loadedModelPath != modelPath) {
            close()
            val newEngine = Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
            )
            // Model loading can take several seconds; caller must invoke this on a worker thread.
            newEngine.initialize()
            engine = newEngine
            conversation = newEngine.createConversation(
                ConversationConfig(maxOutputToken = 512)
            )
            loadedModelPath = modelPath
        }

        val prompt = if (sourceChinese) {
            """
            You are a professional Traditional Chinese to Vietnamese translator.
            Translate the user's text into natural, complete Vietnamese used in everyday life.
            Preserve every meaning, subject, object, negation, condition, tense, date, time,
            number, amount, unit, name and degree of emotion. Do not omit details or add facts.
            Preserve the original topic and proposition; do not replace a specific condition with a broader or different statement.
            Use neutral, general-public wording. Do not assume family or romantic relationships.
            Output only the Vietnamese translation. Do not explain or add quotation marks.

            Text to translate:
            $sourceText
            """.trimIndent()
        } else {
            """
            You are a professional Vietnamese to Traditional Chinese translator.
            Translate the user's text into natural Traditional Chinese as used in Taiwan.
            Preserve every meaning, subject, object, negation, condition, tense, date, time,
            number, amount, unit, name and degree of emotion. Do not omit details or add facts.
            Use neutral, general-public wording. Do not assume family or romantic relationships.
            Output only the Traditional Chinese translation. Do not explain or add quotation marks.

            Text to translate:
            $sourceText
            """.trimIndent()
        }

        val result = conversation!!.sendMessage(prompt).toString().trim()
        val cleaned = result
            .removePrefix("```").removeSuffix("```")
            .removeSurrounding("\"").removeSurrounding("'")
            .trim()
        if (cleaned.isBlank()) throw IllegalStateException("本機 AI 沒有產生有效翻譯。")
        return cleaned
    }

    @Synchronized
    fun close() {
        try { conversation?.close() } catch (_: Exception) { }
        conversation = null
        try { engine?.close() } catch (_: Exception) { }
        engine = null
        loadedModelPath = null
    }
}
