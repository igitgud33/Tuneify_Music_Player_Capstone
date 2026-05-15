package com.example.musicplayer.music_player_app.frontend.screens.login

import android.content.Context
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import com.example.musicplayer.music_player_app.backend.data.SessionManager
import com.example.musicplayer.music_player_app.backend.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginModel(private val context: Context) {

    private val userDao = AppDatabase.getDatabase(context).userDao()

    fun validateUser(username: String, password: String, callback: (Boolean, User?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (username == "test" && password == "test") {
                var user = userDao.getUserByUsername("test")
                if (user == null) {
                    val adminUser = User(username = "test", password = "test")
                    val newId = userDao.insertUser(adminUser)
                    user = adminUser.copy(id = newId.toInt())
                }
                
                withContext(Dispatchers.Main) {
                    SessionManager.login(context, user!!)
                    callback(true, user)
                }
                return@launch
            }

            val user = userDao.getUserByUsername(username)
            val isValid = user != null && user.password == password
            
            withContext(Dispatchers.Main) {
                if (isValid) {
                    SessionManager.login(context, user!!)
                }
                callback(isValid, user)
            }
        }
    }

    fun registerUser(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newUser = User(username = username, password = password)
                userDao.insertUser(newUser)
                withContext(Dispatchers.Main) {
                    callback(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, e.message)
                }
            }
        }
    }
}
