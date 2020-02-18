package de.martin_gutsche.campusdualstundenplan

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EventMapping::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventMappingDao(): EventMappingDao
}