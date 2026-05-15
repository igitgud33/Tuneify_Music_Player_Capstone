package com.example.musicplayer.music_player_app.frontend.screens.library

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.musicplayer.R

class LibraryAdapter(
    private val playlists: List<PlaylistInfo>,
    private val onSelectionModeChanged: (Boolean) -> Unit,
    private val onPlaylistClicked: (Int, String, String) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>() {

    private val selectedPlaylists = mutableSetOf<PlaylistInfo>()
    var isSelectionMode = false
        private set

    class LibraryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textPlaylistName: TextView = view.findViewById(R.id.textPlaylistName)
        val textSongCount: TextView = view.findViewById(R.id.textSongCount)
        val imageCover: ImageView = view.findViewById(R.id.imagePlaylistCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_placeholder, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.textPlaylistName.text = playlist.name
        holder.textSongCount.text = "${playlist.songCount} songs"

        if (playlist.coverUri.isNotEmpty()) {
            holder.imageCover.load(Uri.parse(playlist.coverUri)) {
                crossfade(true)
                error(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.imageCover.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val isSelected = selectedPlaylists.contains(playlist)
        holder.itemView.setBackgroundColor(if (isSelected) Color.parseColor("#448A65D6") else Color.TRANSPARENT)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(playlist)
            } else {
                onPlaylistClicked(playlist.id, playlist.name, playlist.coverUri)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(playlist)
                onSelectionModeChanged(true)
                true
            } else {
                false
            }
        }
    }

    private fun toggleSelection(playlist: PlaylistInfo) {
        if (selectedPlaylists.contains(playlist)) {
            selectedPlaylists.remove(playlist)
        } else {
            selectedPlaylists.add(playlist)
        }
        notifyDataSetChanged()
        if (selectedPlaylists.isEmpty()) {
            isSelectionMode = false
            onSelectionModeChanged(false)
        }
    }

    fun getSelectedPlaylistIds(): List<Int> = selectedPlaylists.map { it.id }

    fun clearSelection() {
        selectedPlaylists.clear()
        isSelectionMode = false
        onSelectionModeChanged(false)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = playlists.size
}