package com.example.musicplayer.music_player_app.backend.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.musicplayer.music_player_app.frontend.screens.playlist.Song

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<Song>

    @Query("SELECT * FROM songs WHERE playlistId = :playlistId")
    suspend fun getSongsForPlaylist(playlistId: Int): List<Song>

    @Query("SELECT COUNT(*) FROM songs WHERE playlistId = :playlistId")
    suspend fun getSongCountForPlaylist(playlistId: Int): Int

    @Query("SELECT * FROM songs WHERE fileUri = :uri AND playlistId = :playlistId LIMIT 1")
    suspend fun getSongByUriAndPlaylist(uri: String, playlistId: Int): Song?

    @Insert
    suspend fun insertSong(song: Song)

    @Delete
    suspend fun deleteSongs(songs: List<Song>)
}