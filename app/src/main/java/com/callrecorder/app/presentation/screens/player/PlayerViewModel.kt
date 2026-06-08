package com.callrecorder.app.presentation.screens.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.domain.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PlayerUiState(
    val currentRecording: Recording? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun playRecording(recording: Recording) {
        // Stop current if any
        stopPlayback()

        val isContentUri = recording.filePath.startsWith("content://")
        if (!isContentUri) {
            val file = File(recording.filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${recording.filePath}")
                return
            }
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                if (isContentUri) {
                    val uri = Uri.parse(recording.filePath)
                    setDataSource(context, uri)
                } else {
                    setDataSource(recording.filePath)
                }
                prepare()
                
                // Adjust speed on Android 6.0+ (API 23+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(_uiState.value.playbackSpeed)
                }

                start()
            }

            val fileDuration = mediaPlayer?.duration?.toLong() ?: recording.duration

            _uiState.value = _uiState.value.copy(
                currentRecording = recording,
                isPlaying = true,
                currentPosition = 0,
                duration = fileDuration
            )

            startProgressTracker()

            mediaPlayer?.setOnCompletionListener {
                stopProgressTracker()
                _uiState.value = _uiState.value.copy(
                    isPlaying = false,
                    currentPosition = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback", e)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        val currentState = _uiState.value

        if (currentState.isPlaying) {
            player.pause()
            stopProgressTracker()
            _uiState.value = currentState.copy(isPlaying = false)
        } else {
            player.start()
            startProgressTracker()
            _uiState.value = currentState.copy(isPlaying = true)
        }
    }

    fun seekTo(position: Long) {
        val player = mediaPlayer ?: return
        player.seekTo(position.toInt())
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        val player = mediaPlayer ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && player.isPlaying) {
            try {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to change playback speed", e)
            }
        }
    }

    fun stopPlayback() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                currentRecording = null,
                currentPosition = 0
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _uiState.value = _uiState.value.copy(
                            currentPosition = player.currentPosition.toLong()
                        )
                    }
                }
                delay(200) // Update progress every 200ms
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}
