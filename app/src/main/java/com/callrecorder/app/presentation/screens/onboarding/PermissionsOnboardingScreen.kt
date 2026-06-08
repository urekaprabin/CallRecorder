package com.callrecorder.app.presentation.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsOnboardingScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Permissions Setup", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to Call Recorder! To record, save, and catalog your phone calls automatically, we need a few system permissions.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Items Cards
            PermissionItem(
                icon = Icons.Default.Phone,
                title = "Phone & Call Logs",
                description = "Required to detect incoming/outgoing calls and identify caller numbers."
            )

            PermissionItem(
                icon = Icons.Default.Lock,
                title = "Microphone Access",
                description = "Required to record the call conversation audio."
            )

            // Android 15 Restricted Settings notice Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Xiaomi & Android 15 Sideload Warning",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Because this is a sideloaded personal app, Android 15's 'Enhanced Confirmation' blocks accessibility overlays. To enable them:\n\n" +
                                    "1. Go to your Phone Settings.\n" +
                                    "2. Go to Apps > Manage Apps > Call Recorder.\n" +
                                    "3. Tap the 3-dot menu at the top-right.\n" +
                                    "4. Tap 'Allow restricted settings'.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("App Info")
                }

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Grant Permissions")
                }
            }

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to App")
            }
        }
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
