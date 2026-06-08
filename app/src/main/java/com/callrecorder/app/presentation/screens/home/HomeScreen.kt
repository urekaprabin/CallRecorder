package com.callrecorder.app.presentation.screens.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callrecorder.app.domain.model.Recording
import com.callrecorder.app.presentation.screens.player.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val showStarredOnly by homeViewModel.showStarredOnly.collectAsState()
    val isSyncing by homeViewModel.isSyncing.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recordings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { homeViewModel.triggerManualSync() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Xiaomi recordings")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { homeViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search by name or number...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { homeViewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Starred Filter Chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    FilterChip(
                        selected = showStarredOnly,
                        onClick = { homeViewModel.toggleStarredFilter() },
                        label = { Text("Starred Only") },
                        leadingIcon = {
                            if (showStarredOnly) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }

                // Recordings List
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is HomeUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is HomeUiState.Success -> {
                        val recordings = state.recordings
                        if (recordings.isEmpty()) {
                            EmptyRecordingsState()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp) // Bottom padding for player bar
                            ) {
                                items(recordings, key = { it.id }) { recording ->
                                    RecordingItemCard(
                                        recording = recording,
                                        onPlayClick = { playerViewModel.playRecording(recording) },
                                        onDeleteClick = { homeViewModel.deleteRecording(recording) },
                                        onStarClick = { homeViewModel.toggleStar(recording) },
                                        onSaveNotes = { homeViewModel.updateNotes(recording, it) },
                                        isSelected = playerState.currentRecording?.id == recording.id
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expanding Bottom Player Bar
            AnimatedVisibility(
                visible = playerState.currentRecording != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                playerState.currentRecording?.let { recording ->
                    BottomPlayerPanel(
                        recording = recording,
                        isPlaying = playerState.isPlaying,
                        position = playerState.currentPosition,
                        duration = playerState.duration,
                        speed = playerState.playbackSpeed,
                        onPlayPauseToggle = { playerViewModel.togglePlayPause() },
                        onSeek = { playerViewModel.seekTo(it) },
                        onSpeedChange = { playerViewModel.setSpeed(it) },
                        onClose = { playerViewModel.stopPlayback() }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingItemCard(
    recording: Recording,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStarClick: () -> Unit,
    onSaveNotes: (String?) -> Unit,
    isSelected: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf(recording.notes ?: "") }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete this call recording? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Call Notes") },
            text = {
                Column {
                    Text("Add details or notes about this call conversation:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("Enter notes here...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveNotes(if (notesText.isBlank()) null else notesText)
                        showNotesDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        notesText = recording.notes ?: ""
                        showNotesDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call Type Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (recording.callType == "INCOMING") Color(0x1F4CAF50) 
                        else Color(0x1F2196F3)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (recording.callType == "INCOMING") Icons.Default.CallReceived 
                                 else Icons.Default.CallMade,
                    contentDescription = null,
                    tint = if (recording.callType == "INCOMING") Color(0xFF4CAF50) 
                           else Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.contactName ?: recording.phoneNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (recording.contactName != null) {
                    Text(
                        text = recording.phoneNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Show call notes if exist
                if (!recording.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Note: ${recording.notes}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateTime(recording.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatDuration(recording.duration),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sync Badge / Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Star Button
                IconButton(onClick = onStarClick) {
                    Icon(
                        imageVector = if (recording.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star call",
                        tint = if (recording.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Note Button
                IconButton(onClick = { showNotesDialog = true }) {
                    Icon(
                        imageVector = if (recording.notes != null) Icons.Default.EditNote else Icons.Default.NoteAdd,
                        contentDescription = "Edit notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (recording.source == "NATIVE_SYNC") {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Native", fontSize = 10.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete call",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyRecordingsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Call Recordings Yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Make a phone call or click the Refresh button above to scan and sync native Xiaomi recordings.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BottomPlayerPanel(
    recording: Recording,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    speed: Float,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Top row: Contact, speed selector and close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.contactName ?: recording.phoneNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (recording.callType == "INCOMING") "Incoming Call" else "Outgoing Call",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Speed button
                val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text("${speed}x", fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        speeds.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s}x") },
                                onClick = {
                                    onSpeedChange(s)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close player")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seek bar / Slider
            val maxRange = if (duration > 0) duration.toFloat() else 1f
            Slider(
                value = position.toFloat().coerceIn(0f, maxRange),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxRange,
                modifier = Modifier.fillMaxWidth()
            )

            // Timeline text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(position),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(duration),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action: Play/Pause central toggle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// Format utilities
private fun formatDateTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val timeCalendar = Calendar.getInstance().apply { time = date }

    return if (now.get(Calendar.DATE) == timeCalendar.get(Calendar.DATE)) {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        "Today, " + format.format(date)
    } else {
        val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        format.format(date)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSecs = millis / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
