package de.martin_gutsche.campusdualstundenplan

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventMappingDao {
    @Query("SELECT * FROM eventMapping WHERE json=:json")
    fun get(json: String): EventMapping

    @Insert
    fun insert(mapping: EventMapping)

    @Delete
    fun delete(mapping: EventMapping)

    @Query("SELECT COUNT(*) FROM eventMapping")
    fun count(): Int
}
