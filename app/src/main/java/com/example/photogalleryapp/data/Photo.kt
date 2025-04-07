package com.example.photogalleryapp.data

data class Photo(
    val id: String,
    val url: String,
    val thumbnail: String,
    val title: String,
    var isFavorite: Boolean = false
)