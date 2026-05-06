package com.example.musicplayer.music_player_app.backend.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao

interface AlbumDao {
    @Query("SELECT * FROM albums")
    suspend fun getAllAlbums(): List<Album> // runs above query

    @Insert
    suspend fun insertAlbum(album: Album): Long // takes album object & returns long ID

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: Int): Album? // links to albumId in parameter

}