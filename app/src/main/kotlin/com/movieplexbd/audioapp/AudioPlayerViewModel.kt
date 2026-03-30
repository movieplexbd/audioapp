package com.movieplexbd.audioapp

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieplexbd.audioapp.models.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerViewModel : ViewModel() {
    
    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()
    
    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private var mediaPlayer: MediaPlayer? = null
    
    fun loadTracks(context: Context) {
        viewModelScope.launch {
            val loadedTracks = mutableListOf<AudioTrack>()
            
            // Load from assets
            val assetManager = context.assets
            try {
                val audioFiles = assetManager.list("sample_audio") ?: emptyArray()
                audioFiles.forEachIndexed { index, fileName ->
                    if (fileName.endsWith(".mp3")) {
                        loadedTracks.add(
                            AudioTrack(
                                id = index,
                                title = fileName.replace(".mp3", ""),
                                filePath = "sample_audio/$fileName",
                                isFromAsset = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Load from external storage (Music directory)
            val musicDir = File("/sdcard/Music")
            if (musicDir.exists()) {
                musicDir.listFiles { file ->
                    file.extension == "mp3"
                }?.forEachIndexed { index, file ->
                    loadedTracks.add(
                        AudioTrack(
                            id = loadedTracks.size,
                            title = file.nameWithoutExtension,
                            filePath = file.absolutePath,
                            isFromAsset = false
                        )
                    )
                }
            }
            
            _tracks.value = loadedTracks
        }
    }
    
    fun play(context: Context) {
        if (_tracks.value.isEmpty()) return
        
        val track = _tracks.value[_currentTrackIndex.value]
        
        if (mediaPlayer != null && _isPlaying.value) {
            mediaPlayer?.pause()
        } else if (mediaPlayer != null && !_isPlaying.value) {
            mediaPlayer?.start()
            _isPlaying.value = true
            return
        }
        
        mediaPlayer = MediaPlayer().apply {
            try {
                if (track.isFromAsset) {
                    val afd = context.assets.openFd(track.filePath)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } else {
                    setDataSource(track.filePath)
                }
                
                setOnCompletionListener {
                    next(context)
                }
                
                setOnPreparedListener {
                    start()
                    _duration.value = it.duration
                    _isPlaying.value = true
                    updatePosition()
                }
                
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }
    
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
    }
    
    fun next(context: Context) {
        stop()
        if (_currentTrackIndex.value < _tracks.value.size - 1) {
            _currentTrackIndex.value++
        } else {
            _currentTrackIndex.value = 0
        }
        play(context)
    }
    
    fun previous(context: Context) {
        stop()
        if (_currentTrackIndex.value > 0) {
            _currentTrackIndex.value--
        } else {
            _currentTrackIndex.value = _tracks.value.size - 1
        }
        play(context)
    }
    
    fun seek(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }
    
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }
    
    private fun updatePosition() {
        viewModelScope.launch {
            while (_isPlaying.value) {
                _currentPosition.value = mediaPlayer?.currentPosition ?: 0
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}