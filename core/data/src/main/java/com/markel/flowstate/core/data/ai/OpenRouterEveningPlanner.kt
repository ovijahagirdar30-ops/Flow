package com.markel.flowstate.core.data.ai

import android.util.Log
import com.markel.flowstate.core.data.BuildConfig
import com.markel.flowstate.core.domain.CheckinSnapshot
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.EveningPlanner
import com.markel.flowstate.core.domain.LocalEveningPlanner
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import com.markel.flowstate.core.domain.PlanFeedback
import com.markel.flowstate.core.domain.PlanFeedbackNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * [EveningPlanner] backed by OpenRouter over plain REST (OpenAI-compatible
 * chat/completions). Replaced GeminiEveningPlanner when the Gemini project
 * behind gemini.api.key was denied generateContent access (403 "Your project
 * has been denied access") — OpenRouter's free models need no billing.
 *
 * The [EveningPlanner] seam is unchanged: swapping backends later (Gemini
 * once billing is sorted, Ollama, a hosted service) is again a one-class
 * swap in PlannerModule.
 *
 * Hardened exactly like its Gemini predecessor:
 *  - key comes from BuildConfig (local.properties, never committed); blank
 *    key -> offline planner, app fully functional without any setup
 *  - ANY failure (network, HTTP error, rate limit, malformed response) logs
 *    and falls back to [LocalEveningPlanner], so the check-in flow can never
 *    break — a Regenerate with a comment landing there is a no-op by design,
 *    which is why every failure is logged with the feedback flag
 */
class OpenRouterEveningPlanner @Inject constructor(
    private val fallback: LocalEveningPlanner,
    private val planRepository: EveningPlanRepository
) : EveningPlanner {

    override suspend fun generatePlan(snapshot: CheckinSnapshot, feedback: PlanFeedback?): EveningPlan {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            Log.i(TAG, "No openrouter.api.key in local.properties — using LocalEveningPlanner (feedback=${feedback != null})")
            return fallback.generatePlan(snapshot)
        }
        return try {
            withContext(Dispatchers.IO) {
                // Durable memory: past regenerate notes, newest first. A DB
                // hiccup must never break planning — worst case we plan
                // without memory, exactly as before.
                val memory = runCatching { planRepository.recentFeedback(MEMORY_LIMIT) }
                    .getOrElse { emptyList() }
                val started = System.currentTimeMillis()
                val plan = requestPlanWithRetry(apiKey, snapshot, feedback, memory)
                Log.i(
                    TAG,
                    "plan ok in ${System.currentTimeMillis() - started}ms — " +
                        "blocks=${plan.blocks.size}, feedback=${feedback != null}, memory=${memory.size}"
                )
                plan
            }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "OpenRouter plan generation failed (${e.message}) — using LocalEveningPlanner " +
                    "(feedback=${feedback != null})",
                e
            )
            fallback.generatePlan(snapshot)
        }
    }

    // ── Request ────────────────────────────────────────────────────────────

    /**
     * One automatic retry — the failures observed on device were a DNS blip
     * (6s) and garbage/non-JSON bodies, all transient, and each one silently
     * handed the user a plan that IGNORED their corrections. A read timeout
     * does NOT retry: we already sat through the full 300s budget, so a second
     * attempt would double the wait for the same slow model.
     */
    private suspend fun requestPlanWithRetry(
        apiKey: String,
        snapshot: CheckinSnapshot,
        feedback: PlanFeedback?,
        memory: List<PlanFeedbackNote>
    ): EveningPlan {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return requestPlan(apiKey, snapshot, feedback, memory)
            } catch (e: Exception) {
                if (e is SocketTimeoutException || attempt >= MAX_ATTEMPTS) throw e
                Log.w(TAG, "attempt $attempt/$MAX_ATTEMPTS failed (${e.message?.take(160)}) — retrying")
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    private fun requestPlan(
        apiKey: String,
        snapshot: CheckinSnapshot,
        feedback: PlanFeedback?,
        memory: List<PlanFeedbackNote>
    ): EveningPlan {
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            // NOT 30s: this reasoning model answers in 70-162s (reasoning
            // tokens are generated BEFORE the JSON). At 30s every device
            // request threw SocketTimeoutException and silently fell back to
            // LocalEveningPlanner — which IGNORES feedback — so each
            // Regenerate returned a plan that honored none of the corrections.
            // Even 120s was too tight for the slowest measured run (162s).
            conn.readTimeout = 300_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("X-Title", "FlowState")

            val body = buildRequestBody(snapshot, feedback, memory)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("OpenRouter HTTP $code: ${text.take(300)}")

            return try {
                parsePlan(text, snapshot)
            } catch (e: Exception) {
                // Parse failures need the context the HTTP-error path already
                // logs: status, content-type and what ACTUALLY came back (seen
                // on device: a 2xx whose body was raw model prose, not JSON).
                Log.w(TAG, "OpenRouter HTTP $code ct=${conn.contentType} unparseable body — ${text.take(400)}")
                throw e
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun buildRequestBody(
        snapshot: CheckinSnapshot,
        feedback: PlanFeedback?,
        memory: List<PlanFeedbackNote>
    ): String = buildJsonObject {
        put("model", BuildConfig.OPENROUTER_MODEL.ifBlank { DEFAULT_MODEL })
        putJsonArray("messages") {
            add(buildJsonObject {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            })
            add(buildJsonObject {
                put("role", "user")
                put("content", userText(snapshot, feedback, memory))
            })
        }
        put("temperature", 0.7)
        // Headroom over the 3167 completion tokens the heaviest measured run
        // used (2094 of them hidden reasoning): reasoning counts against
        // max_tokens, and truncation cuts the JSON mid-object → parse fails →
        // silent fallback to the offline planner, which ignores corrections.
        put("max_tokens", 8000)
    }.toString()

    /** Snapshot JSON, the durable PAST CORRECTIONS memory, plus a revise instruction when the user regenerated. */
    private fun userText(
        snapshot: CheckinSnapshot,
        feedback: PlanFeedback?,
        memory: List<PlanFeedbackNote>
    ): String = buildString {
        append(snapshotJson(snapshot))

        // Long-term memory: notes typed on earlier evenings. The current
        // session's note is excluded here — it travels in the REVISE block
        // below, and repeating it would only add noise. Chronological order
        // gives the newest correction the last word.
        val currentNote = feedback?.comment?.trim().orEmpty()
        val remembered = memory
            .filter { it.comment.isNotBlank() && !it.comment.equals(currentNote, ignoreCase = true) }
            .asReversed()
        if (remembered.isNotEmpty()) {
            append("\n\nPAST CORRECTIONS (typed by Ovi on earlier evenings):")
            remembered.forEach {
                append("\n- ").append(it.date).append(": \"").append(it.comment).append('"')
            }
        }

        if (feedback != null) {
            append("\n\nREVISE THE PREVIOUS PLAN.")
            if (feedback.comment.isNotBlank()) {
                append(" User's note: \"")
                append(feedback.comment.trim())
                append('"')
            }
            append(" Previous plan: ")
            append(planJson(feedback.previousPlan))
            append(" Produce a revised plan that honors the note while keeping what already works.")
        }
    }

    private fun planJson(plan: EveningPlan): String = buildJsonObject {
        put("headline", plan.headline)
        putJsonArray("blocks") {
            plan.blocks.forEach { block ->
                add(buildJsonObject {
                    put("startTime", block.startTime)
                    put("durationMinutes", block.durationMinutes)
                    put("title", block.title)
                    put("reason", block.reason)
                    put("kind", block.kind.name)
                    block.referenceId?.let { put("referenceId", it) }
                })
            }
        }
    }.toString()

    private fun snapshotJson(snapshot: CheckinSnapshot): String = buildJsonObject {
        put("date", snapshot.date)
        // Wall-clock time as the check-in finishes — the anchor the model
        // starts the plan from instead of a fixed evening hour.
        put("localTime", LocalTime.now().format(HH_MM))

        val checkin = snapshot.checkin
        if (checkin != null) {
            putJsonObject("checkin") {
                putJsonObject("mood") {
                    put("energy", checkin.mood.energy)
                    put("sleepiness", checkin.mood.sleepiness)
                    put("stress", checkin.mood.stress)
                    put("headache", checkin.mood.headache)
                    put("motivation", checkin.mood.motivation)
                    put("energyComment", checkin.mood.energyComment)
                    put("sleepinessComment", checkin.mood.sleepinessComment)
                    put("stressComment", checkin.mood.stressComment)
                    put("headacheComment", checkin.mood.headacheComment)
                    put("motivationComment", checkin.mood.motivationComment)
                }
                putJsonArray("unexpectedPlans") {
                    checkin.unexpectedPlans.forEach { plan ->
                        add(buildJsonObject {
                            put("description", plan.description)
                            put("startTime", plan.startTime)
                            put("durationMinutes", plan.durationMinutes)
                        })
                    }
                }
            }
        } else {
            put("checkin", JsonNull)
        }

        putJsonArray("tasks") {
            snapshot.tasks.forEach { task ->
                add(buildJsonObject {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("priority", task.priority.name)
                    task.dueDate?.let { put("dueDate", it) }
                })
            }
        }

        putJsonArray("habits") {
            snapshot.habits.forEach { habitWithStatus ->
                val habit = habitWithStatus.habit
                add(buildJsonObject {
                    put("id", habit.id)
                    put("name", habit.name)
                    put("type", habit.habitType.name)
                    put("isCompletedToday", habitWithStatus.isCompletedToday)
                    put("streak", habitWithStatus.streak)
                    habitWithStatus.todayValue?.let { put("todayValue", it) }
                    put("priorityRank", habit.priorityRank)
                    put("rolloverIfMissed", habit.rolloverIfMissed)
                })
            }
        }
    }.toString()

    // ── Response ───────────────────────────────────────────────────────────

    private fun parsePlan(responseBody: String, snapshot: CheckinSnapshot): EveningPlan {
        val root = Json.parseToJsonElement(responseBody).jsonObject
        // Descriptive failures instead of NPEs: the catch in generatePlan logs
        // e.message, and "null" told us nothing about WHY a 2xx body had no
        // plan in it (observed on device: a 2xx response without `choices`).
        val message = root["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?: throw IllegalStateException(
                "response has no choices/message: ${responseBody.take(300)}"
            )
        val text = message["content"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException(
                "empty content, body: ${responseBody.take(300)}"
            )
        val planJson = Json.parseToJsonElement(extractJsonObject(text)).jsonObject

        val blocks = planJson["blocks"]!!.jsonArray.map { element ->
            val obj = element.jsonObject
            PlanBlock(
                startTime = obj["startTime"]!!.jsonPrimitive.content,
                durationMinutes = obj["durationMinutes"]!!.jsonPrimitive.int,
                title = obj["title"]!!.jsonPrimitive.content,
                reason = obj["reason"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                kind = runCatching {
                    PlanBlockKind.valueOf(obj["kind"]!!.jsonPrimitive.content)
                }.getOrDefault(PlanBlockKind.OTHER),
                referenceId = obj["referenceId"]?.jsonPrimitive?.intOrNull
            )
        }.sortedBy { it.startTime }

        if (blocks.isEmpty()) throw IllegalStateException("OpenRouter returned an empty plan")

        return EveningPlan(
            date = snapshot.date,
            generatedAtMillis = System.currentTimeMillis(),
            headline = planJson["headline"]?.jsonPrimitive?.contentOrNull
                ?: "Plan for this evening",
            blocks = blocks
        )
    }

    /**
     * Free models are less disciplined about JSON than Gemini's responseSchema:
     * they may wrap the object in ```json fences or prepend a sentence. Strip
     * both; if there is still no object, let parsing throw -> local fallback.
     */
    private fun extractJsonObject(content: String): String {
        var text = content.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```").removePrefix("json").removeSuffix("```").trim()
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) text = text.substring(start, end + 1)
        return text
    }

    private companion object {
        const val TAG = "OpenRouterEveningPlanner"
        /** Free-tier model pinned here; override via local.properties (openrouter.model). */
        const val DEFAULT_MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free"
        /** How many durable correction notes ride along in each prompt. */
        const val MEMORY_LIMIT = 5
        /** Initial try + one retry for transient failures. */
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MILLIS = 1_500L
        val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

        val SYSTEM_PROMPT = """
            You are the evening transition assistant inside FlowState, Ovi's personal
            task/habit app. On arriving home after a long day, Ovi completes a quick
            check-in and you produce tonight's plan as a single JSON object.

            Input: a snapshot with the date and localTime (the wall-clock time
            the check-in just finished); today's check-in (0-10 scores for energy,
            sleepiness, stress, headache, motivation, a free-text comment on each, and
            any unexpected plans such as "dinner with family"); today's incomplete
            tasks (id, title, description, priority); and habits (id, name, type,
            whether completed today, streak, today's value, priorityRank 1-10 where
            higher matters more, rolloverIfMissed). A PAST CORRECTIONS section may
            follow the snapshot: durable notes Ovi typed on earlier evenings.

            Output rules:
            - Output ONLY the JSON object: no code fences, no commentary before or after.
            - headline: at most 8 words, specific to tonight's mood. Never guilt-trippy.
            - blocks: time-ordered, starting at localTime (the check-in JUST
              finished — begin the first block at or within ~10 minutes of it) and
              running until about 23:35 local time. Fields: startTime as
              zero-padded 24-hour "HH:mm" (e.g. "21:05"), durationMinutes, title
              (short), reason (max ~120 chars, warm and matter-of-fact), kind
              (TASK, HABIT, MEAL, REST, REFLECTION or OTHER), referenceId (the
              task/habit id for TASK/HABIT blocks; omit it otherwise).
            - REST durations are YOUR call from the mood scores — never a fixed
              length. Make the FIRST block a decompress REST right after the
              check-in: heavily drained (high sleepiness, low energy or high
              stress) earns up to an hour, a fine day only 10 minutes. Size the
              evening wind-down REST the same way (30-90 minutes).
            - Exactly one REFLECTION block near 23:00 for 30 minutes ("11PM ritual
              close"); if the check-in was already late, place it after your
              blocks instead of forcing the clock.
            - Run the plan until about 23:35: the last block must END near
              23:35 — never stop scheduling hours early.
            - One block per task/habit: never repeat a title or schedule the
              same id twice.
            - One MEAL block around 19:00, unless the check-in is already past
              dinner time or unexpected plans dictate otherwise.

            Behavior:
            - Reduce decisions: give ONE concrete plan, never options or questions.
            - PAST CORRECTIONS are durable facts from earlier evenings, never
              suggestions: honor every one that applies tonight, especially
              durations ("skincare is only 5 minutes"). Never schedule more
              time for an activity than its correction allows, and prefer the
              corrected activity length over any default you would assume.
            - If a "REVISE THE PREVIOUS PLAN" section follows the snapshot, treat the
              user's note as binding and revise that plan instead of re-rolling.
            - Adapt to mood: low energy or high stress -> fewer and easier tasks,
              more REST; high energy -> more tasks, hardest first.
            - Never induce guilt: never shame undone tasks or missed habits. If
              something doesn't fit, quietly leave it out.
            - Respect unexpected plans: give them a block (kind OTHER or MEAL) and
              schedule AROUND them; never overlap them.
            - Include unfinished habits as HABIT blocks when they fit, favoring high
              priorityRank; habits with rolloverIfMissed may be skipped freely.
            - Use ONLY the ids provided; never invent tasks or habits.
            - Keep block titles practical ("Finish slides", "Read 20 pages"), not
              motivational posters.
        """.trimIndent()
    }
}
