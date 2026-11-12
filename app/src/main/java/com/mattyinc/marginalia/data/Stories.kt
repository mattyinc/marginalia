package com.mattyinc.marginalia.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Mark(val label: String, val note: String)

data class Bar(val label: String, val value: Float, val display: String)

/**
 * One screen of a story. `say` is both the caption and the narration; `kind`
 * picks the drawing that plays above it, and the other fields feed that drawing.
 */
data class Scene(
    val kind: String,
    val say: String,
    val words: String = "",
    val from: Int = 0,
    val to: Int = 0,
    val prefix: String = "",
    val suffix: String = "",
    val plain: Boolean = false,
    val label: String = "",
    val marks: List<Mark> = emptyList(),
    val bars: List<Bar> = emptyList(),
    val values: List<Float> = emptyList(),
    val startLabel: String = "",
    val endLabel: String = "",
    val items: List<String> = emptyList(),
) {
    /** Long enough to read the caption at an easy pace. */
    val durationMs: Long
        get() = (say.split(' ').size * 340L + 1800L).coerceAtLeast(4500L)
}

data class Story(
    val id: String,
    val title: String,
    val topic: String,
    val takeaway: String,
    val scenes: List<Scene>,
) {
    /** The scenes as played: the story, then the one idea to keep. */
    val played: List<Scene> = scenes + Scene(kind = "takeaway", say = takeaway)

    val minutes: Int
        get() = ((played.sumOf { it.durationMs } + 30_000L) / 60_000L).toInt().coerceAtLeast(1)
}

private fun JSONObject.list(name: String): List<JSONObject> {
    val array: JSONArray = optJSONArray(name) ?: return emptyList()
    return (0 until array.length()).map { array.getJSONObject(it) }
}

fun loadStories(context: Context): List<Story> {
    val text = context.assets.open("stories.json").bufferedReader().use { it.readText() }
    val array = JSONArray(text)
    return (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Story(
            id = o.getString("id"),
            title = o.getString("title"),
            topic = o.getString("topic"),
            takeaway = o.getString("takeaway"),
            scenes = o.list("scenes").map { s ->
                val values = s.optJSONArray("values")
                val items = s.optJSONArray("items")
                Scene(
                    kind = s.getString("kind"),
                    say = s.getString("say"),
                    words = s.optString("words"),
                    from = s.optInt("from"),
                    to = s.optInt("to"),
                    prefix = s.optString("prefix"),
                    suffix = s.optString("suffix"),
                    plain = s.optBoolean("plain"),
                    label = s.optString("label"),
                    marks = s.list("marks").map { Mark(it.getString("label"), it.getString("note")) },
                    bars = s.list("bars").map {
                        Bar(it.getString("label"), it.getDouble("value").toFloat(), it.getString("display"))
                    },
                    values = if (values == null) emptyList()
                    else (0 until values.length()).map { values.getDouble(it).toFloat() },
                    startLabel = s.optString("startLabel"),
                    endLabel = s.optString("endLabel"),
                    items = if (items == null) emptyList()
                    else (0 until items.length()).map { items.getString(it) },
                )
            },
        )
    }
}
