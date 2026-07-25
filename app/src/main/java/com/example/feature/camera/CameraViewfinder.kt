package com.example.feature.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Live CameraX preview surface plus the runtime-permission gate.
 *
 * The screen previously rendered a remote Unsplash JPEG here and never asked
 * for CAMERA at all, so on a real device the "camera" showed a stock photo of
 * somebody else's night out.
 */
@Composable
fun CameraViewfinder(
    controller: CameraCaptureController,
    useFrontCamera: Boolean,
    flashMode: Int,
    modifier: Modifier = Modifier,
    onPermissionResult: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(CameraCaptureController.hasCameraPermission(context))
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermission = grants[Manifest.permission.CAMERA] == true
        permissionRequested = true
        onPermissionResult(hasPermission)
    }

    // Ask on first composition rather than making the user hunt for a button.
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        } else {
            onPermissionResult(true)
        }
    }

    // Rebind whenever the lens or flash changes.
    LaunchedEffect(hasPermission, useFrontCamera, flashMode) {
        if (!hasPermission) return@LaunchedEffect
        controller.bind(lifecycleOwner, previewView, useFrontCamera, flashMode)
            .onFailure { onError(it.message ?: "Unable to start the camera.") }
    }

    // Always release the camera when leaving the screen, otherwise it stays
    // locked and every other app (and this one, on re-entry) sees a black frame.
    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    Box(modifier = modifier.background(Color.Black)) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            CameraPermissionPlaceholder(
                permanentlyDenied = permissionRequested,
                onRequest = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                }
            )
        }
    }
}

@Composable
private fun CameraPermissionPlaceholder(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "Camera access needed",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = if (permanentlyDenied) {
                "FOMO can't open the camera. Enable the Camera permission in " +
                    "Settings › Apps › FOMO › Permissions to capture moments."
            } else {
                "Allow camera access to capture and share moments from tonight."
            },
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("Allow camera", fontWeight = FontWeight.Bold)
        }
    }
}
