package com.example.musicplayer.music_player_app.frontend.screens.library

import android.content.Context
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import com.example.musicplayer.music_player_app.backend.data.Playlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryModel(private val context: Context) : LibraryContract.Model {

    override fun fetchPlaylists(callback: (List<PlaylistInfo>?, Throwable?) -> Unit) {
        val database = AppDatabase.getDatabase(context)
        val playlistDao = database.playlistDao()
        val songDao = database.songDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val databasePlaylists = playlistDao.getAllPlaylists()

                val displayPlaylists = databasePlaylists.map { dbPlaylist ->
                    val songCount = songDao.getSongCountForPlaylist(dbPlaylist.id)
                    PlaylistInfo(
                        id = dbPlaylist.id,
                        name = dbPlaylist.name,
                        songCount = songCount,
                        coverUri = dbPlaylist.coverUri
                    )
                }

                withContext(Dispatchers.Main) {
                    callback(displayPlaylists, null)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null, exception)
                }
            }
        }
    }

    override fun insertPlaylist(playlist: Playlist, callback: (Throwable?) -> Unit) {
        val playlistDao = AppDatabase.getDatabase(context).playlistDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                playlistDao.insertPlaylist(playlist)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    callback(exception)
                }
            }
        }
    }

    override fun deletePlaylists(playlists: List<Playlist>, callback: (Throwable?) -> Unit) {
        val playlistDao = AppDatabase.getDatabase(context).playlistDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                playlistDao.deletePlaylists(playlists)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    callback(exception)
                }
            }
        }
    }
}