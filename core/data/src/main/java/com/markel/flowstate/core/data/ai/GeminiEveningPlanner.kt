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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * [EveningPlanner] backed by the Gemini Developer API over plain REST.
 *
 * Chosen over the brand-new Gen AI Kotlin SDK (v1.0, September 2026) and
 * Firebase AI Logic deliberately: no new dependencies, one stable endpoint,
 * and the [EveningPlanner] seam means adopting either later is a one-class
 * swap. Structured output (responseSchema + application/json) guarantees the
 * response parses into an EveningPlan shape.
 *
 * Hardened by construction:
 *  - key comes from BuildConfig (local.properties, never committed); blank
 *    key -> offline planner, app fully functional without any setup
 *  - ANY failure (network, HTTP error, malformed response) logs and falls
 *    back to [LocalEveningPlanner], so the check-in flow can never break
 */
class GeminiEveningPlanner @Inject constructor(
    private val fallback: LocalEveningPlanner,
    private val planRepository: EveningPlanRepository
) : EveningPlanner {

    override suspend fun generatePlan(snapshot: CheckinSnapshot, feedback: PlanFeedback?): EveningPlan {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.i(TAG, "No gemini.api.key in local.properties — using LocalEveningPlanner")
            return fallback.generatePlan(snapshot)
        }
        return try {
            withContext(Dispatchers.IO) {
                // Durable memory: past regenerate notes, newest first. A DB
                // hiccup must never break planning — worst case we plan
                // without memory, exactly as before.
                val memory = runCatching { planRepository.recentFeedback(MEMORY_LIMIT) }
                    .getOrElse { emptyList() }
                requestPlan(apiKey, snapshot, feedback, memory)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini plan generation failed (${e.message}) — using LocalEveningPlanner", e)
            fallback.generatePlan(snapshot)
        }
    }

    // ── Request ────────────────────────────────────────────────────────────

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
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", apiKey)

            val body = buildRequestBody(snapshot, feedback, memory)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Gemini HTTP $code: ${text.take(300)}")

            return parsePlan(text, snapshot)
        } finally {
            conn.disconnect()
        }
    }

    private fun buildRequestBody(
        snapshot: CheckinSnapshot,
        feedback: PlanFeedback?,
        memory: List<PlanFeedbackNote>
    ): String = buildJsonObject {
        putJsonObject("systemInstruction") {
            putJsonArray("parts") {
                add(buildJsonObject { put("text", SYSTEM_PROMPT) })
            }
        }
        putJsonArray("contents") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", userText(snapshot, feedback, memory)) })
                }
            })
        }
        putJsonObject("generationConfig") {
            put("temperature", 0.7)
            put("responseMimeType", "application/json")
            putJsonObject("responseSchema") { planResponseSchema() }
        }
    }.toString()

    private fun JsonObjectBuilder.planResponseSchema() {
        // OpenAPI-style schema; uppercase types are what v1beta expects.
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("headline") { put("type", "STRING") }
            putJsonObject("blocks") {
                put("type", "ARRAY")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("startTime") { put("type", "STRING") }
                        putJsonObject("durationMinutes") { put("type", "INTEGER") }
                        putJsonObject("title") { put("type", "STRING") }
                        putJsonObject("reason") { put("type", "STRING") }
                        putJsonObject("kind") {
                            put("type", "STRING")
                            putJsonArray("enum") { PlanBlockKind.entries.forEach { add(it.name) } }
                        }
                        putJsonObject("referenceId") { put("type", "INTEGER") }
                    }
                    putJsonArray("required") {
                        add("startTime"); add("durationMinutes"); add("title"); add("reason"); add("kind")
                    }
                }
            }
        }
        putJsonArray("required") { add("headline"); add("blocks") }
    }

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
        val text = root["candidates"]!!.jsonArray[0]
            .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray[0]
            .jsonObject["text"]!!.jsonPrimitive.content
        val planJson = Json.parseToJsonElement(text).jsonObject

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

        if (blocks.isEmpty()) throw IllegalStateException("Gemini returned an empty plan")

        return EveningPlan(
            date = snapshot.date,
            generatedAtMillis = System.currentTimeMillis(),
            headline = planJson["headline"]?.jsonPrimitive?.contentOrNull
                ?: "Plan for this evening",
            blocks = blocks
        )
    }

    private companion object {
        const val TAG = "GeminiEveningPlanner"
        const val MODEL = "gemini-3.8-flash"
        /** How many durable correction notes ride along in each prompt. */
        const val MEMORY_LIMIT = 5
        val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

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
