package com.example.musicplayer.music_player_app.frontend.screens.playlist

class PlaylistPresenter(
    private var view: PlaylistContract.View?,
    private val model: PlaylistContract.Model
) : PlaylistContract.Presenter {

    override fun loadPlaylist(playlistId: Int) {
        view?.showLoading()

        model.fetchSongs(playlistId) { songs, error ->
            view?.hideLoading()

            if (error != null) {
                view?.showError(error.message ?: "An unknown error occurred while loading the playlist.")
            } else if (songs != null) {
                view?.displayPlaylist(songs)
            }
        }
    }

    override fun onAddSongClick() {
        view?.showAddSongDialog()
    }

    override fun addSong(title: String, artist: String, uri: String, playlistId: Int) {
        // Check for duplicate
        model.isSongInPlaylist(uri, playlistId) { exists ->
            if (exists) {
                view?.showError("This song is already in the playlist")
            } else {
                val newSong = Song(title = title, artist = artist, fileUri = uri, playlistId = playlistId)
                model.insertSong(newSong) { error ->
                    if (error != null) {
                        view?.showError("Failed to add song: ${error.message}")
                    } else {
                        loadPlaylist(playlistId)
                    }
                }
            }
        }
    }

    fun deleteSongs(songs: List<Song>, playlistId: Int) {
        model.deleteSongs(songs) { error ->
            if (error != null) {
                view?.showError("Failed to delete songs: ${error.message}")
            } else {
                loadPlaylist(playlistId)
            }
        }
    }

    override fun onDestroy() {
        view = null
    }
}