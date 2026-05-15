package com.example.musicplayer.music_player_app.backend.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.musicplayer.music_player_app.frontend.screens.playlist.Song

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayerPlaying = false
    private var currentPlayingSong: Song? = null

    enum class PlaybackMode {
        NORMAL,  // Sequential
        SHUFFLE, // Random order, all played once
        RANDOM   // Truly random selection next
    }

    private var currentMode = PlaybackMode.NORMAL
    private var songList: List<Song> = emptyList()

    // Tracks the order of playback (indices into songList)
    private var playbackOrder: List<Int> = emptyList()
    private var currentOrderIndex = -1

    private val CHANNEL_ID = "MusicServiceChannel"
    private val NOTIFICATION_ID = 1

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopMusic()
            stopSelf()
            return START_NOT_STICKY
        }
        val currentSong = getCurrentSong()
        val notification = createNotification(currentSong?.title ?: "Ready to play", isPlaying())
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("MusicService", "App removed from recents. Stopping service.")
        stopMusic()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String, playing: Boolean): Notification {
        val icon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText(content)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(playing)
            .build()
    }

    private fun updateNotification(content: String, playing: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(content, playing))
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) {
            songList = emptyList()
            playbackOrder = emptyList()
            currentOrderIndex = -1
            return
        }

        // Always update the song list first
        songList = songs

        when (currentMode) {
            PlaybackMode.SHUFFLE -> {
                val indices = songList.indices.toMutableList()
                indices.removeAt(startIndex) // Remove the specific index we want to start with
                indices.shuffle()
                playbackOrder = listOf(startIndex) + indices
                currentOrderIndex = 0
            }
            else -> {
                // NORMAL and RANDOM (Random picks a new index on Next anyway)
                playbackOrder = songList.indices.toList()
                currentOrderIndex = startIndex
            }
        }

        playCurrent(forceRestart = true)
    }

    private fun playCurrent(forceRestart: Boolean = false) {
        if (currentOrderIndex !in playbackOrder.indices) return
        val songIndex = playbackOrder[currentOrderIndex]
        if (songIndex !in songList.indices) return
        
        val song = songList[songIndex]
        playSong(song, forceRestart)
    }

    fun playSong(song: Song, forceRestart: Boolean = false) {
        Log.d("MusicService", "Attempting to play: ${song.title} from URI: ${song.fileUri}")
        try {
            if (!forceRestart && mediaPlayer != null && currentPlayingSong?.fileUri == song.fileUri && currentPlayingSong?.playlistId == song.playlistId) {
                resumeMusic()
                return
            }

            // Sync currentOrderIndex if the song exists in the current playlist
            val indexInList = songList.indexOfFirst { it.fileUri == song.fileUri && it.playlistId == song.playlistId }
            if (indexInList != -1) {
                val orderIndex = playbackOrder.indexOf(indexInList)
                if (orderIndex != -1) {
                    currentOrderIndex = orderIndex
                    Log.d("MusicService", "Synced currentOrderIndex to $currentOrderIndex")
                }
            }

            stopMusic()
            currentPlayingSong = song
            
            val newPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                val uri = Uri.parse(song.fileUri)
                try {
                    if (uri.scheme == "content") {
                        val afd: AssetFileDescriptor? = contentResolver.openAssetFileDescriptor(uri, "r")
                        afd?.let {
                            setDataSource(it.fileDescriptor, it.startOffset, it.length)
                            it.close()
                        } ?: throw Exception("Failed to open AssetFileDescriptor")
                    } else {
                        setDataSource(song.fileUri)
                    }
                } catch (e: SecurityException) {
                    Log.e("MusicService", "SecurityException opening URI: ${song.fileUri}", e)
                    throw e
                } catch (e: Exception) {
                    Log.e("MusicService", "Exception opening URI: ${song.fileUri}", e)
                    throw e
                }
                
                setOnPreparedListener { 
                    Log.d("MusicService", "MediaPlayer prepared, starting playback")
                    it.start()
                    isPlayerPlaying = true
                    updateNotification(song.title, true)
                }
                setOnCompletionListener {
                    Log.d("MusicService", "Playback completed")
                    isPlayerPlaying = false
                    playNext()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicService", "MediaPlayer error: $what, extra: $extra")
                    isPlayerPlaying = false
                    stopMusic() // Clear the broken player
                    false
                }
                prepareAsync()
            }
            mediaPlayer = newPlayer
        } catch (e: Exception) {
            Log.e("MusicService", "Error setting up playback for ${song.fileUri}", e)
            stopMusic() // Ensure state is clean
            isPlayerPlaying = false
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error stopping/releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            isPlayerPlaying = false
            currentPlayingSong = null
        }
    }

    fun playNext() {
        if (songList.isEmpty() || playbackOrder.isEmpty()) return

        if (currentMode == PlaybackMode.RANDOM) {
            // Pick a completely random song from the list
            currentOrderIndex = (0 until playbackOrder.size).random()
        } else {
            // Move to next in the current order (Normal or Shuffle)
            currentOrderIndex = (currentOrderIndex + 1) % playbackOrder.size
        }
        
        Log.d("MusicService", "Navigating Next: New Index=$currentOrderIndex")
        playCurrent(forceRestart = true)
    }

    fun playPrevious() {
        if (songList.isEmpty() || playbackOrder.isEmpty()) return
        
        // If song is more than 3 seconds in, restart the current song
        if (getCurrentPosition() > 3000) {
            seekTo(0)
            return
        }

        if (currentMode == PlaybackMode.RANDOM) {
            // In random mode, "back" just picks another random song
            currentOrderIndex = (0 until playbackOrder.size).random()
        } else {
            // Move to previous in the current order (Normal or Shuffle)
            currentOrderIndex = if (currentOrderIndex <= 0) playbackOrder.size - 1 else currentOrderIndex - 1
        }
        
        Log.d("MusicService", "Navigating Back: New Index=$currentOrderIndex")
        playCurrent(forceRestart = true)
    }

    fun cyclePlaybackMode() {
        currentMode = when (currentMode) {
            PlaybackMode.NORMAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.RANDOM
            PlaybackMode.RANDOM -> PlaybackMode.NORMAL
        }

        if (songList.isEmpty()) return

        val currentSongUri = currentPlayingSong?.fileUri

        when (currentMode) {
            PlaybackMode.SHUFFLE -> {
                val indices = songList.indices.toMutableList()
                val currentIdxInRawList = songList.indexOfFirst { it.fileUri == currentSongUri }
                
                if (currentIdxInRawList != -1) {
                    indices.remove(currentIdxInRawList)
                    indices.shuffle()
                    playbackOrder = listOf(currentIdxInRawList) + indices
                    currentOrderIndex = 0
                } else {
                    indices.shuffle()
                    playbackOrder = indices
                    currentOrderIndex = 0
                }
            }
            else -> {
                // NORMAL and RANDOM
                playbackOrder = songList.indices.toList()
                val currentIdxInRawList = songList.indexOfFirst { it.fileUri == currentSongUri }
                currentOrderIndex = if (currentIdxInRawList != -1) currentIdxInRawList else 0
            }
        }
        Log.d("MusicService", "Mode cycled to $currentMode. New order size: ${playbackOrder.size}")
    }

    fun onSongDeleted(songUri: String) {
        if (currentPlayingSong?.fileUri == songUri) {
            stopMusic()
            currentPlayingSong = null
            updateNotification("No song playing", false)
        }
        // Remove the song from the current memory list
        val updatedList = songList.filter { it.fileUri != songUri }
        if (updatedList.size != songList.size) {
            songList = updatedList
            playbackOrder = songList.indices.toList()
            currentOrderIndex = -1
        }
    }

    fun getPlaybackMode() = currentMode

    fun getCurrentSong(): Song? {
        if (currentPlayingSong != null) return currentPlayingSong
        
        val songIndex = if (currentOrderIndex in playbackOrder.indices) playbackOrder[currentOrderIndex] else -1
        return if (songIndex in songList.indices) songList[songIndex] else null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            isPlayerPlaying
        }
    }

    fun pauseMusic() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlayerPlaying = false
                    updateNotification("(Paused) ${getCurrentSong()?.title ?: ""}", false)
                }
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error in pauseMusic", e)
        }
    }

    fun resumeMusic() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    isPlayerPlaying = true
                    updateNotification(getCurrentSong()?.title ?: "Playing", true)
                }
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error in resumeMusic", e)
            getCurrentSong()?.let { playSong(it) }
        }
    }

    fun getDuration(): Int = try { mediaPlayer?.duration ?: 0 } catch (e: Exception) { 0 }
    fun getCurrentPosition(): Int = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            Log.e("MusicService", "Error in seekTo", e)
        }
    }

    override fun onDestroy() {
        stopMusic()
        super.onDestroy()
    }
}