package com.example.photogalleryapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photogalleryapp.data.Photo
import com.example.photogalleryapp.viewmodel.PhotoGalleryViewModel

// Opt-in annotations are required to use experimental APIs from Jetpack Compose Foundation and Material3.
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
/**
 * The main screen composable function that displays a grid of photos.
 * It uses a Scaffold for basic Material Design layout structure.
 *
 * @param viewModel The ViewModel providing photo data and handling actions.
 * @param onPhotoClick A callback function invoked when a photo is clicked, passing the index of the clicked photo.
 */
@Composable
fun PhotoGridScreen(
    viewModel: PhotoGalleryViewModel,
    onPhotoClick: (Int) -> Unit
) {
    // Observe the list of photos from the ViewModel. Compose will recompose if this list changes.
    val photos = viewModel.photos

    // State to keep track of the ID of the photo that was long-clicked. Used for the context menu.
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    // State to control the visibility of the context menu (AlertDialog).
    var showContextMenu by remember { mutableStateOf(false) }
    // State for the LazyVerticalGrid, allows controlling and observing scroll position.
    val lazyGridState = rememberLazyGridState()

    // Scaffold provides a standard layout structure (TopAppBar, content area, etc.).
    Scaffold(
        // Defines the TopAppBar for the screen.
        topBar = {
            CenterAlignedTopAppBar(
                // Title displayed in the center of the TopAppBar.
                title = {
                    Text(
                        "My Awesome Collection",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary // Use theme color
                    )
                },
                // Actions displayed on the trailing edge of the TopAppBar.
                actions = {
                    // An icon button for refreshing the photo list (currently has an empty onClick).
                    IconButton(onClick = { /* TODO: Implement refresh logic */ }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload Images", // Accessibility description
                            tint = MaterialTheme.colorScheme.tertiary // Use theme color
                        )
                    }
                },
                // Configure the colors of the TopAppBar.
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant // Use theme color
                ),
                // Apply a shadow modifier to the TopAppBar for elevation effect.
                modifier = Modifier.shadow(6.dp)
            )
        },
        // Set the background color for the main content area of the Scaffold.
        containerColor = Color(0xFFE8DEF8) // Specific color for the background
    ) { paddingValues -> // The lambda provides padding values to avoid content overlapping the Scaffold elements (like TopAppBar).

        // LazyVerticalGrid efficiently displays items in a scrollable vertical grid.
        LazyVerticalGrid(
            state = lazyGridState, // Associate the grid state
            // Defines the grid columns. Adaptive means columns are created based on available width and minSize.
            columns = GridCells.Adaptive(minSize = 160.dp),
            // Padding around the entire grid content. Includes top padding from the Scaffold.
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = paddingValues.calculateTopPadding() + 10.dp, // Respect TopAppBar padding
                bottom = 16.dp
            ),
            // Spacing between grid items horizontally.
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            // Spacing between grid items vertically.
            verticalArrangement = Arrangement.spacedBy(14.dp),
            // Modifier to make the grid fill the available screen space.
            modifier = Modifier.fillMaxSize()
        ) {
            // Define the items in the grid. Uses photo ID as a key for better performance and animations.
            items(photos.size, key = { photos[it].id }) { index ->
                // Get the specific photo data for the current item.
                val photo = photos[index]
                // Display a single photo item using the PhotoGridItem composable.
                PhotoGridItem(
                    photo = photo,
                    // Pass the lambda to handle single clicks, invoking the screen's onPhotoClick.
                    onPhotoClick = { onPhotoClick(index) },
                    // Pass the lambda to handle long clicks.
                    onPhotoLongClick = {
                        selectedPhotoId = photo.id // Store the ID of the long-clicked photo.
                        showContextMenu = true // Set the state to show the context menu.
                    },
                    // Pass the lambda to handle clicks on the favorite icon.
                    onFavoriteClick = { viewModel.toggleFavorite(photo.id) },
                    // Modifier to animate item placement changes (e.g., when items are added/removed/reordered).
                    modifier = Modifier.animateItemPlacement(
                        // Use a spring animation for a bouncy effect.
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessVeryLow
                        )
                    )
                )
            }
        }
    }

    // Conditionally display the AlertDialog based on the showContextMenu state and if a photo is selected.
    if (showContextMenu && selectedPhotoId != null) {
        // Find the full Photo object corresponding to the selected ID.
        val photo = photos.find { it.id == selectedPhotoId }
        // Ensure the photo data was found before showing the dialog.
        if (photo != null) {
            // Standard Material Design dialog for showing context actions.
            AlertDialog(
                // Called when the user dismisses the dialog (e.g., by clicking outside or pressing back).
                onDismissRequest = { showContextMenu = false },
                // The title of the dialog.
                title = { Text("Image Actions", fontWeight = FontWeight.Medium) },
                // The main descriptive text of the dialog.
                text = { Text("What do you want to do with '${photo.title}'?") },
                // The primary action button (confirm). Here used for Favorite/Un-favorite.
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.toggleFavorite(photo.id) // Perform the favorite action via ViewModel.
                            showContextMenu = false // Close the dialog.
                        },
                        // Dynamically set button colors based on the photo's favorite status.
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (photo.isFavorite) Color(0xFFF8BBD0) else Color(0xFFB3E5FC)
                        )
                    ) {
                        // Display the appropriate favorite icon (filled or outlined).
                        Icon(
                            imageVector = if (photo.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null, // Decorative icon
                            // Dynamically set icon tint based on favorite status.
                            tint = if (photo.isFavorite) Color.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp)) // Add space between icon and text.
                        // Display the appropriate button text.
                        Text(
                            if (photo.isFavorite) "Un-Favorite" else "Make Favorite",
                            color = MaterialTheme.colorScheme.onSurface // Use theme color for text
                        )
                    }
                },
                // The secondary action button (dismiss/cancel). Here used for Delete.
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.deletePhoto(photo.id) // Perform the delete action via ViewModel.
                            showContextMenu = false // Close the dialog.
                        },
                        // Set the content color (icon and text) for the outlined button.
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null) // Delete icon
                        Spacer(modifier = Modifier.width(8.dp)) // Add space between icon and text.
                        Text("Delete Image")
                    }
                },
                // Set a custom background color for the AlertDialog container.
                containerColor = Color(0xFFFFFDE7)
            )
        }
    }
}

// Opt-in for ExperimentalFoundationApi is needed for combinedClickable.
@OptIn(ExperimentalFoundationApi::class)
/**
 * A composable function representing a single item in the photo grid.
 * Displays the photo thumbnail, title, a favorite button, and handles interactions.
 *
 * @param photo The data object for the photo to display.
 * @param onPhotoClick Callback invoked when the item is clicked.
 * @param onPhotoLongClick Callback invoked when the item is long-clicked.
 * @param onFavoriteClick Callback invoked when the favorite icon is clicked.
 * @param modifier Modifier passed from the caller, used for layout and animation within the grid.
 */
@Composable
fun PhotoGridItem(
    photo: Photo,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier // Default empty modifier
) {
    // State to track whether the AsyncImage has finished loading successfully. Used for the loading indicator.
    var isLoaded by remember { mutableStateOf(false) }
    // Interaction source to track interactions like pressing on the item.
    val interactionSource = remember { MutableInteractionSource() }
    // Collect the pressed state from the interaction source. This state updates when the item is pressed/released.
    val isPressed by interactionSource.collectIsPressedAsState()
    // Calculate a scale factor based on the pressed state for a visual feedback animation.
    val scale = if (isPressed) 0.97f else 1f

    // Card provides a Material Design surface with elevation and rounded corners.
    Card(
        modifier = modifier // Apply modifier passed from the parent (includes animateItemPlacement)
            .fillMaxWidth() // Make the card fill the width allocated by the grid cell.
            .aspectRatio(0.85f) // Maintain a fixed aspect ratio for the card.
            // Apply scaling transformation using graphicsLayer for press effect.
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, RoundedCornerShape(16.dp)) // Add shadow with rounded corners.
            // Handle both single clicks and long clicks on the card.
            .combinedClickable(
                interactionSource = interactionSource, // Connect the interaction source
                indication = null, // Disable default ripple indication
                onClick = onPhotoClick, // Set the single click action
                onLongClick = onPhotoLongClick // Set the long click action
            ),
        shape = RoundedCornerShape(16.dp), // Define the shape (rounded corners) for the card.
        colors = CardDefaults.cardColors(containerColor = Color.White) // Set the card's background color.
    ) {
        // Box is used to layer multiple composables on top of each other.
        Box(modifier = Modifier.fillMaxSize()) { // Fill the entire space of the Card.

            // AsyncImage loads and displays the image from a URL using the Coil library.
            AsyncImage(
                // Build the image request, specifying the data (URL) and enabling crossfade animation.
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.thumbnail) // URL of the thumbnail image
                    .crossfade(500) // Enable crossfade animation over 500ms
                    .build(),
                contentDescription = "Gallery Image", // Accessibility description
                // Scale the image to fill the bounds, cropping if necessary.
                contentScale = ContentScale.Crop,
                // Make the image fill the Box and clip it to the Card's shape.
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                // Callback invoked when the image successfully loads.
                onSuccess = { isLoaded = true } // Update the loading state.
            )

            // Another Box layered on top of the image to create a gradient scrim effect.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Apply a vertical gradient from transparent to semi-transparent black at the bottom.
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f) // Darken the bottom area
                            ),
                            startY = 300f, // Start the gradient effect lower down
                            endY = Float.POSITIVE_INFINITY // Extend gradient to the bottom edge
                        )
                    )
            )

            // Text composable to display the photo's title over the gradient scrim.
            Text(
                text = photo.title,
                color = Color.White.copy(alpha = 0.9f), // Semi-transparent white for readability
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                maxLines = 2, // Limit title to 2 lines
                overflow = TextOverflow.Ellipsis, // Add ellipsis (...) if text overflows
                modifier = Modifier
                    .align(Alignment.BottomStart) // Position at the bottom-left corner inside the Box.
                    .padding(horizontal = 10.dp, vertical = 8.dp) // Add padding around the text.
                    .fillMaxWidth(0.85f) // Limit width to prevent overlapping the favorite icon.
                    .shadow(1.dp, ambientColor = Color.Black.copy(alpha = 0.5f)) // Subtle text shadow
            )

            // IconButton for the favorite action, placed at the top-right corner.
            IconButton(
                onClick = onFavoriteClick, // Trigger the favorite click callback.
                modifier = Modifier
                    .align(Alignment.TopEnd) // Position at the top-right corner inside the Box.
                    .padding(6.dp) // Add padding around the button.
                    .size(38.dp) // Set the size of the button's touch target.
                    // Apply a radial gradient background for better visibility against various image content.
                    .background(
                        Brush.radialGradient(
                            listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        CircleShape // Make the background circular.
                    )
            ) {
                // Icon displayed inside the IconButton (filled heart for favorite, outlined for not).
                Icon(
                    imageVector = if (photo.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null, // Decorative icon
                    // Dynamically set the icon's tint color based on favorite status.
                    tint = if (photo.isFavorite) Color(0xFFF06292) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp) // Set the size of the icon itself.
                )
            }

            // AnimatedVisibility controls the appearance/disappearance of the loading indicator.
            this@Card.AnimatedVisibility( // Scope to Card might not be strictly necessary here
                visible = !isLoaded, // Show the indicator only when the image is *not* loaded.
                enter = fadeIn(), // Use a fade-in animation when appearing.
                exit = fadeOut(), // Use a fade-out animation when disappearing.
                modifier = Modifier.align(Alignment.Center) // Center the indicator within the Box.
            ) {
                // CircularProgressIndicator is the actual loading spinner.
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary, // Use theme color for the spinner
                    modifier = Modifier.size(44.dp) // Set the size of the spinner.
                )
            }
        }
    }
}