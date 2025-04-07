package com.example.photogalleryapp.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.example.photogalleryapp.data.Photo
import java.io.File
import java.io.FileOutputStream

class PhotoGalleryViewModel : ViewModel() {
    private val _photos = mutableStateListOf<Photo>()
    var photos: List<Photo> = _photos

    private var _currentPhotoIndex = mutableStateOf(0)
    val currentPhotoIndex: State<Int> = _currentPhotoIndex

    var isDarkTheme by mutableStateOf(false)
        private set

    init {
        for (i in 20..30) {
            _photos.add(
                Photo(
                    id = "photo_$i",
                    url = "https://picsum.photos/id/${50 + i}/800/600",
                    thumbnail = "https://picsum.photos/id/${50 + i}/200/200",
                    title = "Photo $i"
                )
            )
        }
    }

    fun toggleFavorite(photoId: String) {
        val index = _photos.indexOfFirst { it.id == photoId }
        if (index >= 0) {
            _photos[index] = _photos[index].copy(isFavorite = !_photos[index].isFavorite)
        }
    }

    fun deletePhoto(photoId: String) {
        val index = _photos.indexOfFirst { it.id == photoId }
        if (index >= 0) {
            _photos.removeAt(index)
            if (_currentPhotoIndex.value >= _photos.size) {
                _currentPhotoIndex.value = _photos.size - 1
            }
        }
    }

    fun nextPhoto() {
        if (_photos.isNotEmpty()) {
            _currentPhotoIndex.value = (_currentPhotoIndex.value + 1) % _photos.size
        }
    }

    fun previousPhoto() {
        if (_photos.isNotEmpty()) {
            _currentPhotoIndex.value = (_currentPhotoIndex.value - 1 + _photos.size) % _photos.size
        }
    }

    fun setCurrentPhoto(index: Int) {
        if (index in 0 until _photos.size) {
            _currentPhotoIndex.value = index
        }
    }

    fun addPhoto(photo: Photo) {
        _photos.add(photo)
    }

    fun handlePhotoResult(intent: Intent, context: Context) {
        // Handle camera result
        intent.extras?.get("data")?.let { data ->
            if (data is Bitmap) {
                try {
                    // Save the bitmap to storage and get a Uri
                    val photoFile = createImageFile(context)
                    FileOutputStream(photoFile).use { out ->
                        data.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }

                    val photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )

                    // Create thumbnail
                    val thumbnailBitmap = createThumbnail(data, 200)
                    val thumbnailFile = createImageFile(context, "thumb_")
                    FileOutputStream(thumbnailFile).use { out ->
                        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    val thumbnailUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        thumbnailFile
                    )

                    val photo = Photo(
                        id = "photo_${System.currentTimeMillis()}",
                        url = photoUri.toString(),
                        thumbnail = thumbnailUri.toString(),
                        title = "Camera Photo ${photos.size + 1}"
                    )
                    _photos.add(photo)
                } catch (e: Exception) {
                    Log.e("PhotoGallery", "Error saving camera photo", e)
                }
            }
        }

        // Handle gallery result
        intent.data?.let { uri ->
            try {
                // Get the bitmap from the gallery URI
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

                // Create thumbnail
                val thumbnailBitmap = createThumbnail(bitmap, 200)
                val thumbnailFile = createImageFile(context, "thumb_")
                FileOutputStream(thumbnailFile).use { out ->
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                val thumbnailUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    thumbnailFile
                )

                val photo = Photo(
                    id = "photo_${System.currentTimeMillis()}",
                    url = uri.toString(),
                    thumbnail = thumbnailUri.toString(),
                    title = "Gallery Photo ${photos.size + 1}"
                )
                _photos.add(photo)
            } catch (e: Exception) {
                Log.e("PhotoGallery", "Error processing gallery photo", e)
            }
        }

    }
    private fun createImageFile(context: Context, prefix: String = ""): File {
        val timeStamp = System.currentTimeMillis().toString()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${prefix}JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun createThumbnail(bitmap: Bitmap, size: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = size.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    fun clearAllFavorites() {
        photos = photos.map { it.copy(isFavorite = false) }
    }
    // Helper extension function
    fun getFavoriteCount(): Int {
        return _photos.count { it.isFavorite }
    }

}
