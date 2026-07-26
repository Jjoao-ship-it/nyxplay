package com.jay.nyxplay.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MediaEntity::class, PlaylistEntity::class, PlaylistMediaCrossRef::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(MediaTypeConverters::class)
abstract class NyxDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: NyxDatabase? = null

        fun getInstance(context: Context): NyxDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NyxDatabase::class.java,
                    "nyxplay.db"
                )
                    // Fase de desenvolvimento, sem dados de produção a preservar —
                    // simplifica evolução de schema em vez de escrever migrations manuais.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
