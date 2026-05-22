package com.golfcues.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.golfcues.app.data.entity.DetectionSettingsEntity
import com.golfcues.app.data.entity.GolfSessionEntity
import com.golfcues.app.data.entity.ShotEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GolfSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: GolfSessionEntity)

    @Update
    suspend fun update(session: GolfSessionEntity)

    @Query("SELECT * FROM golf_sessions ORDER BY startTimeMillis DESC")
    fun observeAll(): Flow<List<GolfSessionEntity>>

    @Query("SELECT * FROM golf_sessions WHERE id = :sessionId")
    fun observeById(sessionId: String): Flow<GolfSessionEntity?>

    @Query("SELECT * FROM golf_sessions WHERE endTimeMillis IS NULL LIMIT 1")
    suspend fun getActiveSession(): GolfSessionEntity?

    @Query("SELECT * FROM golf_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): GolfSessionEntity?

    @Query("DELETE FROM golf_sessions")
    suspend fun deleteAll()
}

@Dao
interface ShotEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ShotEventEntity)

    @Update
    suspend fun update(event: ShotEventEntity)

    @Query("SELECT * FROM shot_events WHERE sessionId = :sessionId ORDER BY timestampMillis DESC")
    fun observeBySession(sessionId: String): Flow<List<ShotEventEntity>>

    @Query("SELECT * FROM shot_events WHERE id = :eventId")
    suspend fun getById(eventId: String): ShotEventEntity?

    @Query("DELETE FROM shot_events")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: DetectionSettingsEntity)

    @Query("SELECT * FROM detection_settings WHERE id = 1")
    fun observe(): Flow<DetectionSettingsEntity?>

    @Query("SELECT * FROM detection_settings WHERE id = 1")
    suspend fun get(): DetectionSettingsEntity?
}
