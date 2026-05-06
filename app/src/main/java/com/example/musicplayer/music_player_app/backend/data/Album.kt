package com.example.musicplayer.music_player_app.backend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")

data class Album (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val artist: String,
    val coverUri: String? = null
)

