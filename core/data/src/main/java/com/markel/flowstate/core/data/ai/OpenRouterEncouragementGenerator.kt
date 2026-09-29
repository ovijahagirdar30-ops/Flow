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
 * [EncouragementGenerator] backed by OpenRouter over plain REST
 * (OpenAI-compatible chat/completions) — the same hardening as
 * OpenRouterEveningPlanner: a blank key (offline / unkeyed builds) or ANY
 * failure (network, HTTP, blank completion) logs and falls back to
 * [LocalEncouragementGenerator], so the night check-in can never stall on
 * or fail because of this call. Replaced GeminiEncouragementGenerator when
 * the Gemini project behind gemini.api.key was denied generateContent access.
 */
class OpenRouterEncouragementGenerator @Inject constructor(
    private val fallback: LocalEncouragementGenerator
) : EncouragementGenerator {

    override suspend fun generate(stats: DayReviewStats): String {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            Log.i(TAG, "No openrouter.api.key in local.properties — using LocalEncouragementGenerator")
            return fallback.generate(stats)
        }
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
            // Same reasoning latency as the planner (70-162s measured); 30s
            // here would time out every night-message request on device.
            conn.readTimeout = 300_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("X-Title", "FlowState")

            conn.outputStream.use { it.write(requestBody(stats).toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("OpenRouter HTTP $code: ${text.take(200)}")

            val message = parseMessage(text).trim()
            if (message.isEmpty()) throw IllegalStateException("OpenRouter returned an empty message")
            return message
        } finally {
            conn.disconnect()
        }
    }

    private fun requestBody(stats: DayReviewStats): String = buildJsonObject {
        put("model", BuildConfig.OPENROUTER_MODEL.ifBlank { DEFAULT_MODEL })
        putJsonArray("messages") {
            add(buildJsonObject {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            })
            add(buildJsonObject {
                put("role", "user")
                put("content", statsJson(stats))
            })
        }
        put("temperature", 0.9)
        // The message itself is ~40 tokens, but this model's hidden reasoning
        // counts against the cap too (280 of 300 measured) — headroom keeps a
        // stats-heavy evening from truncating into a visibly cut sentence.
        put("max_tokens", 1500)
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
        return root["choices"]!!.jsonArray[0]
            .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
    }

    private companion object {
        const val TAG = "OpenRouterNightMessage"
        const val DEFAULT_MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free"
        const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

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
