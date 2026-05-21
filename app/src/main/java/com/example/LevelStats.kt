package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "level_stats")
data class LevelStats(
    @PrimaryKey val levelNumber: Int, // 1..50, or negative values for custom image sizes
    val gridSize: Int, // 3, 4, 5
    val bestTimeSeconds: Int,
    val bestMoves: Int,
    val timesCompleted: Int
)

@Dao
interface LevelStatsDao {
    @Query("SELECT * FROM level_stats")
    fun getAllStats(): Flow<List<LevelStats>>

    @Query("SELECT * FROM level_stats WHERE levelNumber = :levelNumber LIMIT 1")
    suspend fun getStatsForLevel(levelNumber: Int): LevelStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: LevelStats)
}

@Database(entities = [LevelStats::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelStatsDao(): LevelStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "azanpapu_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class LevelStatsRepository(private val dao: LevelStatsDao) {
    val allStats: Flow<List<LevelStats>> = dao.getAllStats()

    suspend fun recordCompletion(levelNumber: Int, gridSize: Int, timeSeconds: Int, moves: Int) {
        val existing = dao.getStatsForLevel(levelNumber)
        if (existing == null) {
            val stats = LevelStats(
                levelNumber = levelNumber,
                gridSize = gridSize,
                bestTimeSeconds = timeSeconds,
                bestMoves = moves,
                timesCompleted = 1
            )
            dao.insertOrUpdate(stats)
        } else {
            val updated = LevelStats(
                levelNumber = levelNumber,
                gridSize = gridSize,
                bestTimeSeconds = minOf(existing.bestTimeSeconds, timeSeconds),
                bestMoves = minOf(existing.bestMoves, moves),
                timesCompleted = existing.timesCompleted + 1
            )
            dao.insertOrUpdate(updated)
        }
    }
}
