package com.golfcues.app.data.repository

import com.golfcues.app.data.dao.GolfSessionDao
import com.golfcues.app.data.toDomain
import com.golfcues.app.data.toEntity
import com.golfcues.app.domain.model.GolfSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GolfSessionRepository(
    private val dao: GolfSessionDao
) {
    fun observeSessions(): Flow<List<GolfSession>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeSession(sessionId: String): Flow<GolfSession?> =
        dao.observeById(sessionId).map { it?.toDomain() }

    suspend fun getActiveSession(): GolfSession? =
        dao.getActiveSession()?.toDomain()

    suspend fun startSession(): GolfSession {
        val session = GolfSession(
            id = UUID.randomUUID().toString(),
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = null,
            durationMillis = null,
            totalProbableShots = 0,
            confirmedShots = 0,
            rejectedShots = 0,
            missedShots = 0
        )
        dao.insert(session.toEntity())
        return session
    }

    suspend fun endSession(sessionId: String) {
        val entity = dao.getById(sessionId) ?: return
        val endTime = System.currentTimeMillis()
        dao.update(
            entity.copy(
                endTimeMillis = endTime,
                durationMillis = endTime - entity.startTimeMillis
            )
        )
    }

    suspend fun endSessionDirect(session: GolfSession) {
        val endTime = System.currentTimeMillis()
        dao.update(
            session.toEntity().copy(
                endTimeMillis = endTime,
                durationMillis = endTime - session.startTimeMillis
            )
        )
    }

    suspend fun updateSession(session: GolfSession) {
        dao.update(session.toEntity())
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
