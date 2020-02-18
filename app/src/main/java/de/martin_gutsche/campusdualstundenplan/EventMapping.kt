package de.martin_gutsche.campusdualstundenplan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EventMapping(
        @PrimaryKey val json: String,
        @ColumnInfo(name = "id") val id: String
)
