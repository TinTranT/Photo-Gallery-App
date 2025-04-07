package com.example.photogalleryapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.photogalleryapp.viewmodel.PhotoGalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PhotoGalleryViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    val topBarElevation by animateDpAsState(
        targetValue = if (scrollState.value > 0) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "App Configuration",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer // Theme Change
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary) // Theme Change
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onSecondary // Theme Change
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer, // Theme Change
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer // Theme Change
                ),
                modifier = Modifier.shadow(topBarElevation)
            )
        }
    ) { padding ->
        Surface(
            color = if (MaterialTheme.colorScheme.isLight()) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background , // Theme Change
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                SettingsSection(
                    title = "Display Options",
                    icon = Icons.Default.DarkMode,
                    iconTint = MaterialTheme.colorScheme.tertiary // Theme Change
                ) {
                    SwitchSettingItem(
                        title = "Enable Night Mode",
                        subtitle = "Adjust app colors for low light",
                        checked = viewModel.isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() }
                    )
                }

                SettingsSection(
                    title = "Manage Favorites",
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFD32F2F) // Theme Change (Slightly different red)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            "Manage your collection of saved photos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val favoriteCount = viewModel.getFavoriteCount()
                        AnimatedVisibility(
                            visible = favoriteCount > 0,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) // Theme Change
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, // Theme Change
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "You have $favoriteCount saved photos",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer // Theme Change
                                )
                            }
                        }

                        Button(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer, // Keep error related for clarity
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .animateContentSize(),
                            shape = RoundedCornerShape(26.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Remove All Favorites",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                SettingsSection(
                    title = "Information",
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary // Theme Change
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AboutItem(title = "App Version", value = "v1.0.1-beta")
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), thickness = 0.5.dp) // Theme Change (subtler divider)
                        AboutItem(title = "Framework", value = "Compose Multiplatform")
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), thickness = 0.5.dp) // Theme Change
                        AboutItem(title = "Developer", value = "AI Assistant Coder")
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer, // Theme Change (M3 standard surface container)
            title = {
                Text(
                    "Confirm Deletion",
                    color = MaterialTheme.colorScheme.onSurface // Theme Change
                )
            },
            text = {
                Text(
                    "This will permanently remove all your favorited photos. Are you absolutely sure?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Theme Change
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllFavorites()
                        showClearDialog = false
                        scope.launch {
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error // Keep error red
                    )
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary) // Theme Change
                ) {
                    Text("No, Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTint.copy(alpha = 0.1f)), // Theme Change (Use iconTint's base with alpha)
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal // Theme Change (removed italic)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant // Theme Change (less emphasis)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer // Theme Change (M3 standard)
            ),
            elevation = CardDefaults.cardElevation( // Theme Change (no elevation)
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            content()
        }
    }
}

@Composable
fun ColorScheme.isLight(): Boolean {
    return this.primary.luminance() > 0.5f
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface // Theme Change
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        val switchScale = remember { Animatable(1f) }
        LaunchedEffect(checked) {
            switchScale.animateTo(
                targetValue = 0.8f,
                animationSpec = tween(100)
            )
            switchScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.graphicsLayer {
                scaleX = switchScale.value
                scaleY = switchScale.value
            },
            colors = SwitchDefaults.colors( // Theme Change
                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark Mode Active",
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}

@Composable
fun AboutItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary // Theme Change
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}