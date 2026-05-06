package com.example.musicplayer.music_player_app.frontend.screens.album

import android.content.Context
import com.example.musicplayer.music_player_app.backend.data.Album
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumModel(context: Context): AlbumContract.Model {
    // get DAO from DB
    private val albumDao = AppDatabase.getDatabase(context).albumDao()
    override fun fetchAlbums(callback: (List<Album>?, Throwable?) -> Unit) {
       // use coroutine to run in bg (IO thread)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val albums = albumDao.getAllAlbums()
                // switch back to Main thread to send result to Presenter
                withContext(Dispatchers.Main) {
                    callback(albums, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null, e)
                }
            }
        }
    }

    override fun insertAlbum(
        album: Album,
        callback: (List<Album>?, Throwable?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                albumDao.insertAlbum(album)
                val updatedAlbums = albumDao.getAllAlbums() // fetch fresh list
                withContext(Dispatchers.Main) {
                    callback(updatedAlbums, null)  // now types match
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null, e)
                }
            }
        }
    }
}