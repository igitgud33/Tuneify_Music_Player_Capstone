package com.example.musicplayer.music_player_app.frontend.screens.playlist

import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistModel(private val database: AppDatabase) : PlaylistContract.Model {
    
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun fetchSongs(playlistId: Int, callback: (List<Song>?, Throwable?) -> Unit) {
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

    override fun insertSong(song: Song, callback: (Throwable?) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.songDao().insertSong(song)
                }
                callback(null)
            } catch (e: Exception) {
                callback(e)
            }
        }
    }

    override fun deleteSongs(songs: List<Song>, callback: (Throwable?) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.songDao().deleteSongs(songs)
                }
                callback(null)
            } catch (e: Exception) {
                callback(e)
            }
        }
    }

    override fun isSongInPlaylist(uri: String, playlistId: Int, callback: (Boolean) -> Unit) {
        scope.launch {
            val song = withContext(Dispatchers.IO) {
                database.songDao().getSongByUriAndPlaylist(uri, playlistId)
            }
            callback(song != null)
        }
    }
}