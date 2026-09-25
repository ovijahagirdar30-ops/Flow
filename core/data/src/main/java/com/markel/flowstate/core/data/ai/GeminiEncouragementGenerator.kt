package com.markel.flowstate.core.data.ai

import android.util.Log
import com.markel.flowstate.core.data.BuildConfig
import com.markel.flowstate.core.domain.DayReviewStats
import com.markel.flowstate.core.domain.EncouragementGenerator
import com.markel.flowstate.core.domain.LocalEncouragementGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * [EncouragementGenerator] backed by the Gemini Developer API over plain
 * REST — the same hardening as GeminiEveningPlanner: a blank key (offline /
 * unkeyed builds) or ANY failure (network, HTTP, blank completion) logs and
 * falls back to [LocalEncouragementGenerator], so the night check-in can
 * never stall on or fail because of this call.
 */
class GeminiEncouragementGenerator @Inject constructor(
    private val fallback: LocalEncouragementGenerator
) : EncouragementGenerator {

    override suspend fun generate(stats: DayReviewStats): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return fallback.generate(stats)
        return try {
            withContext(Dispatchers.IO) { requestMessage(apiKey, stats) }
        } catch (e: Exception) {
            Log.w(TAG, "Night message failed (${e.message}) — using local fallback", e)
            fallback.generate(stats)
        }
    }

    // ── Request ────────────────────────────────────────────────────────────

    private fun requestMessage(apiKey: String, stats: DayReviewStats): String {
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", apiKey)

            conn.outputStream.use { it.write(requestBody(stats).toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Gemini HTTP $code: ${text.take(200)}")

            val message = parseMessage(text).trim()
            if (message.isEmpty()) throw IllegalStateException("Gemini returned an empty message")
            return message
        } finally {
            conn.disconnect()
        }
    }

    private fun requestBody(stats: DayReviewStats): String = buildJsonObject {
        putJsonObject("systemInstruction") {
            putJsonArray("parts") {
                add(buildJsonObject { put("text", SYSTEM_PROMPT) })
            }
        }
        putJsonArray("contents") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", statsJson(stats)) })
                }
            })
        }
        putJsonObject("generationConfig") {
            put("temperature", 0.9)
            put("maxOutputTokens", 300)
        }
    }.toString()

    private fun statsJson(stats: DayReviewStats): String = buildJsonObject {
        put("date", stats.date)
        put("completedCount", stats.completedCount)
        putJsonArray("completedTitles") { stats.completedTitles.forEach { add(it) } }
        put("pendingCount", stats.pendingCount)
        put("pushedCount", stats.pushedCount)
    }.toString()

    // ── Response ───────────────────────────────────────────────────────────

    private fun parseMessage(responseBody: String): String {
        val root = Json.parseToJsonElement(responseBody).jsonObject
        val text = root["candidates"]!!.jsonArray[0]
            .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray[0]
            .jsonObject["text"]!!.jsonPrimitive.content
        return text
    }

    private companion object {
        const val TAG = "GeminiNightMessage"
        const val MODEL = "gemini-3.8-flash"
        val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

        val SYSTEM_PROMPT = """
            You write the closing message of Ovi's night check-in in FlowState,
            his personal task app. You get a small JSON with the date, how many
            tasks he finished today (plus up to five titles), how many are still
            pending, and how many were pushed to tomorrow.

            Write 2-3 short sentences: warm, matter-of-fact, specific to those
            numbers. Celebrate what got finished; treat unfinished work as
            neutral material for tomorrow — NEVER guilt, shame, obligations, or
            "don't forget" phrasing. Never mention AI, never ask questions.
            Output ONLY the message text: no quotes, no emoji, no heading, no list.
        """.trimIndent()
    }
}
