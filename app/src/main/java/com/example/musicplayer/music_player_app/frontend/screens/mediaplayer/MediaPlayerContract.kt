package com.example.musicplayer.music_player_app.frontend.screens.mediaplayer

import com.example.musicplayer.music_player_app.backend.data.Playlist
import com.example.musicplayer.music_player_app.frontend.screens.playlist.Song

class MediaPlayerContract {

    interface View {
        fun updateSongInfo(title: String, artist: String)
        fun setPlayPauseIcon(isPlaying: Boolean)
        fun updateProgress(currentMinSec: Int, durationMinSec: Int)
        fun showPlaylistSelection(playlists: List<Playlist>)
        fun setShuffleIcon(isShuffle: Boolean)
        fun showAddSongDialog()
    }

    interface Presenter {
        fun onPlayPauseClick()
        fun onPreviousClick()
        fun onNextClick()
        fun onShuffleClick()
        fun onAddSongToPlaylistClick()
        fun onPlaylistSelected(playlist: Playlist)
        fun seekTo(position: Int)
        fun onDestroy()
    }

    interface Model {
        fun fetchPlaylists(callback: (List<Playlist>?, Throwable?) -> Unit)
        fun fetchSongsForPlaylist(playlistId: Int, callback: (List<Song>?, Throwable?) -> Unit)
    }
}