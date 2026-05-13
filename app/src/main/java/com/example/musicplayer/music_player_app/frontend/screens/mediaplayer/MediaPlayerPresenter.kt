package com.example.musicplayer.music_player_app.frontend.screens.mediaplayer

import android.os.Handler
import android.os.Looper
import com.example.musicplayer.music_player_app.backend.data.Playlist
import com.example.musicplayer.music_player_app.backend.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MediaPlayerPresenter(
    private var view: MediaPlayerContract.View?,
    private val musicService: MusicService,
    private val model: MediaPlayerContract.Model
) : MediaPlayerContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object: Runnable {
        override fun run() {
            val current = musicService.getCurrentPosition()
            val total = musicService.getDuration()
            view?.updateProgress(current, total)

            val currentSong = musicService.getCurrentSong()
            if (currentSong != null) {
                view?.updateSongInfo(currentSong.title, currentSong.artist)
            }

            view?.setPlayPauseIcon(musicService.isPlaying())
            handler.postDelayed(this, 1000)
        }
    }

    init {
        handler.post(updateProgressRunnable)
        loadInitialData()
    }

    private fun loadInitialData() {
        model.fetchPlaylists { playlists, error ->
            if (error != null) {
                // Handle error
            } else if (playlists != null && playlists.isNotEmpty()) {
                // view?.showPlaylistSelection(playlists)
            }
        }
    }

    override fun onPlayPauseClick() {
        musicService.pauseResume()
        view?.setPlayPauseIcon(musicService.isPlaying())
    }

    override fun onPreviousClick() {
        musicService.playPrevious()
    }

    override fun onNextClick() {
        musicService.playNext()
    }

    override fun onShuffleClick() {
        musicService.toggleShuffle()
        view?.setShuffleIcon(musicService.isShuffleEnabled())
    }

    override fun onAddSongToPlaylistClick() {
        view?.showAddSongDialog()
    }

    override fun onPlaylistSelected(playlist: Playlist) {
        model.fetchSongsForPlaylist(playlist.id) { songs, error ->
            if (error != null) {
                // Handle error
            } else if (songs != null && songs.isNotEmpty()) {
                musicService.setPlaylist(songs, 0)
            }
        }
    }

    override fun seekTo(position: Int) {
        musicService.seekTo(position)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateProgressRunnable)
        job.cancel()
        view = null
    }
}