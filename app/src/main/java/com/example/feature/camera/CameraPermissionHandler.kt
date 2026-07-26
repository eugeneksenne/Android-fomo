package com.example.feature.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * State holding Camera and Audio runtime permission statuses.
 */
data class CameraPermissionState(
    val hasCameraPermission: Boolean = false,
    val hasAudioPermission: Boolean = false
) {
    val allGranted: Boolean get() = hasCameraPermission && hasAudioPermission
}

/**
 * Helper class for checking and managing CAMERA and RECORD_AUDIO permissions
 * required for FOMO CameraX preview and Live Broadcasting.
 */
class CameraPermissionHandler(private val context: Context) {

    /**
     * Checks if CAMERA permission is currently granted.
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if RECORD_AUDIO permission is currently granted.
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks all required permissions for Camera and Audio streaming.
     */
    fun checkPermissionState(): CameraPermissionState {
        return CameraPermissionState(
            hasCameraPermission = hasCameraPermission(),
            hasAudioPermission = hasAudioPermission()
        )
    }

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}

/**
 * Composable state wrapper that manages permission requests and state updates.
 */
@Composable
fun rememberCameraPermissionState(): State<CameraPermissionState> {
    val context = LocalContext.current
    val handler = remember(context) { CameraPermissionHandler(context) }
    
    var permissionState by remember {
        mutableStateOf(handler.checkPermissionState())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionState = handler.checkPermissionState()
    }

    LaunchedEffect(Unit) {
        permissionState = handler.checkPermissionState()
        if (!permissionState.hasCameraPermission || !permissionState.hasAudioPermission) {
            permissionLauncher.launch(CameraPermissionHandler.REQUIRED_PERMISSIONS)
        }
    }

    return rememberUpdatedState(permissionState)
}

/**
 * Renders a permission request rationale card when permissions are missing.
 */
@Composable
fun CameraPermissionRationaleCard(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.88f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.padding(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission",
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(32.dp)
                )
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Audio Permission",
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Camera & Audio Access Needed",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = "FOMO requires Camera and Microphone permissions to capture club moments, stream live video feeds, and analyze venue BPM.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Grant Permissions",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
