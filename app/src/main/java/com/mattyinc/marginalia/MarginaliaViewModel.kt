package com.mattyinc.marginalia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mattyinc.marginalia.data.DayLog
import com.mattyinc.marginalia.data.Idea
import com.mattyinc.marginalia.data.MarginaliaDb
import com.mattyinc.marginalia.data.Stash
import com.mattyinc.marginalia.data.StashWithCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate

const val DAILY_GOAL = 5

data class Progress(val readToday: Int = 0, val streak: Int = 0, val goal: Int = DAILY_GOAL)

class MarginaliaViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MarginaliaDb.get(app).dao()

    /** Null until the first database read lands, so the UI can tell "loading" from "empty". */
    val feed: StateFlow<List<Idea>?> = dao.feed()
        .map<List<Idea>, List<Idea>?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val all: StateFlow<List<Idea>> = dao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stashes: StateFlow<List<StashWithCount>> = dao.stashes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val progress: StateFlow<Progress> = dao.days()
        .map { days -> progressFrom(days) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Progress())

    init {
        viewModelScope.launch { seedIfEmpty() }
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun progressFrom(days: List<DayLog>): Progress {
        val today = today()
        val byDay = days.associate { it.epochDay to it.readCount }
        val readToday = byDay[today] ?: 0
        // A streak survives until the end of today even if today's goal is not met yet.
        var cursor = if (readToday >= DAILY_GOAL) today else today - 1
        var streak = 0
        while ((byDay[cursor] ?: 0) >= DAILY_GOAL) {
            streak++
            cursor--
        }
        return Progress(readToday = readToday, streak = streak)
    }

    private suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.ideaCount() > 0) return@withContext
        val text = getApplication<Application>().assets.open("seed.json")
            .bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        val ideas = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Idea(
                title = o.getString("title"),
                body = o.getString("body"),
                source = o.getString("source"),
                topic = o.getString("topic"),
            )
        }
        // Shuffle once so the feed mixes topics, then keep that order.
        val shuffled = ideas.shuffled().mapIndexed { i, idea ->
            idea.copy(sort = i, createdAt = System.currentTimeMillis())
        }
        dao.insertIdeas(shuffled)
    }

    private suspend fun stashFor(name: String): Long {
        dao.stashByName(name)?.let { return it.id }
        return dao.insertStash(Stash(name = name, pigment = dao.stashCount() % 6))
    }

    private suspend fun countRead() {
        val day = today()
        val current = dao.day(day)?.readCount ?: 0
        dao.putDay(DayLog(day, current + 1))
    }

    /** Swiped right: keep the idea in the stash named after its topic. */
    fun keep(idea: Idea) = viewModelScope.launch(Dispatchers.IO) {
        dao.markSeen(idea.id, today(), stashFor(idea.topic))
        countRead()
    }

    /** Swiped left: read it, let it go. */
    fun pass(idea: Idea) = viewModelScope.launch(Dispatchers.IO) {
        dao.markSeen(idea.id, today(), null)
        countRead()
    }

    fun moveToStash(ideaId: Long, stashId: Long?) = viewModelScope.launch(Dispatchers.IO) {
        dao.setStash(ideaId, stashId)
    }

    fun setNote(ideaId: Long, note: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.setNote(ideaId, note)
    }

    fun delete(ideaId: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteIdea(ideaId)
    }

    fun returnPassedToFeed() = viewModelScope.launch(Dispatchers.IO) {
        dao.returnPassedToFeed()
    }

    /** An idea you wrote yourself goes straight into a stash, already read. */
    fun addOwn(title: String, body: String, source: String, stashName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val name = stashName.trim().ifEmpty { "Loose leaves" }
            dao.insertIdea(
                Idea(
                    title = title.trim(),
                    body = body.trim(),
                    source = source.trim(),
                    topic = name,
                    stashId = stashFor(name),
                    seenDay = today(),
                    own = true,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
}
