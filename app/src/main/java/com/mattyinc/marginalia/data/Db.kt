package com.mattyinc.marginalia.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One short idea. `seenDay` is null until it has been read in the feed. */
@Entity(tableName = "ideas")
data class Idea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val source: String,
    val topic: String,
    val stashId: Long? = null,
    val seenDay: Long? = null,
    val note: String = "",
    val own: Boolean = false,
    val sort: Int = 0,
    val createdAt: Long = 0,
)

@Entity(tableName = "stashes")
data class Stash(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pigment: Int,
)

/** How many ideas were read on a given day (days since epoch, local time). */
@Entity(tableName = "days")
data class DayLog(
    @PrimaryKey val epochDay: Long,
    val readCount: Int,
)

data class StashWithCount(
    @Embedded val stash: Stash,
    val ideaCount: Int,
)

@Dao
interface MarginaliaDao {
    @Query("SELECT * FROM ideas WHERE seenDay IS NULL ORDER BY sort, id")
    fun feed(): Flow<List<Idea>>

    @Query("SELECT * FROM ideas WHERE stashId IS NOT NULL ORDER BY id DESC")
    fun kept(): Flow<List<Idea>>

    @Query("SELECT * FROM ideas ORDER BY id DESC")
    fun all(): Flow<List<Idea>>

    @Query(
        "SELECT s.*, (SELECT COUNT(*) FROM ideas i WHERE i.stashId = s.id) AS ideaCount " +
            "FROM stashes s ORDER BY s.id"
    )
    fun stashes(): Flow<List<StashWithCount>>

    @Query("SELECT * FROM days ORDER BY epochDay DESC LIMIT 400")
    fun days(): Flow<List<DayLog>>

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun ideaCount(): Int

    @Query("SELECT * FROM stashes WHERE name = :name LIMIT 1")
    suspend fun stashByName(name: String): Stash?

    @Query("SELECT COUNT(*) FROM stashes")
    suspend fun stashCount(): Int

    @Query("SELECT * FROM days WHERE epochDay = :day LIMIT 1")
    suspend fun day(day: Long): DayLog?

    @Insert
    suspend fun insertIdeas(ideas: List<Idea>)

    @Insert
    suspend fun insertIdea(idea: Idea): Long

    @Insert
    suspend fun insertStash(stash: Stash): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDay(day: DayLog)

    @Query("UPDATE ideas SET seenDay = :day, stashId = :stashId WHERE id = :id")
    suspend fun markSeen(id: Long, day: Long, stashId: Long?)

    @Query("UPDATE ideas SET stashId = :stashId WHERE id = :id")
    suspend fun setStash(id: Long, stashId: Long?)

    @Query("UPDATE ideas SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String)

    @Query("UPDATE ideas SET seenDay = NULL WHERE stashId IS NULL")
    suspend fun returnPassedToFeed()

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteIdea(id: Long)
}

@Database(entities = [Idea::class, Stash::class, DayLog::class], version = 1, exportSchema = false)
abstract class MarginaliaDb : RoomDatabase() {
    abstract fun dao(): MarginaliaDao

    companion object {
        @Volatile
        private var instance: MarginaliaDb? = null

        fun get(context: Context): MarginaliaDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MarginaliaDb::class.java,
                    "marginalia.db",
                ).build().also { instance = it }
            }
    }
}
