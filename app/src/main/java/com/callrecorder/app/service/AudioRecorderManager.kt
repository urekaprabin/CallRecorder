package com.callrecorder.app.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

    @Synchronized
    fun startRecording(outputFile: File, audioSource: Int): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording. Stopping current recording first.")
            stopRecording()
        }

        currentFile = outputFile
        
        return try {
            // Instantiate MediaRecorder depending on API version
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder = recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            Log.i(TAG, "Recording started successfully. Output: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            isRecording = false
            currentFile = null
            mediaRecorder = null
            false
        }
    }

    @Synchronized
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w(TAG, "stopRecording: Not currently recording.")
            return null
        }

        val file = currentFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            Log.i(TAG, "Recording stopped successfully. File: ${file?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder (might be call was too short)", e)
            // If stop fails (e.g. call was less than 1 second), delete the empty file
            file?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        } finally {
            mediaRecorder = null
            isRecording = false
            currentFile = null
        }
        return file
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    companion object {
        private const val TAG = "AudioRecorderManager"
    }
}
