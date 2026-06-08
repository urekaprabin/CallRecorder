package com.callrecorder.app.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.callrecorder.app.domain.model.Recording
import com.callrecorder.app.domain.repository.RecordingRepository
import com.callrecorder.app.service.NativeFolderSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val recordings: List<Recording>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showStarredOnly = MutableStateFlow(false)
    val showStarredOnly: StateFlow<Boolean> = _showStarredOnly.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(_searchQuery, _showStarredOnly) { query, starred ->
        query to starred
    }
        .debounce(300)
        .flatMapLatest { (query, starred) ->
            val flow = if (query.isBlank()) {
                recordingRepository.getAllRecordings()
            } else {
                recordingRepository.searchRecordings(query)
            }
            flow.map { list ->
                if (starred) list.filter { it.isStarred } else list
            }
        }
        .map { list ->
            HomeUiState.Success(list) as HomeUiState
        }
        .catch { e ->
            emit(HomeUiState.Error(e.message ?: "An unknown error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading as HomeUiState
        )

    fun toggleStarredFilter() {
        _showStarredOnly.value = !_showStarredOnly.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recording)
            // Delete local file too
            val file = java.io.File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun toggleStar(recording: Recording) {
        viewModelScope.launch {
            recordingRepository.updateRecording(recording.copy(isStarred = !recording.isStarred))
        }
    }

    fun updateNotes(recording: Recording, notes: String?) {
        viewModelScope.launch {
            recordingRepository.updateRecording(recording.copy(notes = notes))
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val syncRequest = OneTimeWorkRequestBuilder<NativeFolderSyncWorker>().build()
            
            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(syncRequest)
            
            // Wait briefly for completion or just simulate spinner since WorkManager runs in background
            kotlinx.coroutines.delay(1500)
            _isSyncing.value = false
        }
    }
}
