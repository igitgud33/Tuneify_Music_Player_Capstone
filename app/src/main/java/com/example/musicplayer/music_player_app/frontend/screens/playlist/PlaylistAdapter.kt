package com.example.musicplayer.music_player_app.frontend.screens.playlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R

class PlaylistAdapter(
    private val songs: List<Song>,
    private val onSelectionChanged: (Boolean) -> Unit,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.SongViewHolder>() {

    private val selectedSongs = mutableSetOf<Song>()
    var isSelectionMode = false
        private set

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textSongTitle: TextView = view.findViewById(R.id.textSongTitle)
        val textArtistName: TextView = view.findViewById(R.id.textArtistName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song_placeholder, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.textSongTitle.text = song.title
        holder.textArtistName.text = song.artist

        val isSelected = selectedSongs.contains(song)
        holder.itemView.setBackgroundColor(if (isSelected) "#448A65D6".toColorInt() else Color.TRANSPARENT)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(song)
            } else {
                onSongClick(song)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(song)
                onSelectionChanged(true)
                true
            } else {
                false
            }
        }
    }

    private fun toggleSelection(song: Song) {
        if (selectedSongs.contains(song)) {
            selectedSongs.remove(song)
        } else {
            selectedSongs.add(song)
        }
        notifyDataSetChanged()
        if (selectedSongs.isEmpty()) {
            isSelectionMode = false
            onSelectionChanged(false)
        }
    }

    fun getSelectedSongs(): List<Song> = selectedSongs.toList()

    fun clearSelection() {
        selectedSongs.clear()
        isSelectionMode = false
        onSelectionChanged(false)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = songs.size
}