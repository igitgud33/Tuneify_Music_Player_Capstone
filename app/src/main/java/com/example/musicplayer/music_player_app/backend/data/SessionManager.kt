package com.example.musicplayer.music_player_app.backend.data

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "MusicPlayerSession"
    private const val KEY_USER_ID = "userId"
    private const val KEY_USERNAME = "username"

    var currentUserId: Int = -1
    var currentUsername: String? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        currentUserId = prefs.getInt(KEY_USER_ID, -1)
        currentUsername = prefs.getString(KEY_USERNAME, null)
    }

    fun login(context: Context, user: User) {
        currentUserId = user.id
        currentUsername = user.username
        
        getPrefs(context).edit().apply {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            apply()
        }
    }

    fun logout(context: Context) {
        currentUserId = -1
        currentUsername = null
        
        getPrefs(context).edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = currentUserId != -1
}
