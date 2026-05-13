package com.example.musicplayer.music_player_app.frontend.screens.playlist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import com.example.musicplayer.music_player_app.backend.service.MusicService
import com.example.musicplayer.music_player_app.frontend.screens.mediaplayer.MediaPlayerActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlaylistActivity : AppCompatActivity(), PlaylistContract.View {

    private lateinit var presenter: PlaylistPresenter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabDelete: FloatingActionButton
    private var playlistId: Int = -1
    private var adapter: PlaylistAdapter? = null
    
    private var musicService: MusicService? = null
    private var isBound = false
    
    private var allSongs: List<Song> = emptyList()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    private var pendingTitle: String = ""
    private var pendingArtist: String = ""

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                // Request persistent access to the file
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Not always possible, but good to try for persistent access
            }
            presenter.addSong(pendingTitle, pendingArtist, it.toString(), playlistId)
        } ?: showError("No file selected")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        playlistId = intent.getIntExtra("PLAYLIST_ID", -1)
        val playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Playlist"
        supportActionBar?.title = playlistName

        recyclerView = findViewById(R.id.recyclerViewPlaylist)
        progressBar = findViewById(R.id.progressBarPlaylist)
        val fabAddSong = findViewById<FloatingActionButton>(R.id.fabAddSong)
        fabDelete = findViewById(R.id.fabDeleteSongs)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val database = AppDatabase.getDatabase(this)
        presenter = PlaylistPresenter(this, PlaylistModel(database))

        fabAddSong.setOnClickListener { presenter.onAddSongClick() }
        fabDelete.setOnClickListener {
            adapter?.getSelectedSongs()?.let { songs ->
                if (songs.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Songs")
                        .setMessage("Are you sure you want to delete ${songs.size} songs?")
                        .setPositiveButton("Delete") { _, _ ->
                            presenter.deleteSongs(songs, playlistId)
                            adapter?.clearSelection()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }

        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        if (playlistId != -1) {
            presenter.loadPlaylist(playlistId)
        } else {
            showError("Invalid Playlist")
            finish()
        }
    }

    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    override fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    override fun displayPlaylist(songs: List<Song>) {
        allSongs = songs
        adapter = PlaylistAdapter(songs, { isSelectionMode ->
            fabDelete.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        }, { clickedSong ->
            onSongClicked(clickedSong)
        })
        recyclerView.adapter = adapter
    }
    
    private fun onSongClicked(song: Song) {
        musicService?.let { service ->
            val index = allSongs.indexOf(song)
            
            val serviceIntent = Intent(this, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            service.setPlaylist(allSongs, index)
            startActivity(Intent(this, MediaPlayerActivity::class.java))
        } ?: showError("Service not connected")
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showAddSongDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Song")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)
        val inputTitle = EditText(this).apply { hint = "Song Title" }
        layout.addView(inputTitle)
        val inputArtist = EditText(this).apply { hint = "Artist" }
        layout.addView(inputArtist)
        builder.setView(layout)
        builder.setPositiveButton("Pick File") { _, _ ->
            val title = inputTitle.text.toString()
            val artist = inputArtist.text.toString()
            if (title.isNotBlank() && artist.isNotBlank()) {
                pendingTitle = title
                pendingArtist = artist
                // Using OpenDocument for persistent URI permissions
                pickAudioLauncher.launch(arrayOf("audio/*"))
            } else {
                showError("Please fill title and artist")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        presenter.onDestroy()
        super.onDestroy()
    }
}