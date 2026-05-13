package com.example.musicplayer.music_player_app.frontend.screens.library

import com.example.musicplayer.music_player_app.backend.data.Playlist

interface LibraryContract {
    interface View {
        fun showLoading()
        fun hideLoading()
        fun displayLibrary(playlists: List<PlaylistInfo>)
        fun showError(message: String)
        fun showAddPlaylistDialog()
    }

    interface Presenter {
        fun loadLibrary()
        fun onAddPlaylistClick()
        fun addPlaylist(name: String, coverUri: String)
        fun onDestroy()
    }

    interface Model {
        fun fetchPlaylists(callback: (List<PlaylistInfo>?, Throwable?) -> Unit)
        fun insertPlaylist(playlist: Playlist, callback: (Throwable?) -> Unit)
        fun deletePlaylists(playlists: List<Playlist>, callback: (Throwable?) -> Unit)
    }
}