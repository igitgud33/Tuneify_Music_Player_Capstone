package com.example.musicplayer.music_player_app.frontend.screens.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.musicplayer.R
import com.example.musicplayer.music_player_app.backend.app.CustomApp
import com.example.musicplayer.music_player_app.frontend.screens.library.LibraryActivity
import com.example.musicplayer.music_player_app.frontend.screens.login.LoginActivity
import com.example.musicplayer.music_player_app.frontend.screens.mediaplayer.MediaPlayerActivity

class DashboardActivity : Activity(), DashboardContract.View {

    private lateinit var dashboardPresenter: DashboardPresenter
    private lateinit var textViewWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        textViewWelcome = findViewById(R.id.textviewUser)
        dashboardPresenter = DashboardPresenter(this, DashboardModel(application as CustomApp))
        dashboardPresenter.initializeUsername()

        val cardLibrary = findViewById<CardView>(R.id.cardLibrary)
        val cardPlayer = findViewById<CardView>(R.id.cardPlayer)
        val buttonBackToLogin = findViewById<Button>(R.id.buttonBackToLogin)

        cardLibrary.setOnClickListener {
            val intent = Intent(this, LibraryActivity::class.java)
            startActivity(intent)
        }

        cardPlayer.setOnClickListener {
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
        }

        buttonBackToLogin.setOnClickListener {
            com.example.musicplayer.music_player_app.backend.data.SessionManager.logout(this)
            // Removed STOP_SERVICE command to allow music to continue playing across login sessions

            val backToLoginIntent = Intent(this, LoginActivity::class.java)
            startActivity(backToLoginIntent)
            finish()
        }
    }

    override fun displayUsername(message: String) {
        textViewWelcome.text = message
    }
}