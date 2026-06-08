package com.callrecorder.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileManager {
    private const val TAG = "FileManager"

    /**
     * Copy a local file to a custom folder selected via Storage Access Framework (SAF)
     * @param context Application context
     * @param localFile The local File to copy
     * @param folderUriStr The persistent SAF tree Uri string
     * @return The path/uri of the copied file, or null if failed
     */
    fun copyFileToSafFolder(context: Context, localFile: File, folderUriStr: String?): String? {
        if (folderUriStr.isNullOrEmpty()) return null

        try {
            val folderUri = Uri.parse(folderUriStr)
            val documentDir = DocumentFile.fromTreeUri(context, folderUri)
            
            if (documentDir == null || !documentDir.exists()) {
                Log.e(TAG, "SAF folder does not exist or permission lost: $folderUriStr")
                return null
            }

            // Create document in SAF folder
            val displayName = localFile.name
            val mimeType = "audio/mp4" // AAC files are mp4 format
            val docFile = documentDir.createFile(mimeType, displayName)

            if (docFile == null) {
                Log.e(TAG, "Failed to create document in SAF folder")
                return null
            }

            // Copy streams
            context.contentResolver.openOutputStream(docFile.uri)?.use { output ->
                FileInputStream(localFile).use { input ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "Successfully copied file to SAF folder: ${docFile.uri}")
            return docFile.uri.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to SAF folder", e)
            return null
        }
    }
}
