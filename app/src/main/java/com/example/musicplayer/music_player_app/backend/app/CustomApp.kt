package com.example.musicplayer.music_player_app.backend.app

import android.app.Application
import com.example.musicplayer.music_player_app.backend.data.User

class CustomApp : Application() {
    // loginUser is deprecated in favor of SessionManager
    var loginUser: User? = null

    override fun onCreate() {
        super.onCreate()
        com.example.musicplayer.music_player_app.backend.data.SessionManager.init(this)
    }
}