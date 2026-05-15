package com.example.musicplayer.music_player_app.backend.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE userId = :userId")
    suspend fun getPlaylistsByUserId(userId: Int): List<Playlist>

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<Playlist>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylists(playlists: List<Playlist>)
    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Int, newName: String)

    @Query("UPDATE playlists SET coverUri = :newCoverUri WHERE id = :playlistId")
    suspend fun updatePlaylistCover(playlistId: Int, newCoverUri: String)
}