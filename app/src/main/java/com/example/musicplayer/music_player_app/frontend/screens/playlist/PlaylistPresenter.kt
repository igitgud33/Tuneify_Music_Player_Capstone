package com.example.musicplayer.music_player_app.frontend.screens.playlist

import kotlinx.coroutines.*

class PlaylistPresenter(
    private var view: PlaylistContract.View?,
    private val model: PlaylistContract.Model
) : PlaylistContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun loadSongs(playlistId: Int) {
        scope.launch {
            try {
                val songs = withContext(Dispatchers.IO) { model.getSongsByPlaylist(playlistId) }
                view?.showSongs(songs)
            } catch (e: Exception) {
                view?.showError(e.message ?: "Error loading songs")
            }
        }
    }

    override fun uploadNewSong(title: String, artist: String, fileUri: String, playlistId: Int) {
        scope.launch {
            try {
                val newSong = Song(title = title, artist = artist, fileUri = fileUri, playlistId = playlistId)
                withContext(Dispatchers.IO) { model.insertSong(newSong) }
                loadSongs(playlistId)
            } catch (e: Exception) {
                view?.showError(e.message ?: "Error uploading song")
            }
        }
    }

    override fun updateSong(song: Song) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { model.updateSong(song) }
                loadSongs(song.playlistId)
            } catch (e: Exception) {
                view?.showError(e.message ?: "Error updating song")
            }
        }
    }

    override fun removeSong(song: Song) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { model.deleteSong(song) }
                loadSongs(song.playlistId)
            } catch (e: Exception) {
                view?.showError(e.message ?: "Error deleting song")
            }
        }
    }

    override fun updatePlaylistName(playlistId: Int, newName: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { model.updatePlaylistName(playlistId, newName) }
            } catch (e: Exception) {
                view?.showError(e.message ?: "Error updating playlist name")
            }
        }
    }

    override fun updatePlaylistCover(playlistId: Int, newCoverUri: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { model.updatePlaylistCover(playlistId, newCoverUri) }
            } catch (e: Exception) {
                view?.showError(e.message ?: "Error updating playlist cover")
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}