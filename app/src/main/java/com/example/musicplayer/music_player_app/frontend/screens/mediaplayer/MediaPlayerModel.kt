package com.example.musicplayer.music_player_app.frontend.screens.mediaplayer

import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import com.example.musicplayer.music_player_app.backend.data.Playlist
import com.example.musicplayer.music_player_app.frontend.screens.playlist.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPlayerModel(private val database: AppDatabase) : MediaPlayerContract.Model {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun fetchPlaylists(callback: (List<Playlist>?, Throwable?) -> Unit) {
        scope.launch {
            try {
                val playlists = withContext(Dispatchers.IO) {
                    database.playlistDao().getAllPlaylists()
                }
                callback(playlists, null)
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }

    override fun fetchSongsForPlaylist(playlistId: Int, callback: (List<Song>?, Throwable?) -> Unit) {
        scope.launch {
            try {
                val songs = withContext(Dispatchers.IO) {
                    database.songDao().getSongsForPlaylist(playlistId)
                }
                callback(songs, null)
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }
}