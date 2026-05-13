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
    private var songList: List<Song> = emptyList()
    
    // Tracks the order of playback (indices into songList)
    private var playbackOrder: List<Int> = emptyList()
    private var currentOrderIndex = -1
    
    private var isShuffle = false

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
        val currentSong = getCurrentSong()
        val notification = createNotification(currentSong?.title ?: "Ready to play")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
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

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Playing Music")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        songList = songs
        if (songList.isEmpty()) {
            playbackOrder = emptyList()
            currentOrderIndex = -1
            return
        }

        if (isShuffle) {
            val indices = songList.indices.toMutableList()
            indices.remove(startIndex)
            indices.shuffle()
            playbackOrder = listOf(startIndex) + indices
            currentOrderIndex = 0
        } else {
            playbackOrder = songList.indices.toList()
            currentOrderIndex = startIndex
        }

        playCurrent()
    }

    private fun playCurrent() {
        if (currentOrderIndex !in playbackOrder.indices) return
        val songIndex = playbackOrder[currentOrderIndex]
        if (songIndex !in songList.indices) return
        
        val song = songList[songIndex]
        playSong(song.fileUri)
    }

    fun playSong(fileUri: String) {
        Log.d("MusicService", "Attempting to play: $fileUri")
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                val uri = Uri.parse(fileUri)
                if (uri.scheme == "content") {
                    val afd: AssetFileDescriptor? = contentResolver.openAssetFileDescriptor(uri, "r")
                    afd?.let {
                        setDataSource(it.fileDescriptor, it.startOffset, it.length)
                        it.close()
                    } ?: run {
                        Log.e("MusicService", "Failed to open AssetFileDescriptor for $fileUri")
                        return
                    }
                } else {
                    setDataSource(fileUri)
                }
                
                setOnPreparedListener { 
                    it.start()
                    val currentSong = getCurrentSong()
                    updateNotification(currentSong?.title ?: "Unknown")
                    
                    // Ensure service stays in foreground
                    startForeground(NOTIFICATION_ID, createNotification(currentSong?.title ?: "Unknown"))
                }
                setOnCompletionListener {
                    playNext()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicService", "MediaPlayer error: $what, extra: $extra")
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error setting up playback for $fileUri", e)
        }
    }

    fun playNext() {
        if (playbackOrder.isEmpty()) return
        currentOrderIndex = (currentOrderIndex + 1) % playbackOrder.size
        playCurrent()
    }

    fun playPrevious() {
        if (playbackOrder.isEmpty()) return
        
        // Restart current song if more than 3 seconds in
        if (getCurrentPosition() > 3000) {
            seekTo(0)
            return
        }

        currentOrderIndex = if (currentOrderIndex <= 0) playbackOrder.size - 1 else currentOrderIndex - 1
        playCurrent()
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        if (songList.isEmpty()) return

        val currentSongIndex = if (currentOrderIndex in playbackOrder.indices) playbackOrder[currentOrderIndex] else -1

        if (isShuffle) {
            // Enable shuffle: current song stays at the front
            val indices = songList.indices.toMutableList()
            if (currentSongIndex != -1) {
                indices.remove(currentSongIndex)
                indices.shuffle()
                playbackOrder = listOf(currentSongIndex) + indices
                currentOrderIndex = 0
            } else {
                indices.shuffle()
                playbackOrder = indices
                currentOrderIndex = 0
            }
        } else {
            // Disable shuffle: return to normal order
            playbackOrder = songList.indices.toList()
            currentOrderIndex = if (currentSongIndex != -1) currentSongIndex else 0
        }
    }

    fun isShuffleEnabled() = isShuffle

    fun getCurrentSong(): Song? {
        val songIndex = if (currentOrderIndex in playbackOrder.indices) playbackOrder[currentOrderIndex] else -1
        return if (songIndex in songList.indices) songList[songIndex] else null
    }

    fun isPlaying(): Boolean = try { mediaPlayer?.isPlaying ?: false } catch (e: Exception) { false }

    fun pauseResume() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.start()
                }
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error in pauseResume", e)
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
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}