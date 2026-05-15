package com.example.musicplayer.music_player_app.frontend.screens.playlist

interface PlaylistContract {
    interface View {
        fun showSongs(songs: List<Song>)
        fun showError(message: String)
    }

    interface Presenter {
        fun loadSongs(playlistId: Int)
        fun uploadNewSong(title: String, artist: String, fileUri: String, playlistId: Int)
        fun updateSong(song: Song)
        fun removeSong(song: Song)
        fun updatePlaylistName(playlistId: Int, newName: String)
        fun updatePlaylistCover(playlistId: Int, newCoverUri: String)
        fun onDestroy()
    }

    interface Model {
        suspend fun getSongsByPlaylist(playlistId: Int): List<Song>
        suspend fun insertSong(song: Song)
        suspend fun updateSong(song: Song)
        suspend fun deleteSong(song: Song)
        suspend fun updatePlaylistName(playlistId: Int, newName: String)
        suspend fun updatePlaylistCover(playlistId: Int, newCoverUri: String)
    }
}