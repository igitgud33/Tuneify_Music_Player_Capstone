package com.example.musicplayer.music_player_app.backend.data

data class PlaylistInfo(
    val id: Int,
    val name: String,
    val songCount: Int,
    val coverUri: String
)