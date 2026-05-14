package com.example.musicplayer.music_player_app.frontend.screens.mediaplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.R
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import com.example.musicplayer.music_player_app.backend.data.Playlist
import com.example.musicplayer.music_player_app.backend.service.MusicService

class MediaPlayerActivity: AppCompatActivity(), MediaPlayerContract.View {
    private lateinit var presenter: MediaPlayerContract.Presenter
    private var musicService: MusicService? = null
    private lateinit var seekBar: SeekBar
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            val database = AppDatabase.getDatabase(this@MediaPlayerActivity)
            val model = MediaPlayerModel(database)
            presenter = MediaPlayerPresenter(this@MediaPlayerActivity, musicService!!, model)

            setupUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        seekBar = findViewById(R.id.seekBarPlayer)

        val intent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun setupUI() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) presenter.seekTo(progress)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        findViewById<ImageButton>(R.id.btnPlayPause).setOnClickListener {
            presenter.onPlayPauseClick()
        }

        findViewById<ImageButton>(R.id.btnPrevSong).setOnClickListener {
            presenter.onPreviousClick()
        }

        findViewById<ImageButton>(R.id.btnNextSong).setOnClickListener {
            presenter.onNextClick()
        }

        findViewById<ImageButton>(R.id.btnShuffle).setOnClickListener {
            presenter.onShuffleClick()
        }

        findViewById<TextView>(R.id.textSelectPlaylist).setOnClickListener {
            showPlaylistMenu()
        }

        findViewById<TextView>(R.id.textPlayerArtist).setOnClickListener {
            showAddSongDialog()
        }
    }

    private fun showPlaylistMenu() {
        val anchor = findViewById<TextView>(R.id.textSelectPlaylist)
        val popup = PopupMenu(this, anchor)
        
        val database = AppDatabase.getDatabase(this)
        val model = MediaPlayerModel(database)
        
        model.fetchPlaylists { playlists, error ->
            if (error != null) {
                // Handle error
            } else if (playlists != null) {
                if (playlists.isEmpty()) {
                    popup.menu.add("No playlists found")
                } else {
                    playlists.forEach { playlist ->
                        popup.menu.add(playlist.name)
                    }
                }
                popup.setOnMenuItemClickListener { item ->
                    val selected = playlists.find { it.name == item.title }
                    selected?.let {
                        presenter.onPlaylistSelected(it)
                        anchor.text = it.name
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun showAddSongDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Song")
        builder.setMessage("Go to a playlist to add songs.")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    override fun showPlaylistSelection(playlists: List<Playlist>) {}

    override fun updatePlaybackMode(mode: MusicService.PlaybackMode) {
        val btn = findViewById<ImageButton>(R.id.btnShuffle)
        val message = when (mode) {
            MusicService.PlaybackMode.NORMAL -> {
                btn.alpha = 0.5f
                "Mode: Normal"
            }
            MusicService.PlaybackMode.SHUFFLE -> {
                btn.alpha = 1.0f
                "Mode: Shuffle"
            }
            MusicService.PlaybackMode.RANDOM -> {
                btn.alpha = 1.0f
                "Mode: Random"
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun updateSongInfo(title: String, artist: String) {
        findViewById<TextView>(R.id.textPlayerTitle).text = title
        findViewById<TextView>(R.id.textPlayerArtist).text = artist
    }

    override fun setPlayPauseIcon(isPlaying: Boolean) {
        val playPauseBtn = findViewById<ImageButton>(R.id.btnPlayPause)
        playPauseBtn.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    override fun updateProgress(currentMinSec: Int, durationMinSec: Int) {
        seekBar.max = durationMinSec
        seekBar.progress = currentMinSec
        findViewById<TextView>(R.id.textCurrentTime).text = formatTime(currentMinSec)
        findViewById<TextView>(R.id.textTotalTime).text = formatTime(durationMinSec)
    }

    private fun formatTime(ms: Int): String {
        val mins = (ms / 1000) / 60
        val secs = (ms / 1000) % 60
        return String.format(java.util.Locale.getDefault(), "%d:%02d", mins, secs)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        presenter.onDestroy()
    }
}