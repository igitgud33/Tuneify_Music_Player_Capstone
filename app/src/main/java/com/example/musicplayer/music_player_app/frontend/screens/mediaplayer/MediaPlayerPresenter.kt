package com.example.musicplayer.music_player_app.frontend.screens.mediaplayer


import android.os.Handler
import android.os.Looper
import com.example.musicplayer.music_player_app.backend.service.MusicService

class MediaPlayerPresenter(
    private var view: MediaPlayerContract.View,
    private var musicService: MusicService,


) : MediaPlayerContract.Presenter {

    // helps run code repeatedly and display progress bar UI
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object: Runnable {
        override fun run() {
            val current = musicService.getCurrentPosition()
            val total = musicService.getDuration()

            view.updateProgress(current, total)

            // update per 1 sec
            handler.postDelayed(this, 1000)
        }
    }

    /*
    INSTRUCTIONS:
    1. Download mp3 and rename following this format: "song_name"
    2. Paste in "raw" package (below mipmap)
    3. In val songUri, change "song_name" to the name sa mp3 file
    Optional: input name title and artist in view.updateSongInfo()
    4. Remove comment symbols and try running DashboardActivity
    */

    init {
        // get song path file from raw package
        val songUri = "android.resource://com.example.musicplayer/raw/pink_room"
        musicService.playSong(songUri)

        view.setPlayPauseIcon(musicService.isPlaying())

        // update UI based on song
        view.updateSongInfo("Title", "Artist")
        view.setPlayPauseIcon(true)

        // run handler
        handler.post(updateProgressRunnable)
    }


    // play/pause logic
    override fun onPlayPauseClick() {
        musicService.pauseResume()

        // flip icon
        view.setPlayPauseIcon(musicService.isPlaying())
    }

    // navigation (to be filled out later: main focus is being able to play a song)
    override fun onPreviousClick() {}
    override fun onNextClick() {}

    // calls service (fun to be implemented later)
    override fun seekTo(position: Int) {
        musicService.seekTo(position)
    }

    override fun onDestroy() {
        // stop handler
        handler.removeCallbacks(updateProgressRunnable)
    }


}