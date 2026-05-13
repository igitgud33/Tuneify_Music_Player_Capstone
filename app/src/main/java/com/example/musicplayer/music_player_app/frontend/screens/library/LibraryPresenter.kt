package com.example.musicplayer.music_player_app.frontend.screens.library

import com.example.musicplayer.music_player_app.backend.data.Playlist

class LibraryPresenter(
    private var view: LibraryContract.View?,
    private val model: LibraryContract.Model
) : LibraryContract.Presenter {

    override fun loadLibrary() {
        view?.showLoading()

        model.fetchPlaylists { playlists, error ->
            view?.hideLoading()

            if (error != null) {
                view?.showError(error.message ?: "An unknown error occurred while loading your library.")
            } else if (playlists != null) {
                view?.displayLibrary(playlists)
            }
        }
    }

    override fun onAddPlaylistClick() {
        view?.showAddPlaylistDialog()
    }

    override fun addPlaylist(name: String, coverUri: String) {
        val newPlaylist = Playlist(name = name, coverUri = coverUri)
        model.insertPlaylist(newPlaylist) { error ->
            if (error != null) {
                view?.showError("Failed to add playlist: ${error.message}")
            } else {
                loadLibrary()
            }
        }
    }

    fun deletePlaylists(playlistIds: List<Int>) {
        // Need full playlist objects for @Delete
        val playlistsToDelete = playlistIds.map { Playlist(id = it, name = "", coverUri = "") }
        model.deletePlaylists(playlistsToDelete) { error ->
            if (error != null) {
                view?.showError("Failed to delete playlists: ${error.message}")
            } else {
                loadLibrary()
            }
        }
    }

    override fun onDestroy() {
        view = null
    }
}