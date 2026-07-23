package com.example.feature.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// A mathematically safe alternative to EaseOutBack that prevents CubicBezierEasing solver crashes near 1.0
val SafeEaseOutBack = Easing { fraction ->
    if (fraction >= 0.999f) 1f
    else {
        val t = fraction - 1f
        1f + 2.70158f * t * t * t + 1.70158f * t * t
    }
}

enum class WelcomeStep {
    LAUNCH,
    WELCOME,
    ONBOARDING,
    AUTH_HUB,
    SIGN_IN,
    SIGN_UP_STEP1,
    SIGN_UP_STEP2,
    VERIFICATION,
    LOCATION_PERMISSION,
    NOTIFICATION_PERMISSION,
    CINEMATIC_WELCOME
}

@Composable
fun WelcomeAuthScreen(
    onAuthComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(WelcomeStep.LAUNCH) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Navigation and workflow helper states
    var isNewUser by remember { mutableStateOf(false) }
    var userEmailOrPhone by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Safe Firebase Auth setup
    val auth = remember {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBWw5-i4emerWFtN95d-i41odIEb_UghLQ")
                    .setApplicationId("1:191228918156:android:6f30ad6655bb35688c3ea9")
                    .setProjectId("findlyts-4116c")
                    .setStorageBucket("findlyts-4116c.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Launch Screen / Auto-login check
    LaunchedEffect(auth) {
        if (currentStep == WelcomeStep.LAUNCH) {
            delay(1200) // Beautiful cinematic intro
            if (auth?.currentUser != null) {
                userName = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: ""
                userEmailOrPhone = auth.currentUser?.email ?: ""
                currentStep = WelcomeStep.CINEMATIC_WELCOME
            } else {
                currentStep = WelcomeStep.WELCOME
            }
        }
    }

    fun performGoogleSignIn() {
        val credentialManager = CredentialManager.create(context)
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }

        if (webClientId.isNullOrEmpty()) {
            authError = "Google Sign-In is not configured for this build. Please sign in using Email & Password or Guest Mode."
            Toast.makeText(context, authError, Toast.LENGTH_LONG).show()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        isLoading = true
        authError = null

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val firebaseAuth = auth ?: FirebaseAuth.getInstance()
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    
                    firebaseAuth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            userName = user?.displayName ?: "Google User"
                            userEmailOrPhone = user?.email ?: ""
                            Toast.makeText(context, "Google Sign-In successful!", Toast.LENGTH_SHORT).show()

                            if (user != null) {
                                try {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    val userDoc = mapOf(
                                        "uid" to user.uid,
                                        "displayName" to (user.displayName ?: "Google User"),
                                        "email" to (user.email ?: ""),
                                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                        "lastLoginAt" to com.google.firebase.Timestamp.now()
                                    )
                                    db.collection("users").document(user.uid).set(userDoc, com.google.firebase.firestore.SetOptions.merge())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            currentStep = WelcomeStep.LOCATION_PERMISSION
                        } else {
                            val exception = task.exception
                            authError = exception?.localizedMessage ?: "Google Authentication failed."
                            Toast.makeText(context, "Error: $authError", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    isLoading = false
                    authError = "Unexpected credential format received."
                    Toast.makeText(context, authError, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
                authError = e.localizedMessage ?: "Google Sign-In failed. Please sign in with Email & Password."
                Toast.makeText(context, authError, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun performSignIn(email: String, pword: String) {
        val cleanEmail = email.trim()
        val cleanPassword = pword.trim()
        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authError = "Please enter both email and password."
            Toast.makeText(context, authError, Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        authError = null
        
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(cleanEmail, cleanPassword)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    userName = user?.displayName ?: cleanEmail.substringBefore("@")
                    userEmailOrPhone = cleanEmail
                    Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()

                    if (user != null) {
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val userDoc = mapOf(
                                "uid" to user.uid,
                                "email" to cleanEmail,
                                "displayName" to (user.displayName ?: cleanEmail.substringBefore("@")),
                                "lastLoginAt" to com.google.firebase.Timestamp.now()
                            )
                            db.collection("users").document(user.uid).set(userDoc, com.google.firebase.firestore.SetOptions.merge())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    currentStep = WelcomeStep.LOCATION_PERMISSION
                } else {
                    val exception = task.exception
                    val errorMsg = exception?.localizedMessage ?: "Authentication failed. Please check your credentials."
                    authError = errorMsg
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun performSignUp(name: String, email: String, pword: String) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPassword = pword.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authError = "All fields are required."
            Toast.makeText(context, authError, Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        authError = null
        
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        displayName = cleanName
                    }
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        isLoading = false
                        userName = cleanName
                        userEmailOrPhone = cleanEmail
                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()

                        if (user != null) {
                            try {
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val userDoc = mapOf(
                                    "uid" to user.uid,
                                    "displayName" to cleanName,
                                    "email" to cleanEmail,
                                    "handle" to cleanEmail.substringBefore("@"),
                                    "createdAt" to com.google.firebase.Timestamp.now(),
                                    "bio" to "Nightlife explorer discovering hot spots and live vibes.",
                                    "vibeDna" to listOf("Nightlife", "Amapiano", "Festivals", "Rooftops")
                                )
                                db.collection("users").document(user.uid).set(userDoc)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        currentStep = WelcomeStep.LOCATION_PERMISSION
                    } ?: run {
                        isLoading = false
                        userName = cleanName
                        userEmailOrPhone = cleanEmail
                        currentStep = WelcomeStep.LOCATION_PERMISSION
                    }
                } else {
                    isLoading = false
                    val exception = task.exception
                    val errorMsg = exception?.localizedMessage ?: "Registration failed. Please try again."
                    authError = errorMsg
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun performPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            Toast.makeText(context, "Please enter your email address.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Password reset email sent to $cleanEmail. Check your inbox.", Toast.LENGTH_LONG).show()
                } else {
                    val msg = task.exception?.localizedMessage ?: "Failed to send password reset email."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun performAnonymousGuestSignIn() {
        isLoading = true
        authError = null
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    userName = "Guest Explorer"
                    userEmailOrPhone = user?.uid ?: "guest"
                    Toast.makeText(context, "Signed in as Guest", Toast.LENGTH_SHORT).show()
                    currentStep = WelcomeStep.LOCATION_PERMISSION
                } else {
                    val msg = task.exception?.localizedMessage ?: "Guest login failed."
                    authError = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020202))
    ) {
        // Shared Luxurious Cinematic Background (Always running softly underneath)
        if (currentStep != WelcomeStep.LAUNCH && currentStep != WelcomeStep.CINEMATIC_WELCOME) {
            CinematicAmbientBackground()
        }

        // Main Screen Transition Manager
        Crossfade(
            targetState = currentStep,
            animationSpec = tween(600, easing = LinearOutSlowInEasing),
            label = "WelcomeFlowTransition"
        ) { step ->
            when (step) {
                WelcomeStep.LAUNCH -> {
                    LaunchStageView()
                }
                WelcomeStep.WELCOME -> {
                    WelcomeStageView(
                        onGetStarted = {
                            currentStep = WelcomeStep.ONBOARDING
                        },
                        onSignIn = {
                            isNewUser = false
                            currentStep = WelcomeStep.SIGN_IN
                        }
                    )
                }
                WelcomeStep.ONBOARDING -> {
                    OnboardingStageView(
                        onFinished = {
                            currentStep = WelcomeStep.AUTH_HUB
                        }
                    )
                }
                WelcomeStep.AUTH_HUB -> {
                    AuthHubStageView(
                        onGoogleSelected = {
                            performGoogleSignIn()
                        },
                        onEmailSelected = {
                            isNewUser = true
                            currentStep = WelcomeStep.SIGN_UP_STEP1
                        },
                        onBack = {
                            currentStep = WelcomeStep.WELCOME
                        }
                    )
                }
                WelcomeStep.SIGN_IN -> {
                    SignInStageView(
                        onBack = { currentStep = WelcomeStep.WELCOME },
                        onSignUpClicked = {
                            isNewUser = true
                            currentStep = WelcomeStep.SIGN_UP_STEP1
                        },
                        onSkipClicked = {
                            performAnonymousGuestSignIn()
                        },
                        onForgotPassword = { email ->
                            performPasswordReset(email)
                        },
                        onProviderSelected = { provider ->
                            performGoogleSignIn()
                        },
                        onSuccess = { email, password ->
                            performSignIn(email, password)
                        },
                        isLoading = isLoading,
                        errorMessage = authError
                    )
                }
                WelcomeStep.SIGN_UP_STEP1 -> {
                    SignUpStep1View(
                        onBack = { currentStep = WelcomeStep.AUTH_HUB },
                        onSkipClicked = { performAnonymousGuestSignIn() },
                        onSignInClicked = { currentStep = WelcomeStep.SIGN_IN },
                        onProviderSelected = { provider ->
                            performGoogleSignIn()
                        },
                        onNext = { name, email ->
                            userName = name
                            userEmailOrPhone = email
                            currentStep = WelcomeStep.SIGN_UP_STEP2
                        }
                    )
                }
                WelcomeStep.SIGN_UP_STEP2 -> {
                    SignUpStep2View(
                        name = userName,
                        email = userEmailOrPhone,
                        onBack = { currentStep = WelcomeStep.SIGN_UP_STEP1 },
                        onSignInClicked = { currentStep = WelcomeStep.SIGN_IN },
                        onSuccess = { password ->
                            userPassword = password
                            performSignUp(userName, userEmailOrPhone, password)
                        },
                        isLoading = isLoading,
                        errorMessage = authError
                    )
                }
                WelcomeStep.VERIFICATION -> {
                    VerificationStageView(
                        target = userEmailOrPhone,
                        onBack = { currentStep = if (isNewUser) WelcomeStep.SIGN_UP_STEP2 else WelcomeStep.SIGN_IN },
                        onVerified = {
                            currentStep = WelcomeStep.LOCATION_PERMISSION
                        }
                    )
                }
                WelcomeStep.LOCATION_PERMISSION -> {
                    LocationPermissionStageView(
                        onComplete = {
                            currentStep = WelcomeStep.NOTIFICATION_PERMISSION
                        }
                    )
                }
                WelcomeStep.NOTIFICATION_PERMISSION -> {
                    NotificationPermissionStageView(
                        onComplete = {
                            currentStep = WelcomeStep.CINEMATIC_WELCOME
                        }
                    )
                }
                WelcomeStep.CINEMATIC_WELCOME -> {
                    CinematicWelcomeStageView(
                        userName = if (userName.isEmpty()) "Jordan" else userName,
                        onFinished = onAuthComplete
                    )
                }
            }
        }
    }
}

// ==========================================
// BACKGROUND: SHIMMER & CLUB LIGHTS
// ==========================================

@Composable
fun CinematicAmbientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientLights")
    
    // Smooth infinite breathing/moving circles for deep visual layer
    val xOffset1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "light1X"
    )
    val yOffset1 by infiniteTransition.animateFloat(
        initialValue = 200f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "light1Y"
    )
    
    val xOffset2 by infiniteTransition.animateFloat(
        initialValue = 800f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "light2X"
    )
    val yOffset2 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "light2Y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030205))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(90.dp)
                .alpha(0.6f)
        ) {
            // Neon Violet Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB026FF).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(xOffset1, yOffset1),
                    radius = 500f
                ),
                radius = 500f,
                center = Offset(xOffset1, yOffset1)
            )
            // Hot Pink Club Pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(xOffset2, yOffset2),
                    radius = 450f
                ),
                radius = 450f,
                center = Offset(xOffset2, yOffset2)
            )
            // Slow golden shimmer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF9500).copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width * 0.5f + xOffset1 * 0.3f, size.height * 0.7f),
                    radius = 400f
                ),
                radius = 400f,
                center = Offset(size.width * 0.5f + xOffset1 * 0.3f, size.height * 0.7f)
            )
        }
    }
}

// ==========================================
// STAGE 1: BRAND LAUNCH STAGE
// ==========================================

@Composable
fun LaunchStageView() {
    val scaleAnim = remember { Animatable(0.92f) }
    val alphaAnim = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000, easing = SafeEaseOutBack)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(800)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scaleAnim.value).alpha(alphaAnim.value)
        ) {
            // High-end Findlyts Logo Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                        )
                    )
            ) {
                // Outer circle cutout for visual style
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Logo icon",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "FINDLYTS",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = 6.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Find your level.\nLive the moment.",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ==========================================
// BRAND LOGO HEADER COMPONENT
// ==========================================

@Composable
fun FindlytsLogoHeader(
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Stylized F Emblem with Sparkle Star
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size.dp)
                    .clip(RoundedCornerShape((size * 0.32).dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Findlyts Sparkle",
                    tint = Color.White,
                    modifier = Modifier.size((size * 0.55).dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "findlyts",
                fontWeight = FontWeight.Black,
                fontSize = (size * 0.65).sp,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FIND YOUR LEVEL, ",
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.22).sp,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "LIVE THE MOMENT",
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.22).sp,
                letterSpacing = 2.sp,
                color = Color(0xFFFF2D55)
            )
        }
    }
}

// ==========================================
// SOCIAL AUTH ROW COMPONENT
// ==========================================

@Composable
fun SocialAuthRow(
    onProviderSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.12f)
            )
            Text(
                text = "or continue with",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.12f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google
            SocialIconButton(
                bgColor = Color(0xFF1E1B29),
                content = {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFEA4335))
                    }
                },
                label = "Google",
                onClick = { onProviderSelected("Google") }
            )

            // Apple
            SocialIconButton(
                bgColor = Color(0xFF1E1B29),
                content = {
                    Text("", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                },
                label = "Apple",
                onClick = { onProviderSelected("Apple") }
            )

            // Spotify
            SocialIconButton(
                bgColor = Color(0xFF1E1B29),
                content = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Spotify",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                label = "Spotify",
                onClick = { onProviderSelected("Spotify") }
            )

            // X / Twitter
            SocialIconButton(
                bgColor = Color(0xFF1E1B29),
                content = {
                    Text("X", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                },
                label = "X",
                onClick = { onProviderSelected("X") }
            )

            // Facebook
            SocialIconButton(
                bgColor = Color(0xFF1E1B29),
                content = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1877F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("f", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    }
                },
                label = "Facebook",
                onClick = { onProviderSelected("Facebook") }
            )
        }
    }
}

@Composable
fun SocialIconButton(
    bgColor: Color,
    content: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = bgColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

// ==========================================
// STAGE 2: WELCOME STAGE
// ==========================================

@Composable
fun WelcomeStageView(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Brand Header
        FindlytsLogoHeader(
            modifier = Modifier.padding(top = 28.dp),
            size = 46
        )

        // Center Content - Crowd Atmosphere & Feature Cards
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 3 Feature Badges Row (Discover, Connect, Experience)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureBadgeItem(
                    icon = Icons.Default.ConfirmationNumber,
                    title = "DISCOVER",
                    subtitle = "The hottest events\nnear you",
                    accentColor = Color(0xFFB026FF)
                )
                FeatureBadgeItem(
                    icon = Icons.Default.Groups,
                    title = "CONNECT",
                    subtitle = "See who's going\nand join",
                    accentColor = Color(0xFFFF2D55)
                )
                FeatureBadgeItem(
                    icon = Icons.Default.Bolt,
                    title = "EXPERIENCE",
                    subtitle = "Live club lobbies\nand real vibes",
                    accentColor = Color(0xFFFF9500)
                )
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Get Started",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = "Sign in",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2D55),
                    modifier = Modifier.clickable { onSignIn() }
                )
            }
        }
    }
}

@Composable
fun FeatureBadgeItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF140F22),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

// ==========================================
// STAGE 3: 3-PAGE PREMIUM ONBOARDING
// ==========================================

data class OnboardingPage(
    val title: String,
    val description: String,
    val type: String // "CITY", "LOBBY", "VIBE"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingStageView(
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Find your city.",
            description = "Discover nightlife, clubs, restaurants and experiences happening around you tonight.",
            type = "CITY"
        ),
        OnboardingPage(
            title = "Join the moment.",
            description = "See who's out, join live club lobbies and experience nightlife together.",
            type = "LOBBY"
        ),
        OnboardingPage(
            title = "Personalized for you.",
            description = "Get event recommendations, Flash Drops and venues that match your vibe.",
            type = "VIBE"
        )
    )

    val pagerState = androidx.compose.foundation.pager.rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinished) {
                Text("Skip", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }

        // Pager and Custom Graphics
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // High fidelity vector representations using Jetpack Compose drawing
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF0C0A12)),
                    contentAlignment = Alignment.Center
                ) {
                    when (page.type) {
                        "CITY" -> OnboardingCityGraphic()
                        "LOBBY" -> OnboardingLobbyGraphic()
                        "VIBE" -> OnboardingVibeGraphic()
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = page.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = page.description,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // Bottom Controls: Indicators and Next/Get Started Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "indicator"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFB026FF) else Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // CTA Button
            val isLastPage = pagerState.currentPage == pages.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastPage) Color.Transparent else Color(0xFF14121E)
                ),
                contentPadding = PaddingValues()
            ) {
                if (isLastPage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Let's Go", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingCityGraphic() {
    Canvas(modifier = Modifier.size(200.dp)) {
        val path = Path().apply {
            moveTo(20f, 180f)
            lineTo(20f, 100f)
            lineTo(50f, 100f)
            lineTo(50f, 130f)
            lineTo(80f, 130f)
            lineTo(80f, 60f)
            lineTo(120f, 60f)
            lineTo(120f, 110f)
            lineTo(150f, 110f)
            lineTo(150f, 80f)
            lineTo(180f, 80f)
            lineTo(180f, 180f)
            close()
        }
        // Draw elegant city outline with gradient fill and glows
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFB026FF).copy(alpha = 0.5f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = Color(0xFFB026FF),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        // Golden sparkling dots for nightlife
        drawCircle(Color(0xFFFF9500), radius = 6f, center = Offset(35f, 115f))
        drawCircle(Color(0xFFFF2D55), radius = 6f, center = Offset(100f, 80f))
        drawCircle(Color(0xFFFF9500), radius = 6f, center = Offset(165f, 95f))
        drawCircle(Color(0xFFB026FF), radius = 10f, center = Offset(100f, 40f), alpha = 0.4f)
    }
}

@Composable
fun OnboardingLobbyGraphic() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wave"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.size(200.dp)) {
            // Pulse waves of music lobby
            drawCircle(
                color = Color(0xFFFF2D55).copy(alpha = 0.15f),
                radius = 70f * waveScale,
                center = center
            )
            drawCircle(
                color = Color(0xFFB026FF).copy(alpha = 0.25f),
                radius = 45f * waveScale,
                center = center
            )
            // Center element
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFB026FF)),
                    center = center
                ),
                radius = 32f,
                center = center
            )
        }
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun OnboardingVibeGraphic() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VibeChip("Amapiano", Color(0xFFFF9500))
                VibeChip("Rooftops", Color(0xFF34C759))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VibeChip("Techno", Color(0xFFB026FF))
                VibeChip("Speakeasy", Color(0xFFFF2D55))
            }
        }
    }
}

@Composable
fun VibeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// STAGE 4: AUTHENTICATION CHOICE HUB
// ==========================================

@Composable
fun AuthHubStageView(
    onEmailSelected: () -> Unit,
    onGoogleSelected: () -> Unit = onEmailSelected,
    onBack: () -> Unit
) {
    var showMoreOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Back Button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        // Mid Heading
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Welcome to Findlyts",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose how you'd like to continue.",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }

        // Primary & Secondary auth providers
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthChoiceButton(
                text = "Continue with Google",
                icon = Icons.Default.Star,
                onClick = onGoogleSelected
            )
            AuthChoiceButton(
                text = "Continue with Apple",
                icon = Icons.Default.Lock,
                onClick = onEmailSelected
            )
            AuthChoiceButton(
                text = "Continue with Phone",
                icon = Icons.Default.Phone,
                onClick = onEmailSelected
            )
            AuthChoiceButton(
                text = "Continue with Email",
                icon = Icons.Default.Email,
                onClick = onEmailSelected
            )

            AnimatedVisibility(
                visible = showMoreOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AuthChoiceButton(
                        text = "Continue with Facebook",
                        icon = Icons.Default.Public,
                        onClick = onEmailSelected
                    )
                    AuthChoiceButton(
                        text = "Continue with Spotify",
                        icon = Icons.Default.MusicNote,
                        onClick = onEmailSelected
                    )
                }
            }

            if (!showMoreOptions) {
                TextButton(
                    onClick = { showMoreOptions = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("More sign-in options", color = Color(0xFFB026FF), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Safe disclaimer
        Text(
            text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
fun AuthChoiceButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF110E18),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
        }
    }
}

// ==========================================
// STAGE 5: SIGN IN SCREEN
// ==========================================

@Composable
fun SignInStageView(
    onBack: () -> Unit,
    onSuccess: (String, String) -> Unit,
    onSignUpClicked: () -> Unit,
    onSkipClicked: () -> Unit,
    onForgotPassword: (String) -> Unit = {},
    onProviderSelected: (String) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isLoading) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            FindlytsLogoHeader(size = 32)

            TextButton(onClick = onSkipClicked, enabled = !isLoading) {
                Text("Skip", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Welcome back",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sign in to continue your nightlife journey.",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF2D55),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email/Phone Input
            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Email or phone number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                },
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF130F1E),
                    unfocusedContainerColor = Color(0xFF130F1E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                },
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF130F1E),
                    unfocusedContainerColor = Color(0xFF130F1E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onForgotPassword(emailOrPhone) }, enabled = !isLoading) {
                    Text(
                        text = "Forgot password?",
                        color = Color(0xFFFF2D55),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CTA Button with Gradient
        Button(
            onClick = {
                if (emailOrPhone.isNotEmpty() && password.isNotEmpty()) {
                    onSuccess(emailOrPhone, password)
                }
            },
            enabled = emailOrPhone.isNotEmpty() && password.isNotEmpty() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color(0xFF1B1824)
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (emailOrPhone.isNotEmpty() && password.isNotEmpty() && !isLoading) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                            )
                        } else {
                            SolidColor(Color(0xFF1E1B29))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Sign In", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Social Auth Buttons
        SocialAuthRow(onProviderSelected = onProviderSelected)

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Footer Link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "Don't have an account? ",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = "Sign up",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF2D55),
                modifier = Modifier.clickable { onSignUpClicked() }
            )
        }
    }
}

// ==========================================
// STAGE 6: TWO-STEP SIGN UP
// ==========================================

@Composable
fun SignUpStep1View(
    onBack: () -> Unit,
    onSkipClicked: () -> Unit,
    onSignInClicked: () -> Unit,
    onNext: (name: String, emailOrPhone: String) -> Unit,
    onProviderSelected: (String) -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            FindlytsLogoHeader(size = 32)

            TextButton(onClick = onSkipClicked) {
                Text("Skip", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Create your account",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Join findlyts and live the moment.",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF130F1E),
                    unfocusedContainerColor = Color(0xFF130F1E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Email or phone number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF130F1E),
                    unfocusedContainerColor = Color(0xFF130F1E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue Button
        Button(
            onClick = { onNext(name, emailOrPhone) },
            enabled = name.isNotEmpty() && emailOrPhone.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color(0xFF1E1B29)
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (name.isNotEmpty() && emailOrPhone.isNotEmpty()) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                            )
                        } else {
                            SolidColor(Color(0xFF1E1B29))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Social Auth
        SocialAuthRow(onProviderSelected = onProviderSelected)

        Spacer(modifier = Modifier.height(24.dp))

        // Footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = "Sign in",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF2D55),
                modifier = Modifier.clickable { onSignInClicked() }
            )
        }
    }
}

@Composable
fun SignUpStep2View(
    name: String,
    email: String,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
    onSignInClicked: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(true) }

    // Live validation checks
    val isMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }
    val matchesConfirm = password.isNotEmpty() && password == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isLoading) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            FindlytsLogoHeader(size = 32)

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Set your password",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Secure your Findlyts account.",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF2D55),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input Fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                },
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF130F1E),
                    unfocusedContainerColor = Color(0xFF130F1E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                },
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF130F1E),
                    unfocusedContainerColor = Color(0xFF130F1E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password strength checks
            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ValidationCheckItem(text = "At least 8 characters", isMet = isMinLength)
                ValidationCheckItem(text = "At least one uppercase letter", isMet = hasUppercase)
                ValidationCheckItem(text = "At least one number", isMet = hasNumber)
                ValidationCheckItem(text = "Passwords match", isMet = matchesConfirm)
            }

            // Terms Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { acceptedTerms = !acceptedTerms }
                    .padding(vertical = 6.dp)
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFB026FF),
                        uncheckedColor = Color.White.copy(alpha = 0.4f)
                    )
                )
                Text(
                    text = "I agree to the Terms of Service and Privacy Policy.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CTA Button
        val canSubmit = isMinLength && matchesConfirm && acceptedTerms && !isLoading
        Button(
            onClick = {
                if (canSubmit) {
                    onSuccess(password)
                }
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color(0xFF1E1B29)
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (canSubmit) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB026FF), Color(0xFFFF2D55), Color(0xFFFF9500))
                            )
                        } else {
                            SolidColor(Color(0xFF1E1B29))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Sign Up", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = "Sign in",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF2D55),
                modifier = Modifier.clickable { onSignInClicked() }
            )
        }
    }
}

@Composable
fun ValidationCheckItem(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isMet) Color(0xFF34C759) else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isMet) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

// ==========================================
// STAGE 7: OTP VERIFICATION
// ==========================================

@Composable
fun VerificationStageView(
    target: String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    var otpCode by remember { mutableStateOf("") }
    val maxOtpLength = 6
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Verification",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter the 6-digit code sent to\n${if (target.isEmpty()) "your email" else target}",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Code Slots View
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(maxOtpLength) { index ->
                    val digit = otpCode.getOrNull(index)
                    val isCurrent = index == otpCode.length
                    
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) Color(0xFF1E1428) else Color(0xFF110E18))
                            .border(
                                width = 1.5.dp,
                                color = if (isCurrent) Color(0xFFB026FF) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit?.toString() ?: "",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resend Helper
            TextButton(onClick = { 
                Toast.makeText(context, "Verification code resent to ${if (target.isEmpty()) "your email" else target}", Toast.LENGTH_SHORT).show() 
            }) {
                Text("Resend code", color = Color(0xFFB026FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Premium Numpad Block
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "CLEAR")
            )
            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowKeys.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (key == "CLEAR") {
                                        if (otpCode.isNotEmpty()) {
                                            otpCode = otpCode.dropLast(1)
                                        }
                                    } else if (key.isNotEmpty()) {
                                        if (otpCode.length < maxOtpLength) {
                                            otpCode += key
                                        }
                                        if (otpCode.length == maxOtpLength) {
                                            onVerified()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "CLEAR") {
                                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = Color.White)
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STAGE 8: LOCATION PERMISSION
// ==========================================

@Composable
fun LocationPermissionStageView(
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Center visual & content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14121E)),
                contentAlignment = Alignment.Center
            ) {
                // Moving pulse overlay around GPS pin
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFFB026FF),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Find what's happening near you.",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Location helps Findlyts recommend nearby venues, live events, music lobbies, and exclusive local experiences tonight.",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Action Buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB026FF))
            ) {
                Text("Allow Location", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
            TextButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Not Now", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// STAGE 9: NOTIFICATION PERMISSION
// ==========================================

@Composable
fun NotificationPermissionStageView(
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Center elements
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1428)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Never miss the moment.",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Receive notifications on local Flash Drops, event reminders, live map activities, and status updates from friends out tonight.",
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Action Buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
            ) {
                Text("Allow Notifications", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
            TextButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Later", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// STAGE 10: LOGGED-IN CINEMATIC WELCOME (Current Mayor + City)
// ==========================================

@Composable
fun CinematicWelcomeStageView(
    userName: String,
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, animationSpec = tween(1200, easing = SafeEaseOutBack))
        }
        launch {
            alpha.animateTo(1f, animationSpec = tween(900))
        }
        // Auto transition after showing cinematic welcome
        delay(4000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        // Shimmering stars/lights overlay in background
        CinematicAmbientBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFB026FF), CircleShape)
            ) {
                AsyncImage(
                    model = "https://i.pravatar.cc/150?img=12", // Current user
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome back, $userName!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ENTERING THE BEAT",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 2.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Cinematic Mayor City Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                color = Color(0xFF140D22).copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0xFFB026FF).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CURRENT CITY: JOHANNESBURG",
                        color = Color(0xFFFF9500),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Mayor image
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFFF2D55), CircleShape)
                        ) {
                            AsyncImage(
                                model = "https://i.pravatar.cc/150?img=11", // Kabza De Small
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Kabza De Small",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "👑 Mayor of the Night",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Amapiano Fridays is currently live at FOMO Club Rosebank, boasting a massive 95% party rating. The live map has loaded.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = Color(0xFFB026FF), modifier = Modifier.size(32.dp))
        }
    }
}
