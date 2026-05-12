package com.example.musicplayer.music_player_app.frontend.screens.library

import android.content.Context
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryModel(private val context: Context) : LibraryContract.Model {

    override fun fetchPlaylists(callback: (List<PlaylistInfo>?, Throwable?) -> Unit) {
        val playlistDao = AppDatabase.getDatabase(context).playlistDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val databasePlaylists = playlistDao.getAllPlaylists()

                val displayPlaylists = databasePlaylists.map { dbPlaylist ->
                    PlaylistInfo(
                        name = dbPlaylist.name,
                        songCount = 0,
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
}