package com.callrecorder.app.service

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callrecorder.app.domain.model.Recording
import com.callrecorder.app.domain.repository.RecordingRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class NativeFolderSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun recordingRepository(): RecordingRepository
    }

    private val recordingRepository: RecordingRepository by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        ).recordingRepository()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting periodic native folder sync...")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customFolderUriStr = prefs.getString(KEY_CUSTOM_FOLDER_URI, null)

        // Sync from Xiaomi Native Folder:
        // Attempt 1: Direct File API (works if permissions granted or older SDK)
        val nativeDir = File(NATIVE_XIAOMI_PATH)
        if (nativeDir.exists() && nativeDir.isDirectory) {
            Log.d(TAG, "Direct access to Xiaomi path available. Scanning files...")
            scanDirectDirectory(nativeDir)
        }

        // Attempt 2: DocumentFile SAF (best for Android 15 scoped storage)
        if (!customFolderUriStr.isNullOrEmpty()) {
            try {
                val folderUri = Uri.parse(customFolderUriStr)
                // Take persistable permission check
                context.contentResolver.takePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val documentDir = DocumentFile.fromTreeUri(context, folderUri)
                if (documentDir != null && documentDir.isDirectory) {
                    Log.d(TAG, "Scanning SAF directory: ${documentDir.uri}")
                    scanDocumentDirectory(documentDir)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan SAF directory", e)
            }
        }

        Result.success()
    }

    private suspend fun scanDirectDirectory(directory: File) {
        val files = directory.listFiles { _, name ->
            name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".wav") || name.endsWith(".amr")
        } ?: return

        for (file in files) {
            val dbRecord = recordingRepository.getRecordingByFilePath(file.absolutePath)
            if (dbRecord == null) {
                // New call recording found! Parse and import it.
                importDirectFile(file)
            }
        }
    }

    private suspend fun scanDocumentDirectory(directory: DocumentFile) {
        val files = directory.listFiles()
        for (docFile in files) {
            if (docFile.isFile && (docFile.name?.endsWith(".mp3") == true ||
                        docFile.name?.endsWith(".m4a") == true ||
                        docFile.name?.endsWith(".wav") == true ||
                        docFile.name?.endsWith(".amr") == true)) {
                
                val path = docFile.uri.toString()
                val dbRecord = recordingRepository.getRecordingByFilePath(path)
                if (dbRecord == null) {
                    importDocumentFile(docFile)
                }
            }
        }
    }

    private suspend fun importDirectFile(file: File) {
        Log.i(TAG, "Importing native file: ${file.name}")
        val parsedMetadata = parseFileName(file.name)
        val phoneNumber = parsedMetadata.phoneNumber
        val timestamp = parsedMetadata.timestamp
        val contactName = resolveContactName(phoneNumber)

        // Copy file to app-specific directory for persistence
        val appRecordingsDir = File(context.filesDir, "recordings")
        if (!appRecordingsDir.exists()) appRecordingsDir.mkdirs()

        val destFile = File(appRecordingsDir, file.name)
        try {
            file.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val recording = Recording(
                phoneNumber = phoneNumber,
                contactName = contactName,
                callType = parsedMetadata.callType,
                duration = 0, // Duration will be calculated on play or retrieved via MediaMetadataRetriever
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                timestamp = timestamp,
                source = "NATIVE_SYNC"
            )
            recordingRepository.insertRecording(recording)
            Log.d(TAG, "Imported file successfully saved to database: ${destFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy and import direct file: ${file.name}", e)
        }
    }

    private suspend fun importDocumentFile(docFile: DocumentFile) {
        Log.i(TAG, "Importing SAF file: ${docFile.name}")
        val name = docFile.name ?: "Unknown"
        val parsedMetadata = parseFileName(name)
        val phoneNumber = parsedMetadata.phoneNumber
        val timestamp = parsedMetadata.timestamp
        val contactName = resolveContactName(phoneNumber)

        // Copy SAF file to local cache
        val appRecordingsDir = File(context.filesDir, "recordings")
        if (!appRecordingsDir.exists()) appRecordingsDir.mkdirs()

        val destFile = File(appRecordingsDir, name)
        try {
            context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val recording = Recording(
                phoneNumber = phoneNumber,
                contactName = contactName,
                callType = parsedMetadata.callType,
                duration = 0,
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                timestamp = timestamp,
                source = "NATIVE_SYNC"
            )
            recordingRepository.insertRecording(recording)
            Log.d(TAG, "Imported SAF file successfully saved to database: ${destFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy and import SAF file: $name", e)
        }
    }

    // Helper data structure
    private data class ParsedCall(val phoneNumber: String, val timestamp: Long, val callType: String)

    private fun parseFileName(fileName: String): ParsedCall {
        // Xiaomi files are typically named in patterns like:
        // "John Doe_1234567890_20260608153022.mp3" or "1234567890_20260608153022.mp3"
        // Let's build a regex parser.
        var phoneNumber = "Unknown"
        var timestamp = System.currentTimeMillis()
        var callType = "INCOMING" // default fallback

        try {
            // Find numbers and dates in the name
            val numberPattern = Pattern.compile("(\\+?\\d{10,14})")
            val numberMatcher = numberPattern.matcher(fileName)
            if (numberMatcher.find()) {
                phoneNumber = numberMatcher.group(1) ?: "Unknown"
            }

            // Extract date (usually 14 digits like 20260608153022)
            val datePattern = Pattern.compile("(\\d{14})")
            val dateMatcher = datePattern.matcher(fileName)
            if (dateMatcher.find()) {
                val dateStr = dateMatcher.group(1)
                if (dateStr != null) {
                    val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    val parsedDate = format.parse(dateStr)
                    if (parsedDate != null) {
                        timestamp = parsedDate.time
                    }
                }
            }

            // Simple heuristic to detect call type based on file name contents
            if (fileName.lowercase().contains("outgoing") || fileName.lowercase().contains("out")) {
                callType = "OUTGOING"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse file name metadata: $fileName", e)
        }

        return ParsedCall(phoneNumber, timestamp, callType)
    }

    private fun resolveContactName(number: String): String? {
        if (number == "Unknown") return null
        var contactName: String? = null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (columnIndex != -1) {
                    contactName = cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact name", e)
        } finally {
            cursor?.close()
        }
        return contactName
    }

    companion object {
        private const val TAG = "NativeFolderSyncWorker"
        const val WORK_NAME = "com.callrecorder.app.service.NativeFolderSyncWorker"
        private const val NATIVE_XIAOMI_PATH = "/storage/emulated/0/MIUI/sound_recorder/call_rec"

        private const val PREFS_NAME = "call_recorder_prefs"
        private const val KEY_CUSTOM_FOLDER_URI = "custom_folder_uri"
    }
}

