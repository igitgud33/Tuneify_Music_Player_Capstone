package com.example.musicplayer.music_player_app.frontend.screens.playlist

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.musicplayer.R
import com.example.musicplayer.music_player_app.backend.data.AppDatabase
import com.example.musicplayer.music_player_app.frontend.screens.mediaplayer.MediaPlayerActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlaylistActivity : AppCompatActivity(), PlaylistContract.View {

    private lateinit var presenter: PlaylistContract.Presenter
    private lateinit var adapter: PlaylistAdapter
    private lateinit var textPlaylistTitle: TextView
    private lateinit var imagePlaylistCover: ImageView
    private var currentPlaylistId: Int = -1
    private var currentCoverUri: String? = null

    // --- NEW: Service Connection for inline playback ---
    private var musicService: com.example.musicplayer.music_player_app.backend.service.MusicService? = null
    private var isBound = false

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as com.example.musicplayer.music_player_app.backend.service.MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            
            // Sync adapter state with current service state
            musicService?.let { service ->
                val currentSong = service.getCurrentSong()
                adapter.setPlaybackState(currentSong?.id, service.isPlaying())
            }
        }
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, com.example.musicplayer.music_player_app.backend.service.MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            musicService?.let { service ->
                val currentSong = service.getCurrentSong()
                adapter.setPlaybackState(currentSong?.id, service.isPlaying())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { 
            try {
                // For OpenDocument, we MUST take persistable permission if we want to play it later or from a service
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                Log.e("PlaylistActivity", "Failed to take persistable permission", e)
            }
            extractMetadataAndUpload(it) 
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.e("PlaylistActivity", "Failed to take persistable permission for image", e)
            }
            currentCoverUri = it.toString()
            imagePlaylistCover.load(it)
            presenter.updatePlaylistCover(currentPlaylistId, it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        currentPlaylistId = intent.getIntExtra("PLAYLIST_ID", -1)
        val currentPlaylistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Unknown Playlist"
        currentCoverUri = intent.getStringExtra("PLAYLIST_COVER")

        if (currentPlaylistId == -1) {
            Toast.makeText(this, "Error loading playlist", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val database = AppDatabase.getDatabase(this)
        val model = PlaylistModel(database.songDao(), database.playlistDao())
        presenter = PlaylistPresenter(this, model)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPlaylist)
        val fabUpload = findViewById<FloatingActionButton>(R.id.fabUploadSong)
        textPlaylistTitle = findViewById(R.id.textPlaylistTitle)
        imagePlaylistCover = findViewById(R.id.imagePlaylistCoverHeader)
        val buttonEditPlaylistTitle = findViewById<ImageButton>(R.id.buttonEditPlaylistTitle)

        textPlaylistTitle.text = currentPlaylistName

        if (!currentCoverUri.isNullOrEmpty()) {
            imagePlaylistCover.load(Uri.parse(currentCoverUri))
        }

        imagePlaylistCover.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        adapter = PlaylistAdapter(
            onItemClicked = { song ->
                // Ensure service knows the playlist context before opening the player
                musicService?.setPlaylist(adapter.currentList, adapter.currentList.indexOf(song))

                // Row click opens the full player screen
                val intent = Intent(this, MediaPlayerActivity::class.java).apply {
                    putExtra("SONG_TITLE", song.title)
                    putExtra("SONG_ARTIST", song.artist)
                    putExtra("SONG_URI", song.fileUri)
                }
                startActivity(intent)
            },
            onPlayAction = { song, command ->
                // Button click handles inline audio via the bound service
                val serviceIntent = Intent(this, com.example.musicplayer.music_player_app.backend.service.MusicService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }

                when (command) {
                    PlaylistAdapter.PlaybackCommand.PLAY_NEW -> {
                        musicService?.setPlaylist(adapter.currentList, adapter.currentList.indexOf(song))
                    }
                    PlaylistAdapter.PlaybackCommand.RESUME -> musicService?.resumeMusic()
                    PlaylistAdapter.PlaybackCommand.PAUSE -> musicService?.pauseMusic()
                }
            },
            onEditClicked = { song ->
                showEditSongDialog(song)
            },
            onDeleteClicked = { song ->
                musicService?.onSongDeleted(song.fileUri)
                presenter.removeSong(song)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabUpload.setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*"))
        }

        buttonEditPlaylistTitle.setOnClickListener {
            showEditPlaylistTitleDialog()
        }

        presenter.loadSongs(currentPlaylistId)
    }

    private fun extractMetadataAndUpload(uri: Uri) {
        val retriever = MediaMetadataRetriever()
        var title = "Unknown Title"
        var artist = "Unknown Artist"

        try {
            retriever.setDataSource(this, uri)
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            title = metaTitle ?: getFileNameFromUri(uri)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        } catch (e: Exception) {
            title = getFileNameFromUri(uri)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }

        presenter.uploadNewSong(title, artist, uri.toString(), currentPlaylistId)
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result?.substringBeforeLast(".") ?: "Unknown Track"
    }

    private fun showEditSongDialog(song: Song) {
        val builder = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 0)

        val inputTitle = EditText(this)
        inputTitle.setText(song.title)
        layout.addView(inputTitle)

        val inputArtist = EditText(this)
        inputArtist.setText(song.artist)
        layout.addView(inputArtist)

        builder.setView(layout)

        builder.setPositiveButton("Save") { _, _ ->
            val newTitle = inputTitle.text.toString().ifEmpty { "Unknown Title" }
            val newArtist = inputArtist.text.toString().ifEmpty { "Unknown Artist" }

            val updatedSong = song.copy(title = newTitle, artist = newArtist)
            presenter.updateSong(updatedSong)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showEditPlaylistTitleDialog() {
        val builder = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 0)

        val inputTitle = EditText(this)
        inputTitle.setText(textPlaylistTitle.text)
        layout.addView(inputTitle)

        builder.setView(layout)
        builder.setPositiveButton("Save") { _, _ ->
            val newTitle = inputTitle.text.toString().ifEmpty { "My Playlist" }
            textPlaylistTitle.text = newTitle
            presenter.updatePlaylistName(currentPlaylistId, newTitle)
            intent.putExtra("PLAYLIST_NAME", newTitle)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun showSongs(songs: List<Song>) {
        adapter.submitList(songs) {
            // After data is loaded, sync UI with service if bound
            if (isBound) {
                musicService?.let { service ->
                    val currentSong = service.getCurrentSong()
                    adapter.setPlaybackState(currentSong?.id, service.isPlaying())
                }
            }
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}