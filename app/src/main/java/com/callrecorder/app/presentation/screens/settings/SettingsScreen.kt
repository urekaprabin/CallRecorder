package com.callrecorder.app.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serviceEnabled by viewModel.serviceEnabled.collectAsState()
    val recordingMode by viewModel.recordingMode.collectAsState()
    val autoSpeakerphone by viewModel.autoSpeakerphone.collectAsState()
    val customFolderUri by viewModel.customFolderUri.collectAsState()

    // Activity launcher for choosing directory (Storage Access Framework)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read/write permissions for future accesses
            try {
                val contentResolver = context.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                viewModel.setCustomFolderUri(uri.toString())
                Toast.makeText(context, "Custom save directory saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Failed to persist SAF permission", e)
                Toast.makeText(context, "Failed to save directory permission.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Section 1: Master Control
            SettingsSectionHeader(title = "Recording Control")
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master Recording Toggle", fontWeight = FontWeight.Bold)
                        Text(
                            "Enable or disable all call recording and synchronization triggers.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = { viewModel.setServiceEnabled(it) }
                    )
                }
            }

            // Section 2: Recording Strategy Mode
            SettingsSectionHeader(title = "Recording Mode")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Recording Strategy:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    val options = listOf(
                        "HYBRID" to "Hybrid Mode (Recommended)\nUses app-recorder and watches Xiaomi system folder.",
                        "APP" to "App Recorder Only\nRecords directly using device microphone (requires speakerphone).",
                        "NATIVE" to "Xiaomi Native Sync Only\nWatches the MIUI call recordings folder and registers them."
                    )

                    options.forEach { (key, desc) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (recordingMode == key),
                                    onClick = { viewModel.setRecordingMode(key) }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (recordingMode == key),
                                onClick = { viewModel.setRecordingMode(key) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = desc, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Section 3: App Recorder Subsettings
            if (recordingMode == "APP" || recordingMode == "HYBRID") {
                SettingsSectionHeader(title = "App Recorder Settings")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Speakerphone", fontWeight = FontWeight.Bold)
                            Text(
                                "Automatically turn on speakerphone during active calls so the microphone can capture the caller's voice.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSpeakerphone,
                            onCheckedChange = { viewModel.setAutoSpeakerphone(it) }
                        )
                    }
                }
            }

            // Section 4: Storage Location settings
            SettingsSectionHeader(title = "Storage Destination")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Save recordings to:", fontWeight = FontWeight.Bold)
                    Text(
                        "By default files are saved internally. Choose a custom directory to easily access recordings in your file manager.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (customFolderUri.isEmpty()) "Select Custom Folder" 
                                   else "Change Custom Folder"
                        )
                    }

                    if (customFolderUri.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val parsedUri = Uri.parse(customFolderUri)
                        Text(
                            text = "Path: ${parsedUri.lastPathSegment ?: "Custom Folder"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Section 5: Android 15 Restricted settings info
            SettingsSectionHeader(title = "Android 15 / Xiaomi Assistance")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("System Settings Info", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "If background services or overlays fail, click below to open application info, click 'Allow restricted settings' in the 3-dot menu, and enable Draw Over Other Apps permissions.",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Open Application Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
