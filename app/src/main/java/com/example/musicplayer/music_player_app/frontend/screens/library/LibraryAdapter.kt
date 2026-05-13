package com.example.musicplayer.music_player_app.frontend.screens.library

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.music_player_app.frontend.screens.playlist.PlaylistActivity

class LibraryAdapter(
    private val playlists: List<PlaylistInfo>,
    private val onSelectionChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>() {

    private val selectedPlaylists = mutableSetOf<PlaylistInfo>()
    var isSelectionMode = false
        private set

    class LibraryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textPlaylistName: TextView = view.findViewById(R.id.textPlaylistName)
        val textSongCount: TextView = view.findViewById(R.id.textSongCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_placeholder, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.textPlaylistName.text = playlist.name
        holder.textSongCount.text = "${playlist.songCount} songs"

        val isSelected = selectedPlaylists.contains(playlist)
        holder.itemView.setBackgroundColor(if (isSelected) Color.parseColor("#448A65D6") else Color.TRANSPARENT)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(playlist)
            } else {
                val intent = Intent(holder.itemView.context, PlaylistActivity::class.java).apply {
                    putExtra("PLAYLIST_ID", playlist.id)
                    putExtra("PLAYLIST_NAME", playlist.name)
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(playlist)
                onSelectionChanged(true)
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
            onSelectionChanged(false)
        }
    }

    fun getSelectedPlaylistIds(): List<Int> = selectedPlaylists.map { it.id }

    fun clearSelection() {
        selectedPlaylists.clear()
        isSelectionMode = false
        onSelectionChanged(false)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = playlists.size
}