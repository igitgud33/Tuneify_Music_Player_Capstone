package com.example.musicplayer.music_player_app.frontend.screens.library

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LibraryActivity : AppCompatActivity(), LibraryContract.View {

    private lateinit var presenter: LibraryPresenter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabDelete: FloatingActionButton
    private var adapter: LibraryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recyclerView = findViewById(R.id.recyclerViewLibrary)
        progressBar = findViewById(R.id.progressBarLibrary)
        val fabAddPlaylist = findViewById<FloatingActionButton>(R.id.fabAddPlaylist)
        fabDelete = findViewById(R.id.fabDeletePlaylists)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        presenter = LibraryPresenter(this, LibraryModel(this))
        
        fabAddPlaylist.setOnClickListener { presenter.onAddPlaylistClick() }
        fabDelete.setOnClickListener {
            adapter?.getSelectedPlaylistIds()?.let { ids ->
                if (ids.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Playlists")
                        .setMessage("Are you sure you want to delete ${ids.size} playlists?")
                        .setPositiveButton("Delete") { _, _ ->
                            presenter.deletePlaylists(ids)
                            adapter?.clearSelection()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }

        presenter.loadLibrary()
    }

    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    override fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    override fun displayLibrary(playlists: List<PlaylistInfo>) {
        adapter = LibraryAdapter(playlists) { isSelectionMode ->
            fabDelete.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        }
        recyclerView.adapter = adapter
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showAddPlaylistDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create New Playlist")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)
        val inputName = EditText(this).apply { hint = "Playlist Name" }
        layout.addView(inputName)
        builder.setView(layout)
        builder.setPositiveButton("Create") { _, _ ->
            val name = inputName.text.toString()
            if (name.isNotBlank()) {
                presenter.addPlaylist(name, "")
            } else {
                showError("Playlist name cannot be empty")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }
}