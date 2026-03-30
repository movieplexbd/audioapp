package com.movieplexbd.audioapp.models

data class AudioTrack(
    val id: Int,
    val title: String,
    val filePath: String,
    val isFromAsset: Boolean = false
)