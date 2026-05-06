package com.example.musicplayer.music_player_app.frontend.screens.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val fileUri: String,
    val playlistId: Int? = null,
    val albumId: Int? = null
)