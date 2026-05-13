package com.example.musicplayer.music_player_app.frontend.screens.playlist

interface PlaylistContract {
    interface View {
        fun showLoading()
        fun hideLoading()
        fun displayPlaylist(songs: List<Song>)
        fun showError(message: String)
        fun showAddSongDialog()
    }

    interface Presenter {
        fun loadPlaylist(playlistId: Int)
        fun onAddSongClick()
        fun addSong(title: String, artist: String, uri: String, playlistId: Int)
        fun onDestroy()
    }

    interface Model {
        fun fetchSongs(playlistId: Int, callback: (List<Song>?, Throwable?) -> Unit)
        fun insertSong(song: Song, callback: (Throwable?) -> Unit)
        fun deleteSongs(songs: List<Song>, callback: (Throwable?) -> Unit)
        fun isSongInPlaylist(uri: String, playlistId: Int, callback: (Boolean) -> Unit)
    }
}