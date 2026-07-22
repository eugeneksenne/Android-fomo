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
                    .setApiKey("AIzaSyDummyKeyForBuildAndLocalTest123")
                    .setApplicationId("1:1234567890:android:abcdef01234567")
                    .setProjectId("fomo-dummy-project")
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
        // We attempt to get the web client ID if available. For AI Studio sandbox we provide a default that will likely fail with a developer error, triggering our fallback logic.
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "dummy_client_id_for_simulation_1234.apps.googleusercontent.com"
        } catch (e: Exception) {
            "dummy_client_id_for_simulation_1234.apps.googleusercontent.com"
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
                    
                    if (auth != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                userName = user?.displayName ?: "Google User"
                                userEmailOrPhone = user?.email ?: ""
                                Toast.makeText(context, "Google Sign-In successful!", Toast.LENGTH_SHORT).show()
                                currentStep = WelcomeStep.LOCATION_PERMISSION
                            } else {
                                val exception = task.exception
                                authError = exception?.localizedMessage ?: "Google Authentication failed."
                                Toast.makeText(context, "Error: $authError", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        isLoading = false
                        userName = googleIdTokenCredential.displayName ?: "Google User"
                        userEmailOrPhone = googleIdTokenCredential.id ?: ""
                        Toast.makeText(context, "Signed in with Google (Simulated)", Toast.LENGTH_SHORT).show()
                        currentStep = WelcomeStep.LOCATION_PERMISSION
                    }
                } else {
                    isLoading = false
                    Toast.makeText(context, "Unexpected credential type.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { // Catch GetCredentialException and others
                e.printStackTrace()
                // If it fails because of dummy client ID or environment, fallback to simulated flow so user isn't blocked
                delay(1000)
                isLoading = false
                userName = "Demo Google User"
                userEmailOrPhone = "demo@gmail.com"
                Toast.makeText(context, "Simulated Google Sign-In (Missing Real Config)", Toast.LENGTH_SHORT).show()
                currentStep = WelcomeStep.LOCATION_PERMISSION
            }
        }
    }

    fun performSignIn(email: String, pword: String) {
        if (email.isBlank() || pword.isBlank()) {
            Toast.makeText(context, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        authError = null
        
        if (auth != null) {
            auth.signInWithEmailAndPassword(email, pword)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        userName = user?.displayName ?: email.substringBefore("@")
                        userEmailOrPhone = email
                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        currentStep = WelcomeStep.LOCATION_PERMISSION
                    } else {
                        val exception = task.exception
                        val errorMsg = exception?.localizedMessage ?: "Authentication failed."
                        authError = errorMsg
                        Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // Fallback simulated sign in
            scope.launch {
                delay(1000)
                isLoading = false
                userName = email.substringBefore("@")
                userEmailOrPhone = email
                Toast.makeText(context, "Signed in (Simulated Mode)", Toast.LENGTH_SHORT).show()
                currentStep = WelcomeStep.LOCATION_PERMISSION
            }
        }
    }

    fun performSignUp(name: String, email: String, pword: String) {
        if (email.isBlank() || pword.isBlank() || name.isBlank()) {
            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        authError = null
        
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, pword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = name
                        }
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            isLoading = false
                            userName = name
                            userEmailOrPhone = email
                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            currentStep = WelcomeStep.LOCATION_PERMISSION
                        } ?: run {
                            isLoading = false
                            userName = name
                            userEmailOrPhone = email
                            currentStep = WelcomeStep.LOCATION_PERMISSION
                        }
                    } else {
                        isLoading = false
                        val exception = task.exception
                        val errorMsg = exception?.localizedMessage ?: "Registration failed."
                        authError = errorMsg
                        Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // Fallback simulated sign up
            scope.launch {
                delay(1000)
                isLoading = false
                userName = name
                userEmailOrPhone = email
                Toast.makeText(context, "Account created (Simulated Mode)", Toast.LENGTH_SHORT).show()
                currentStep = WelcomeStep.LOCATION_PERMISSION
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
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = "Findlyts Logo",
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FINDLYTS",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 4.sp,
                color = Color.White
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Find your level.",
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Discover nightlife happening around you in real time.",
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 24.sp
            )
        }

        // Action Buttons with Primary Gradient
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
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
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = "Already have an account? Sign In",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
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
            .padding(24.dp),
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
            Text("Sign In", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
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

            // Input Fields
            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Email or Phone") },
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedLabelColor = Color(0xFFB026FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { /* simulated trigger */ }, enabled = !isLoading) {
                    Text("Forgot Password?", color = Color(0xFFB026FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // CTA
        Button(
            onClick = {
                if (emailOrPhone.isEmpty() || password.isEmpty()) {
                    // Quick toast simulation
                } else {
                    onSuccess(emailOrPhone, password)
                }
            },
            enabled = emailOrPhone.isNotEmpty() && password.isNotEmpty() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB026FF),
                disabledContainerColor = Color(0xFF1B1824)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Sign In", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// STAGE 6: TWO-STEP SIGN UP
// ==========================================

@Composable
fun SignUpStep1View(
    onBack: () -> Unit,
    onNext: (name: String, emailOrPhone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Step 1 of 2", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Create Account",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Email or Phone Number") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Button(
            onClick = { onNext(name, emailOrPhone) },
            enabled = name.isNotEmpty() && emailOrPhone.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB026FF),
                disabledContainerColor = Color(0xFF1B1824)
            )
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun SignUpStep2View(
    name: String,
    email: String,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isLoading) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Step 2 of 2", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Secure Account",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.White
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF2D55),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Choose Password") },
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password strength feedback
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PasswordCriteriaRow("✓ At least 8 characters", isMinLength)
                PasswordCriteriaRow("✓ At least one uppercase letter", hasUppercase)
                PasswordCriteriaRow("✓ At least one number", hasNumber)
            }

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB026FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (confirmPassword.isNotEmpty()) {
                PasswordCriteriaRow("✓ Passwords match", matchesConfirm)
            }
        }

        // Action button
        Button(
            onClick = { onSuccess(password) },
            enabled = isMinLength && hasUppercase && hasNumber && matchesConfirm && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB026FF),
                disabledContainerColor = Color(0xFF1B1824)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Create Account", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PasswordCriteriaRow(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isMet) Color(0xFF34C759) else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
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
            TextButton(onClick = { /* Simulated Resend */ }) {
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
