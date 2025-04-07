package com.example.photogalleryapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape // Changed shape import (no new library)
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.photogalleryapp.viewmodel.PhotoGalleryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// Custom color palette for the full screen viewer UI theme
val DarkBackground = Color(0xFF1A1A2E) // Dark Navy Blue
val PrimaryAccent = Color(0xFFE94560) // Vibrant Pink/Red
val SecondaryAccent = Color(0xFF0F3460) // Darker Blue
val TextColorPrimary = Color(0xFFF0F0F0) // Off-white
val TextColorSecondary = Color(0xFFA0A0A0) // Light Gray
val FavoriteColorActive = Color(0xFFE94560) // Use primary accent for active favorite

/**
 * Composable function for displaying a single photo in full screen mode.
 * Allows zooming, panning, swiping between photos, and toggling favorite status.
 *
 * @param viewModel The PhotoGalleryViewModel instance providing photo data and actions.
 * @param onNavigateBack Callback function to execute when the user navigates back (e.g., pressing the back button).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPhotoScreen(
    viewModel: PhotoGalleryViewModel,
    onNavigateBack: () -> Unit
) {
    // Retrieve photo list and current index from the ViewModel
    val photos = viewModel.photos
    val currentIndex by viewModel.currentPhotoIndex // Use 'by' for direct State access

    // Navigate back if there are no photos to display (e.g., after deleting the last one)
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { // Use LaunchedEffect to avoid calling during composition
            onNavigateBack()
        }
        return // Stop composition here if photos are empty
    }

    // Ensure currentIndex is valid, handle potential race conditions or invalid states
    // If the index becomes invalid (e.g., list shrinks), try to navigate back.
    val currentPhoto = photos.getOrNull(currentIndex)
    if (currentPhoto == null) {
        LaunchedEffect(currentIndex, photos.size) {
            onNavigateBack()
        }
        return // Stop composition if current photo is inaccessible
    }


    // State for the current zoom level of the photo
    var scale by remember { mutableStateOf(1f) }
    // State for horizontal panning offset (used when zoomed in)
    var offsetX by remember { mutableStateOf(0f) }
    // State for vertical panning offset (used when zoomed in)
    var offsetY by remember { mutableStateOf(0f) }
    // Temporary state to store the initial offset at the start of a drag gesture
    var initialOffsetX by remember { mutableStateOf(0f) }
    // State for tracking horizontal swipe progress (0 = centered, >0 = swiping right, <0 = swiping left)
    // Used for swipe navigation animation between photos.
    var swipeProgress by remember { mutableStateOf(0f) }

    // State to control the visibility of the top/bottom control bars (like AppBar, navigation)
    var showControls by remember { mutableStateOf(true) }
    // Coroutine scope tied to the composable's lifecycle for launching background tasks
    val coroutineScope = rememberCoroutineScope()
    // Job reference for the auto-hide controls timer, allows cancelling the timer
    var controlsJob: Job? = remember { null }

    /**
     * Resets the timer that automatically hides the UI controls (top/bottom bars)
     * after a specific delay (4 seconds). Cancels any existing timer.
     */
    fun resetControlsTimer() {
        controlsJob?.cancel() // Cancel any previous timer job
        showControls = true // Ensure controls are visible when timer resets
        controlsJob = coroutineScope.launch {
            delay(4000) // Wait for 4 seconds
            showControls = false // Hide controls after the delay
        }
    }

    // Effect hook that runs when the `currentIndex` changes.
    // Resets zoom, pan, and restarts the auto-hide timer for the new photo.
    LaunchedEffect(currentIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        swipeProgress = 0f // Reset swipe progress when photo changes
        resetControlsTimer()
    }

    // Animated value for swipe progress. Provides a smooth spring-back effect
    // when a swipe gesture is released without navigating.
    val dragProgress by animateFloatAsState(
        targetValue = swipeProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, // Bounce effect
            stiffness = Spring.StiffnessVeryLow // Slow spring
        )
    )

    // Animated alpha (opacity) value for the top and bottom control bars.
    // Controls the fade-in/fade-out animation when `showControls` state changes.
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f, // Target alpha based on visibility state
        animationSpec = tween(400) // Duration of the fade animation
    )

    // Root container for the screen. Sets the background color and handles tap gestures.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground) // Apply the custom dark background color
        // Add pointer input detection for tap-based interactions
        .pointerInput(Unit) { // `Unit` key means this runs once
            detectTapGestures(
                // On a single tap, toggle the visibility of the controls
                onTap = {
                    showControls = !showControls
                    // If controls are now shown, reset the auto-hide timer
                    if (showControls) resetControlsTimer()
                },
                // On a double tap, toggle the zoom level between 1x and 2x
                onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.0f // Toggle scale
                    // If zooming out to 1x, reset the pan offsets
                    if (scale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    // Reset the auto-hide timer on interaction
                    resetControlsTimer()
                }
            )
        }
    ) {
        // Inner Box responsible for displaying the image and handling complex gestures:
        // - Dragging for panning (when zoomed) or swiping (when at 1x scale)
        // - Pinching for zooming
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Add pointer input detection for drag gestures. Keyed to `scale` so drag logic
                // can adapt correctly if zoom changes mid-drag (though less common).
                .pointerInput(scale) {
                    detectDragGestures(
                        // Record the starting horizontal offset when dragging begins
                        onDragStart = {
                            initialOffsetX = offsetX
                            resetControlsTimer() // Interaction resets hide timer
                        },
                        // When the drag gesture ends
                        onDragEnd = {
                            // Check if the image is at normal scale (or very close to it)
                            // and if the horizontal swipe was significant (more than 30% of screen width)
                            if (scale <= 1.05f && abs(swipeProgress) > 0.3f) {
                                // Launch a coroutine to handle the navigation asynchronously
                                coroutineScope.launch {
                                    // If swiped right (positive progress) and not the first photo
                                    if (swipeProgress > 0 && currentIndex > 0) {
                                        viewModel.previousPhoto() // Navigate to previous
                                    }
                                    // If swiped left (negative progress) and not the last photo
                                    else if (swipeProgress < 0 && currentIndex < photos.size - 1) {
                                        viewModel.nextPhoto() // Navigate to next
                                    }
                                    // Reset swipe progress and offset after navigation attempt or completion
                                    swipeProgress = 0f
                                    offsetX = 0f
                                }
                            } else {
                                // If not navigating (not zoomed out enough or swipe not far enough),
                                // animate the photo back to its resting position.
                                coroutineScope.launch {
                                    swipeProgress = 0f // Target for `dragProgress` animation
                                    // If scale was exactly 1f, ensure offset snaps back to 0
                                    if (scale <= 1f) offsetX = 0f
                                }
                            }
                        },
                        // During the drag gesture
                        onDrag = { change, dragAmount ->
                            change.consume() // Consume the pointer event to prevent propagation
                            resetControlsTimer() // Interaction resets hide timer

                            // If the image is zoomed in (scale > 1f)
                            if (scale > 1f) {
                                // Update pan offsets based on drag amount
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                // Limit panning based on image bounds and zoom level
                                val maxOffsetWidth = (scale - 1f) * size.width / 2f
                                val maxOffsetHeight = (scale - 1f) * size.height / 2f
                                offsetX = offsetX.coerceIn(-maxOffsetWidth, maxOffsetWidth)
                                offsetY = offsetY.coerceIn(-maxOffsetHeight, maxOffsetHeight)
                            } else {
                                // If image is at normal scale, interpret horizontal drag as swipe progress
                                offsetX += dragAmount.x // Still update offsetX for immediate visual feedback
                                // Calculate swipe progress relative to screen width
                                swipeProgress = offsetX / size.width
                            }
                        }
                    )
                }
                // Add pointer input detection for pinch-to-zoom gestures.
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        resetControlsTimer() // Interaction resets hide timer
                        // Update the scale, clamping it between 1x and 5x zoom
                        scale = (scale * zoom).coerceIn(1f, 5f)

                        // If zoomed in after the gesture update
                        if (scale > 1f) {
                            // Apply panning from the pinch gesture for a natural feel
                            offsetX += pan.x
                            offsetY += pan.y
                            // Limit panning based on the new scale
                            val maxOffsetWidth = (scale - 1f) * size.width / 2f
                            val maxOffsetHeight = (scale - 1f) * size.height / 2f
                            offsetX = offsetX.coerceIn(-maxOffsetWidth, maxOffsetWidth)
                            offsetY = offsetY.coerceIn(-maxOffsetHeight, maxOffsetHeight)
                        } else {
                            // If scale is back to 1f, reset offsets
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        ) {
            // Display the previous photo underneath during a right swipe gesture
            // This provides visual context for the swipe navigation.
            if (currentIndex > 0 && swipeProgress > 0) {
                val previousPhoto = photos[currentIndex - 1]
                SubcomposeAsyncImage( // Use SubcomposeAsyncImage for loading state handling
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(previousPhoto.url)
                        .crossfade(true) // Enable crossfade animation
                        .build(),
                    contentDescription = null, // Decorative image during transition
                    contentScale = ContentScale.Fit, // Fit the image within bounds
                    // Show a loading indicator while the previous image loads
                    loading = {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                color = PrimaryAccent, // Use accent color for loading
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        // Apply translation based on swipe progress to slide it into view
                        .graphicsLayer {
                            translationX = -size.width + (dragProgress * size.width)
                        }
                )
            }

            // Display the next photo underneath during a left swipe gesture
            // This provides visual context for the swipe navigation.
            if (currentIndex < photos.size - 1 && swipeProgress < 0) {
                val nextPhoto = photos[currentIndex + 1]
                SubcomposeAsyncImage( // Use SubcomposeAsyncImage for loading state handling
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(nextPhoto.url)
                        .crossfade(true) // Enable crossfade animation
                        .build(),
                    contentDescription = null, // Decorative image during transition
                    contentScale = ContentScale.Fit, // Fit the image within bounds
                    // Show a loading indicator while the next image loads
                    loading = {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                color = PrimaryAccent, // Use accent color for loading
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        // Apply translation based on swipe progress to slide it into view
                        .graphicsLayer {
                            translationX = size.width + (dragProgress * size.width)
                        }
                )
            }

            // Display the primary, currently selected photo.
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentPhoto.url)
                    .crossfade(true)
                    .build(),
                contentDescription = currentPhoto.title.ifEmpty { "Full screen image" }, // Use title or default
                contentScale = ContentScale.Fit, // Fit the image initially
                // Show a prominent loading indicator for the main image
                loading = {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            color = PrimaryAccent,
                            strokeWidth = 5.dp, // Thicker indicator
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    // Apply transformations: zoom (scaleX, scaleY), pan (translationX/Y when zoomed),
                    // or swipe translation (translationX when not zoomed).
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        // Apply pan offset if zoomed, otherwise apply swipe progress translation
                        translationX = if (scale > 1f) offsetX else (dragProgress * size.width)
                        translationY = offsetY // Vertical pan offset only applies when zoomed
                    }
            )

            // Display visual indicators (arrows) at the screen edges during a swipe gesture
            // Only shown when a swipe is actively in progress (`swipeProgress != 0f`).
            if (swipeProgress != 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp) // Padding from screen edges
                ) {
                    // Left arrow indicator (shown when swiping right to previous photo)
                    if (swipeProgress > 0.05f && currentIndex > 0) { // Threshold to appear
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart) // Position on the left edge
                                .size(56.dp) // Larger indicator size
                                .alpha(swipeProgress * 2.5f) // Fade in based on swipe amount
                                .background(PrimaryAccent.copy(alpha = 0.8f), RoundedCornerShape(16.dp)) // Styled background
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft, // Specific arrow icon
                                contentDescription = "Previous Hint", // Accessibility text
                                tint = TextColorPrimary, // Text color for icon
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp) // Icon size within the box
                            )
                        }
                    }

                    // Right arrow indicator (shown when swiping left to next photo)
                    if (swipeProgress < -0.05f && currentIndex < photos.size - 1) { // Threshold to appear
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd) // Position on the right edge
                                .size(56.dp) // Larger indicator size
                                .alpha(abs(swipeProgress) * 2.5f) // Fade in based on swipe amount
                                .background(PrimaryAccent.copy(alpha = 0.8f), RoundedCornerShape(16.dp)) // Styled background
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight, // Specific arrow icon
                                contentDescription = "Next Hint", // Accessibility text
                                tint = TextColorPrimary, // Text color for icon
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp) // Icon size within the box
                            )
                        }
                    }
                }
            }

            // Top control bar area (contains back button, title, actions)
            // Uses AnimatedVisibility to fade in/out based on `showControls` state.
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(animationSpec = tween(durationMillis = 400)), // Fade-in animation
                exit = fadeOut(animationSpec = tween(durationMillis = 400)), // Fade-out animation
                modifier = Modifier.align(Alignment.TopCenter) // Position at the top
            ) {
                // Gradient overlay behind the app bar for better text/icon visibility against the photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp) // Height of the gradient area
                        .background(
                            Brush.verticalGradient( // Apply a vertical gradient
                                colors = listOf(
                                    DarkBackground.copy(alpha = 0.9f), // Darker at the top
                                    Color.Transparent // Fades to transparent
                                )
                            )
                        )
                ) {
                    // Material 3 Top App Bar composable
                    CenterAlignedTopAppBar(
                        // Display the photo title, centered. Provide default if title is empty.
                        title = {
                            Text(
                                text = currentPhoto.title.ifEmpty { "Untitled Image" },
                                color = TextColorPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, // Prevent wrapping
                                overflow = TextOverflow.Ellipsis, // Add ellipsis if too long
                                fontStyle = FontStyle.Italic // Apply italic style
                            )
                        },
                        // Navigation icon (typically back or close)
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) { // Trigger navigation back on click
                                Icon(
                                    Icons.Default.Close, // Use Close icon
                                    contentDescription = "Close Viewer", // Accessibility text
                                    tint = TextColorPrimary // Icon color
                                )
                            }
                        },
                        // Action icons on the right side of the app bar
                        actions = {
                            // Favorite button: Toggles favorite status via ViewModel
                            IconButton(onClick = { viewModel.toggleFavorite(currentPhoto.id) }) {
                                Icon(
                                    // Show filled heart if favorite, border if not
                                    imageVector = if (currentPhoto.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (currentPhoto.isFavorite) "Unfavorite" else "Favorite",
                                    // Use accent color for active favorite, primary text color otherwise
                                    tint = if (currentPhoto.isFavorite) FavoriteColorActive else TextColorPrimary
                                )
                            }

                            // Zoom button: Toggles zoom or resets zoom
                            IconButton(
                                onClick = {
                                    // If currently zoomed in, zoom out (reset)
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        // If not zoomed in, zoom in (to 2x)
                                        scale = 2.0f
                                    }
                                    resetControlsTimer() // Reset hide timer on interaction
                                }
                            ) {
                                Icon(
                                    // Show ZoomOut icon if zoomed, ZoomIn icon otherwise
                                    if (scale > 1f) Icons.Outlined.ZoomOut else Icons.Outlined.ZoomIn,
                                    contentDescription = if (scale > 1f) "Zoom Out" else "Zoom In",
                                    tint = TextColorPrimary // Icon color
                                )
                            }
                        },
                        // Make the app bar container transparent to see the gradient behind it
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }

            // Bottom control bar area (contains photo counter, navigation buttons)
            // Uses AnimatedVisibility to fade in/out based on `showControls` state.
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(animationSpec = tween(durationMillis = 400)), // Fade-in animation
                exit = fadeOut(animationSpec = tween(durationMillis = 400)), // Fade-out animation
                modifier = Modifier.align(Alignment.BottomCenter) // Position at the bottom
            ) {
                // Gradient overlay behind the bottom controls for better visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Height of the gradient area
                        .background(
                            Brush.verticalGradient( // Apply a vertical gradient
                                colors = listOf(
                                    Color.Transparent, // Transparent at the top
                                    DarkBackground.copy(alpha = 0.9f) // Darker at the bottom
                                )
                            )
                        )
                ) {
                    // Column to arrange bottom elements vertically
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter) // Align content to the bottom of the Box
                            .padding(bottom = 8.dp) // Add padding at the very bottom
                    ) {
                        // Text displaying the current photo index and total count
                        Text(
                            text = "Image ${currentIndex + 1} of ${photos.size}", // User-friendly format
                            color = TextColorSecondary, // Less prominent text color
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center, // Center the text
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp) // Padding around the text
                        )

                        // Row containing the Previous/Next navigation buttons and index indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 12.dp), // Padding around the row
                            horizontalArrangement = Arrangement.SpaceBetween, // Space buttons apart
                            verticalAlignment = Alignment.CenterVertically // Align items vertically center
                        ) {
                            // Previous Button (IconButton)
                            IconButton(
                                onClick = { viewModel.previousPhoto() }, // Trigger previous photo action
                                enabled = currentIndex > 0, // Disable if it's the first photo
                                modifier = Modifier
                                    .size(48.dp) // Standard IconButton size
                                    .background( // Apply background color based on enabled state
                                        if (currentIndex > 0) SecondaryAccent else SecondaryAccent.copy(alpha = 0.3f),
                                        shape = CircleShape // Circular background
                                    )
                            ) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew, // iOS-style back arrow
                                    contentDescription = "Previous Image", // Accessibility text
                                    // Change tint based on enabled state
                                    tint = if (currentIndex > 0) TextColorPrimary else TextColorSecondary
                                )
                            }

                            // Display current index number prominently between buttons
                            Text(
                                text = "${currentIndex + 1}", // Show 1-based index
                                color = TextColorPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Next Button (IconButton)
                            IconButton(
                                onClick = { viewModel.nextPhoto() }, // Trigger next photo action
                                enabled = currentIndex < photos.size - 1, // Disable if it's the last photo
                                modifier = Modifier
                                    .size(48.dp) // Standard IconButton size
                                    .background( // Apply background color based on enabled state
                                        if (currentIndex < photos.size - 1) SecondaryAccent else SecondaryAccent.copy(alpha = 0.3f),
                                        shape = CircleShape // Circular background
                                    )
                            ) {
                                Icon(
                                    Icons.Default.ArrowForwardIos, // iOS-style forward arrow
                                    contentDescription = "Next Image", // Accessibility text
                                    // Change tint based on enabled state
                                    tint = if (currentIndex < photos.size - 1) TextColorPrimary else TextColorSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Initial Gesture Instruction Overlay
            // State variable to control if the instructions have been shown
            var showInstructions by remember { mutableStateOf(true) }
            // Display the overlay only if `showInstructions` is true
            if (showInstructions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Dark, semi-transparent background to overlay the photo
                        .background(DarkBackground.copy(alpha = 0.95f))
                        // Make the entire overlay clickable to dismiss it
                        .clickable {
                            showInstructions = false // Set state to false to hide overlay
                            resetControlsTimer() // Start the controls timer after dismissing instructions
                        }
                ) {
                    // Column to arrange instruction elements vertically
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center) // Center the column on the screen
                            .padding(horizontal = 24.dp, vertical = 48.dp), // Padding within the overlay
                        horizontalAlignment = Alignment.CenterHorizontally, // Center items horizontally
                        verticalArrangement = Arrangement.spacedBy(24.dp) // Space between instruction rows
                    ) {
                        // Overlay Title
                        Text(
                            "Photo Viewer Tips",
                            color = PrimaryAccent, // Use accent color for title
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Use the helper composable for each instruction row
                        InstructionRow(icon = Icons.Default.Swipe, text = "Swipe Left/Right to change photos")
                        InstructionRow(icon = Icons.Default.ZoomInMap, text = "Pinch or Double-Tap to Zoom")
                        InstructionRow(icon = Icons.Default.TouchApp, text = "Single Tap to show/hide controls")
                        InstructionRow(icon = Icons.Default.DragIndicator, text = "Drag when zoomed to pan around")

                        // Dismiss Prompt Text
                        Text(
                            "Tap anywhere to start exploring!",
                            color = TextColorSecondary.copy(alpha = 0.8f), // Less prominent color
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 24.dp) // Add space above dismiss text
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper composable to display a single row within the instruction overlay,
 * containing an icon and corresponding descriptive text.
 * Reduces code repetition in the main `FullPhotoScreen` composable.
 *
 * @param icon The ImageVector for the icon to display.
 * @param text The instruction text associated with the icon.
 */
@Composable
private fun InstructionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    // Row layout for icon and text
    Row(
        verticalAlignment = Alignment.CenterVertically, // Align icon and text vertically
        modifier = Modifier.padding(vertical = 8.dp) // Padding above/below the row
    ) {
        // Instruction Icon
        Icon(
            imageVector = icon,
            contentDescription = null, // Icon is decorative within the instruction text
            tint = TextColorPrimary, // Use primary text color for the icon
            modifier = Modifier.size(32.dp) // Set icon size
        )
        // Spacer between icon and text
        Spacer(modifier = Modifier.width(20.dp))
        // Instruction Text
        Text(
            text = text,
            color = TextColorPrimary, // Use primary text color
            fontSize = 17.sp // Set text size
        )
    }
}