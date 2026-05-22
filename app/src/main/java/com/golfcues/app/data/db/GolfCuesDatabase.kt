package com.golfcues.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.golfcues.app.data.dao.GolfSessionDao
import com.golfcues.app.data.dao.SettingsDao
import com.golfcues.app.data.dao.ShotEventDao
import com.golfcues.app.data.entity.DetectionSettingsEntity
import com.golfcues.app.data.entity.GolfSessionEntity
import com.golfcues.app.data.entity.ShotEventEntity

@Database(
    entities = [
        GolfSessionEntity::class,
        ShotEventEntity::class,
        DetectionSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GolfCuesDatabase : RoomDatabase() {
    abstract fun golfSessionDao(): GolfSessionDao
    abstract fun shotEventDao(): ShotEventDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var instance: GolfCuesDatabase? = null

        fun getInstance(context: Context): GolfCuesDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GolfCuesDatabase::class.java,
                    "golfcues.db"
                ).build().also { instance = it }
            }
        }
    }
}
