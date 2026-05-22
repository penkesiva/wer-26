package com.golfcues.app.data.repository

import com.golfcues.app.data.dao.ShotEventDao
import com.golfcues.app.data.toDomain
import com.golfcues.app.data.toEntity
import com.golfcues.app.domain.model.ShotEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShotEventRepository(
    private val dao: ShotEventDao
) {
    fun observeEvents(sessionId: String): Flow<List<ShotEvent>> =
        dao.observeBySession(sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun insertEvent(event: ShotEvent) {
        dao.insert(event.toEntity())
    }

    suspend fun updateEvent(event: ShotEvent) {
        dao.update(event.toEntity())
    }

    suspend fun getEvent(eventId: String): ShotEvent? =
        dao.getById(eventId)?.toDomain()

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
