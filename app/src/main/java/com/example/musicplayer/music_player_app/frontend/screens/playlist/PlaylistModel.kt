package com.example.musicplayer.music_player_app.frontend.screens.playlist

class PlaylistModel : PlaylistContract.Model {
    override fun fetchSongs(callback: (List<Song>?, Throwable?) -> Unit) {
        val mockSongs = listOf(
            Song(title = "test", artist = "test", fileUri = ""),
            Song(title = "test", artist = "test", fileUri = ""),
            Song(title = "test", artist = "test", fileUri = "")
        )

        callback(mockSongs, null)
    }
}