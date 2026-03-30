package com.movieplexbd.audioapp

import android.media.MediaPlayer

class AudioPlayer {
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    
    fun play(filePath: String, onCompletion: (() -> Unit)? = null) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                setOnCompletionListener {
                    onCompletion?.invoke()
                    release()
                    mediaPlayer = null
                }
                prepare()
                start()
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }
    
    fun resume() {
        mediaPlayer?.start()
        isPlaying = true
    }
    
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }
    
    fun isPlaying(): Boolean = isPlaying
}