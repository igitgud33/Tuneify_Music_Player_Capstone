package com.example.musicplayer.music_player_app.frontend.screens.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.musicplayer.R
import com.example.musicplayer.music_player_app.backend.app.CustomApp
import com.example.musicplayer.music_player_app.frontend.screens.dashboard.DashboardActivity
import com.example.musicplayer.music_player_app.frontend.screens.register.RegisterActivity
import com.example.musicplayer.music_player_app.frontend.utils.getButton
import com.example.musicplayer.music_player_app.frontend.utils.getEditTextValue
import com.example.musicplayer.music_player_app.frontend.utils.toast

class LoginActivity : Activity(), LoginContract.View {
    lateinit var Presenter: LoginPresenter

    var username = "";
    var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // AUTO-LOGIN: Check if user is already logged in
        if (com.example.musicplayer.music_player_app.backend.data.SessionManager.isLoggedIn()) {
            showHome()
            return
        }

        setContentView(R.layout.activity_login)

        Presenter = LoginPresenter(this, LoginModel(this))
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textviewRegister = findViewById<TextView>(R.id.textviewRegister)

        buttonLogin.setOnClickListener{
            username = getEditTextValue(R.id.edittextUsername)
            password = getEditTextValue(R.id.edittextPassword)

            Presenter.login(username, password)
        }

        textviewRegister.setOnClickListener{

            val registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
            finish()
        }
    }

    override fun showSuccess() {
        toast("Login successful!")
    }

    override fun showInvalidCredentials() {
        toast("Credentials do not match!")
    }

    override fun showEmptyFields() {
        toast("Username and Password cannot be empty!")
    }

    override fun showHome() {
        val dashboardIntent = Intent(this, DashboardActivity::class.java)

        startActivity(dashboardIntent)
        finish()
    }
}