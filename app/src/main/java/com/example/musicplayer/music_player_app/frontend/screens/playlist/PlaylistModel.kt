package com.example.musicplayer.music_player_app.frontend.screens.playlist

import com.example.musicplayer.music_player_app.backend.data.PlaylistDao
import com.example.musicplayer.music_player_app.backend.data.SongDao

class PlaylistModel(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) : PlaylistContract.Model {

    override suspend fun getSongsByPlaylist(playlistId: Int): List<Song> {
        return songDao.getSongsByPlaylist(playlistId)
    }

    override suspend fun insertSong(song: Song) {
        songDao.insertSong(song)
    }

    override suspend fun updateSong(song: Song) {
        songDao.updateSong(song)
    }

    override suspend fun deleteSong(song: Song) {
        songDao.deleteSongs(listOf(song))
    }

    override suspend fun updatePlaylistName(playlistId: Int, newName: String) {
        playlistDao.updatePlaylistName(playlistId, newName)
    }

    override suspend fun updatePlaylistCover(playlistId: Int, newCoverUri: String) {
        playlistDao.updatePlaylistCover(playlistId, newCoverUri)
    }
}