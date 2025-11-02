package com.example.mindspacemobileapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock


// Data Models
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val role: String = "client", // client, psychologist, admin
    val status: String = "active", // active, pending, rejected
    val specializations: List<String> = emptyList(),
    val experience: String = "",
    val licenseNumber: String = "",
    val certificateUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val authProvider: String = "email" // email, google
)

data class MoodEntry(
    val id: String = "",
    val userId: String = "",
    val mood: String = "",
    val emoji: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Session(
    val id: String = "",
    val psychologistId: String = "",
    val psychologistName: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val date: Long = 0L,
    val duration: Int = 60, // minutes
    val status: String = "scheduled", // scheduled, completed, cancelled
    val notes: String = "",
    val meetingLink: String = ""
)

data class Resource(
    val id: String = "",
    val psychologistId: String = "",
    val psychologistName: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "", // article, video, exercise
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Seminar(
    val id: String = "",
    val psychologistId: String = "",
    val psychologistName: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val duration: Int = 90,
    val maxParticipants: Int = 50,
    val registeredUsers: List<String> = emptyList(),
    val meetingLink: String = ""
)
@Composable
fun PendingApprovalScreen(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFFFA726)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Application Under Review",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Thank you for applying to join MindSpace as a psychologist. Our admin team is reviewing your credentials and will notify you once approved.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = {
                auth.signOut()
                googleSignInClient.signOut()
                onSignOut()
            }
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
fun RejectedScreen(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Application Not Approved",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Unfortunately, we were unable to approve your application at this time. Please contact support for more information.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = {
                auth.signOut()
                googleSignInClient.signOut()
                onSignOut()
            }
        ) {
            Text("Sign Out")
        }
    }
}


@Composable
fun MainAppScreen(
    user: com.google.firebase.auth.FirebaseUser,
    userProfile: UserProfile,
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                when (userProfile.role) {
                    "client" -> {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home") },
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Face, "Mood") },
                            label = { Text("Mood") },
                            selected = currentScreen == "mood",
                            onClick = { currentScreen = "mood" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Edit, "Journal") },
                            label = { Text("Journal") },
                            selected = currentScreen == "journal",
                            onClick = { currentScreen = "journal" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.CalendarToday, "Sessions") },
                            label = { Text("Sessions") },
                            selected = currentScreen == "sessions",
                            onClick = { currentScreen = "sessions" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, "Profile") },
                            label = { Text("Profile") },
                            selected = currentScreen == "profile",
                            onClick = { currentScreen = "profile" }
                        )
                    }
                    "psychologist" -> {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home") },
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.CalendarToday, "Sessions") },
                            label = { Text("Sessions") },
                            selected = currentScreen == "sessions",
                            onClick = { currentScreen = "sessions" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DateRange, "Seminars") },
                            label = { Text("Seminars") },
                            selected = currentScreen == "seminars",
                            onClick = { currentScreen = "seminars" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.ShoppingCart, "Resources") },
                            label = { Text("Resources") },
                            selected = currentScreen == "resources",
                            onClick = { currentScreen = "resources" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, "Profile") },
                            label = { Text("Profile") },
                            selected = currentScreen == "profile",
                            onClick = { currentScreen = "profile" }
                        )
                    }
                    "admin" -> {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, "Dashboard") },
                            label = { Text("Dashboard") },
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.CheckCircle, "Approvals") },
                            label = { Text("Approvals") },
                            selected = currentScreen == "approvals",
                            onClick = { currentScreen = "approvals" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, "Profile") },
                            label = { Text("Profile") },
                            selected = currentScreen == "profile",
                            onClick = { currentScreen = "profile" }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (userProfile.role) {
                "client" -> {
                    when (currentScreen) {
                        "home" -> ClientHomeScreen(userProfile)
                        "mood" -> MoodTrackerScreen(userProfile.uid)
                        "journal" -> JournalScreen(userProfile.uid)
                        "sessions" -> ClientSessionsScreen(userProfile)
                        "profile" -> ProfileScreen(user, userProfile, auth, googleSignInClient, onSignOut)
                    }
                }
                "psychologist" -> {
                    when (currentScreen) {
                        "home" -> PsychologistHomeScreen(userProfile)
                        "sessions" -> PsychologistSessionsScreen(userProfile)
                        "seminars" -> SeminarsScreen(userProfile)
                        "resources" -> ResourcesScreen(userProfile)
                        "profile" -> ProfileScreen(user, userProfile, auth, googleSignInClient, onSignOut)
                    }
                }
                "admin" -> {
                    when (currentScreen) {
                        "home" -> AdminDashboardScreen()
                        "approvals" -> AdminApprovalsScreen()
                        "profile" -> ProfileScreen(user, userProfile, auth, googleSignInClient, onSignOut)
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6750A4),
                    secondary = Color(0xFF625B71),
                    tertiary = Color(0xFF7D5260),
                    background = Color(0xFFFFFBFE),
                    surface = Color(0xFFFFFBFE),
                    primaryContainer = Color(0xFFEADDFF),
                    secondaryContainer = Color(0xFFE8DEF8)
                )
            ) {
                MindSpaceApp(auth, googleSignInClient)
            }
        }
    }
}

@Composable
fun MindSpaceApp(auth: FirebaseAuth, googleSignInClient: GoogleSignInClient) {
    var user by remember { mutableStateOf(auth.currentUser) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var needsProfileCompletion by remember { mutableStateOf(false) }
    var isGoogleSignIn by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()

    // Listen to auth state changes
    DisposableEffect(Unit) {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(authListener)
        onDispose {
            auth.removeAuthStateListener(authListener)
        }
    }

    // Listen to user profile changes
    LaunchedEffect(user?.uid) {
        isLoading = true
        if (user != null) {
            val docRef = db.collection("users").document(user!!.uid)

            // First try to get the document
            docRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    userProfile = profile

                    // Check if it's a Google sign-in without profile
                    isGoogleSignIn = profile?.authProvider == "google"

                    // Check if profile needs completion (psychologist without credentials)
                    needsProfileCompletion = profile?.role == "psychologist" &&
                            profile.specializations.isEmpty() &&
                            profile.licenseNumber.isEmpty()
                } else {
                    // Profile doesn't exist - must be Google sign-in
                    isGoogleSignIn = true
                    needsProfileCompletion = true
                    userProfile = null
                }
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                userProfile = null
                isGoogleSignIn = true
                needsProfileCompletion = true
            }

            // Then listen for real-time updates
            docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    userProfile = profile

                    isGoogleSignIn = profile?.authProvider == "google"

                    needsProfileCompletion = profile?.role == "psychologist" &&
                            profile.specializations.isEmpty() &&
                            profile.licenseNumber.isEmpty()
                }
            }
        } else {
            isLoading = false
            userProfile = null
            needsProfileCompletion = false
            isGoogleSignIn = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener {
                if (it.isSuccessful) {
                    user = auth.currentUser
                    isGoogleSignIn = true
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            user == null -> {
                AuthenticationScreen(
                    auth = auth,
                    googleSignInClient = googleSignInClient,
                    launcher = launcher,
                    onSignInSuccess = {
                        // Refresh will happen automatically via auth state listener
                    }
                )
            }
            needsProfileCompletion -> {
                // If it's a psychologist who registered via email/password, show completion screen
                if (userProfile?.role == "psychologist" && !isGoogleSignIn) {
                    PsychologistProfileCompletionScreen(
                        user = user!!,
                        existingProfile = userProfile!!,
                        db = db,
                        onProfileCompleted = {
                            needsProfileCompletion = false
                        }
                    )
                }
                // If it's Google sign-in (no profile exists yet), show role selection
                else if (isGoogleSignIn) {
                    RoleSelectionScreen(user!!, db) { profile ->
                        userProfile = profile
                        needsProfileCompletion = false
                        isGoogleSignIn = false
                    }
                }
                // Fallback
                else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            userProfile?.role == "psychologist" && userProfile?.status == "pending" -> {
                PendingApprovalScreen(auth, googleSignInClient) {
                    auth.signOut()
                    user = null
                    userProfile = null
                }
            }
            userProfile?.role == "psychologist" && userProfile?.status == "rejected" -> {
                RejectedScreen(auth, googleSignInClient) {
                    auth.signOut()
                    user = null
                    userProfile = null
                }
            }
            userProfile != null -> {
                MainAppScreen(user!!, userProfile!!, auth, googleSignInClient) {
                    auth.signOut()
                    user = null
                    userProfile = null
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading your profile...")
                    }
                }
            }
        }
    }
}


@Composable
fun AuthenticationScreen(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onSignInSuccess: () -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }

    if (isRegistering) {
        RegisterScreen(
            auth = auth,
            googleSignInClient = googleSignInClient,
            launcher = launcher,
            onBackToSignIn = { isRegistering = false },
            onRegisterSuccess = onSignInSuccess
        )
    } else {
        SignInScreen(
            auth = auth,
            googleSignInClient = googleSignInClient,
            launcher = launcher,
            onNavigateToRegister = { isRegistering = true },
            onSignInSuccess = onSignInSuccess
        )
    }
}

@Composable
fun SignInScreen(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onNavigateToRegister: () -> Unit,
    onSignInSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEADDFF),
                        Color(0xFFFFFBFE)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF6750A4)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFF6750A4)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sign in to continue your journey",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        // Email/Password Sign In
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, "Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, "Password") },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Info else Icons.Default.Lock,
                        "Toggle password visibility"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        isLoading = false
                        onSignInSuccess()
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMessage = when (e.message) {
                            "The email address is badly formatted." -> "Invalid email format"
                            "There is no user record corresponding to this identifier. The user may have been deleted." -> "No account found with this email"
                            "The password is invalid or the user does not have a password." -> "Incorrect password"
                            else -> "Sign in failed. Please try again."
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "  OR  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Person, "Sign in with Google", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text(
                "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Register",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6750A4),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

@Composable
fun RegisterScreen(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onBackToSignIn: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var registerAsPsychologist by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEADDFF),
                        Color(0xFFFFFBFE)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF6750A4)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF6750A4)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Join MindSpace today",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    errorMessage = null
                },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Info else Icons.Default.Lock,
                            "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Confirm Password") },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.Info else Icons.Default.Lock,
                            "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (registerAsPsychologist)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { registerAsPsychologist = !registerAsPsychologist }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = registerAsPsychologist,
                        onCheckedChange = { registerAsPsychologist = it }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Register as a Psychologist",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "You'll need to provide credentials for verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (errorMessage != null) {
            item {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Button(
                onClick = {
                    when {
                        displayName.isBlank() -> errorMessage = "Please enter your name"
                        email.isBlank() -> errorMessage = "Please enter your email"
                        password.isBlank() -> errorMessage = "Please enter a password"
                        password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                        password != confirmPassword -> errorMessage = "Passwords do not match"
                        else -> {
                            isLoading = true
                            errorMessage = null

                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener { authResult ->
                                    val user = authResult.user
                                    if (user != null) {
                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                            .setDisplayName(displayName)
                                            .build()

                                        user.updateProfile(profileUpdates)
                                            .addOnSuccessListener {
                                                // Create user profile in Firestore
                                                val userProfile = UserProfile(
                                                    uid = user.uid,
                                                    email = email,
                                                    displayName = displayName,
                                                    photoUrl = "",
                                                    bio = "",
                                                    role = if (registerAsPsychologist) "psychologist" else "client",
                                                    status = if (registerAsPsychologist) "pending" else "active",
                                                    authProvider = "email"
                                                )

                                                db.collection("users").document(user.uid)
                                                    .set(userProfile)
                                                    .addOnSuccessListener {
                                                        isLoading = false
                                                        // Force refresh the auth state
                                                        auth.currentUser?.reload()?.addOnCompleteListener {
                                                            onRegisterSuccess()
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isLoading = false
                                                        errorMessage = "Failed to create profile: ${e.message}"
                                                        // If profile creation fails, delete the auth user
                                                        user.delete()
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "Failed to update profile: ${e.message}"
                                                user.delete()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = when {
                                        e.message?.contains("email address is already in use") == true ->
                                            "Email already registered"
                                        e.message?.contains("email address is badly formatted") == true ->
                                            "Invalid email format"
                                        else -> "Registration failed: ${e.message}"
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Account", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "  OR  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedButton(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Person, "Sign up with Google", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign up with Google", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6750A4),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.clickable { onBackToSignIn() }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    user: com.google.firebase.auth.FirebaseUser,
    db: FirebaseFirestore,
    onProfileCreated: (UserProfile) -> Unit
) {
    var selectedRole by remember { mutableStateOf("client") }
    var bio by remember { mutableStateOf("") }
    var specializations by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var certificateUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    val certificatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        certificateUris = uris
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Complete Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tell us a bit about yourself",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text("I am a:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedRole == "client",
                    onClick = { selectedRole = "client" },
                    label = { Text("Client") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRole == "psychologist",
                    onClick = { selectedRole = "psychologist" },
                    label = { Text("Psychologist") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                placeholder = { Text("Tell us about yourself...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (selectedRole == "psychologist") {
            item {
                OutlinedTextField(
                    value = specializations,
                    onValueChange = { specializations = it },
                    label = { Text("Specializations") },
                    placeholder = { Text("e.g., Anxiety, Depression, PTSD") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("Years of Experience") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = licenseNumber,
                    onValueChange = { licenseNumber = it },
                    label = { Text("License Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedButton(
                    onClick = { certificatePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, "Upload")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Certificates (${certificateUris.size})")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Upload your professional certificates and licenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Button(
                onClick = {
                    isUploading = true
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        bio = bio,
                        role = selectedRole,
                        status = if (selectedRole == "psychologist") "pending" else "active",
                        specializations = if (selectedRole == "psychologist") specializations.split(",").map { it.trim() } else emptyList(),
                        experience = experience,
                        licenseNumber = licenseNumber,
                        authProvider = "google"
                    )

                    if (selectedRole == "psychologist" && certificateUris.isNotEmpty()) {
                        val storage = FirebaseStorage.getInstance()
                        val uploadedUrls = mutableListOf<String>()
                        var uploadCount = 0

                        certificateUris.forEach { uri ->
                            val ref = storage.reference
                                .child("certificates/${user.uid}/${System.currentTimeMillis()}.jpg")
                            ref.putFile(uri).addOnSuccessListener {
                                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                                    uploadedUrls.add(downloadUri.toString())
                                    uploadCount++
                                    if (uploadCount == certificateUris.size) {
                                        val updatedProfile = profile.copy(certificateUrls = uploadedUrls)
                                        db.collection("users").document(user.uid)
                                            .set(updatedProfile)
                                            .addOnSuccessListener {
                                                isUploading = false
                                                onProfileCreated(updatedProfile)
                                            }
                                            .addOnFailureListener {
                                                isUploading = false
                                                // Show error
                                            }
                                    }
                                }
                            }
                        }
                    } else {
                        db.collection("users").document(user.uid)
                            .set(profile)
                            .addOnSuccessListener {
                                isUploading = false
                                onProfileCreated(profile)
                            }
                            .addOnFailureListener {
                                isUploading = false
                                // Show error
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isUploading && bio.isNotEmpty() &&
                        (selectedRole == "client" ||
                                (specializations.isNotEmpty() && experience.isNotEmpty() &&
                                        licenseNumber.isNotEmpty() && certificateUris.isNotEmpty())),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Complete Profile")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsychologistProfileCompletionScreen(
    user: com.google.firebase.auth.FirebaseUser,
    existingProfile: UserProfile,
    db: FirebaseFirestore,
    onProfileCompleted: () -> Unit
) {
    var bio by remember { mutableStateOf(existingProfile.bio) }
    var specializations by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var certificateUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val certificatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        certificateUris = uris
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF6750A4)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Complete Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Provide your professional credentials",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            OutlinedTextField(
                value = bio,
                onValueChange = {
                    bio = it
                    errorMessage = null
                },
                label = { Text("Bio") },
                placeholder = { Text("Tell us about yourself...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = specializations,
                onValueChange = {
                    specializations = it
                    errorMessage = null
                },
                label = { Text("Specializations") },
                placeholder = { Text("e.g., Anxiety, Depression, PTSD") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = experience,
                onValueChange = {
                    experience = it
                    errorMessage = null
                },
                label = { Text("Years of Experience") },
                placeholder = { Text("e.g., 5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = licenseNumber,
                onValueChange = {
                    licenseNumber = it
                    errorMessage = null
                },
                label = { Text("License Number") },
                placeholder = { Text("Your professional license number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedButton(
                onClick = { certificatePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, "Upload")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Certificates (${certificateUris.size})")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Upload your professional certificates and licenses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (errorMessage != null) {
            item {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Button(
                onClick = {
                    when {
                        bio.isBlank() -> errorMessage = "Please enter your bio"
                        specializations.isBlank() -> errorMessage = "Please enter your specializations"
                        experience.isBlank() -> errorMessage = "Please enter your years of experience"
                        licenseNumber.isBlank() -> errorMessage = "Please enter your license number"
                        certificateUris.isEmpty() -> errorMessage = "Please upload at least one certificate"
                        else -> {
                            isUploading = true
                            errorMessage = null
                            val storage = FirebaseStorage.getInstance()
                            val uploadedUrls = mutableListOf<String>()
                            var uploadCount = 0
                            val totalFiles = certificateUris.size

                            certificateUris.forEachIndexed { index, uri ->
                                val timestamp = System.currentTimeMillis()
                                val fileName = "cert_${timestamp}_$index.jpg"
                                val ref = storage.reference
                                    .child("certificates/${user.uid}/$fileName")

                                ref.putFile(uri)
                                    .addOnSuccessListener { taskSnapshot ->
                                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                                            uploadedUrls.add(downloadUri.toString())
                                            uploadCount++

                                            // Check if all files uploaded
                                            if (uploadCount == totalFiles) {
                                                // All uploads complete, update profile
                                                val updates = hashMapOf<String, Any>(
                                                    "bio" to bio,
                                                    "specializations" to specializations.split(",").map { it.trim() },
                                                    "experience" to experience,
                                                    "licenseNumber" to licenseNumber,
                                                    "certificateUrls" to uploadedUrls
                                                )

                                                db.collection("users").document(user.uid)
                                                    .update(updates)
                                                    .addOnSuccessListener {
                                                        isUploading = false
                                                        onProfileCompleted()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isUploading = false
                                                        errorMessage = "Failed to save profile: ${e.localizedMessage}"
                                                    }
                                            }
                                        }.addOnFailureListener { e ->
                                            isUploading = false
                                            errorMessage = "Failed to get download URL: ${e.localizedMessage}"
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isUploading = false
                                        errorMessage = "Failed to upload certificate: ${e.localizedMessage}"
                                    }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isUploading,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isUploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...")
                    }
                } else {
                    Text("Submit for Approval")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Composable
fun PsychologistApprovalCard(
    psychologist: UserProfile,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(psychologist.photoUrl),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        psychologist.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        psychologist.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "License: ${psychologist.licenseNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        "Details"
                    )
                }
            }

            AnimatedVisibility(visible = showDetails) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Bio:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        psychologist.bio,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Specializations:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        psychologist.specializations.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Experience: ${psychologist.experience} years",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Certificates:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    psychologist.certificateUrls.forEach { url ->
                        Text(
                            "• Certificate attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, "Approve", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approve")
                        }
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, "Reject", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}

// PROFILE SCREEN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: com.google.firebase.auth.FirebaseUser,
    userProfile: UserProfile,
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onSignOut: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var isEditingBio by remember { mutableStateOf(false) }
    var editedBio by remember { mutableStateOf(userProfile.bio) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // ... (Image and Spacers for profile picture, name, and email)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                user.displayName ?: "User",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                user.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    // ➡️ REPLACED CODE STARTS HERE
                    userProfile.role.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                    },
                    // ⬅️ REPLACED CODE ENDS HERE
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Bio",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { isEditingBio = !isEditingBio }) {
                            Icon(
                                if (isEditingBio) Icons.Default.Close else Icons.Default.Edit,
                                "Edit Bio"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isEditingBio) {
                        OutlinedTextField(
                            value = editedBio,
                            onValueChange = { editedBio = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                db.collection("users").document(user.uid)
                                    .update("bio", editedBio)
                                isEditingBio = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    } else {
                        Text(
                            userProfile.bio.ifEmpty { "No bio added yet" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (userProfile.bio.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (userProfile.role == "psychologist") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Professional Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Specializations:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            userProfile.specializations.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Experience: ${userProfile.experience} years",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "License: ${userProfile.licenseNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "About MindSpace",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your personal companion for mental wellness, connecting clients with professional psychologists for comprehensive mental health support.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    onSignOut()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ExitToApp, "Sign Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// CLIENT SCREENS

            @Composable
            fun ClientHomeScreen(userProfile: UserProfile) {
                val motivationalQuotes = listOf(
                    "Every day is a fresh start. 🌅",
                    "You are stronger than you think. 💪",
                    "Progress, not perfection. 🌱",
                    "Be kind to yourself today. 💚",
                    "Your mental health matters. 🧠",
                    "Take it one step at a time. 👣",
                    "You've got this! ⭐",
                    "Breathe in peace, breathe out stress. 🌬️"
                )

                val quote = remember { motivationalQuotes.random() }
                val db = FirebaseFirestore.getInstance()
                var upcomingSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
                var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }

                LaunchedEffect(userProfile.uid) {
                    db.collection("sessions")
                        .whereEqualTo("clientId", userProfile.uid)
                        .whereEqualTo("status", "scheduled")
                        .limit(3)
                        .addSnapshotListener { snapshot, _ ->
                            upcomingSessions = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Session::class.java)?.copy(id = doc.id)
                            } ?: emptyList()
                        }

                    db.collection("resources")
                        .limit(5)
                        .addSnapshotListener { snapshot, _ ->
                            resources = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Resource::class.java)?.copy(id = doc.id)
                            } ?: emptyList()
                        }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Welcome back, ${userProfile.displayName.split(" ").first()}! 👋",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Daily Motivation",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    quote,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (upcomingSessions.isNotEmpty()) {
                        item {
                            Text(
                                "Upcoming Sessions",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        items(upcomingSessions) { session ->
                            SessionCard(session, isClient = true)
                        }
                    }

                    item {
                        Text(
                            "Helpful Resources",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    items(resources.take(3)) { resource ->
                        ResourceCard(resource)
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun MoodTrackerScreen(userId: String) {
                val db = FirebaseFirestore.getInstance()
                var moodEntries by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
                var showDialog by remember { mutableStateOf(false) }
                var editingEntry by remember { mutableStateOf<MoodEntry?>(null) }
                var showDeleteDialog by remember { mutableStateOf<MoodEntry?>(null) }

                LaunchedEffect(userId) {
                    db.collection("users").document(userId).collection("moods")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            moodEntries = snapshot?.documents?.mapNotNull { doc ->
                                MoodEntry(
                                    id = doc.id,
                                    userId = userId,
                                    mood = doc.getString("mood") ?: "",
                                    emoji = doc.getString("emoji") ?: "",
                                    note = doc.getString("note") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0L
                                )
                            } ?: emptyList()
                        }
                }

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add Mood")
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Mood Tracker",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "How are you feeling today?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(moodEntries) { entry ->
                            MoodEntryCard(
                                entry = entry,
                                onEdit = { editingEntry = it },
                                onDelete = { showDeleteDialog = it }
                            )
                        }

                        if (moodEntries.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Face,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No mood entries yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Tap the + button to track your first mood",
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDialog) {
                    AddMoodDialog(
                        onDismiss = { showDialog = false },
                        onSave = { mood, emoji, note ->
                            val moodData = hashMapOf(
                                "mood" to mood,
                                "emoji" to emoji,
                                "note" to note,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("users").document(userId).collection("moods")
                                .add(moodData)
                            showDialog = false
                        }
                    )
                }

                editingEntry?.let { entry ->
                    AddMoodDialog(
                        entry = entry,
                        onDismiss = { editingEntry = null },
                        onSave = { mood, emoji, note ->
                            db.collection("users").document(userId).collection("moods")
                                .document(entry.id)
                                .update(
                                    mapOf(
                                        "mood" to mood,
                                        "emoji" to emoji,
                                        "note" to note
                                    )
                                )
                            editingEntry = null
                        }
                    )
                }

                showDeleteDialog?.let { entry ->
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = null },
                        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        title = { Text("Delete Mood Entry?") },
                        text = { Text("This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    db.collection("users").document(userId).collection("moods")
                                        .document(entry.id)
                                        .delete()
                                    showDeleteDialog = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun MoodEntryCard(
                entry: MoodEntry,
                onEdit: (MoodEntry) -> Unit,
                onDelete: (MoodEntry) -> Unit
            ) {
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                entry.emoji,
                                style = MaterialTheme.typography.displaySmall,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.mood, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                        .format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    "More"
                                )
                            }
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                if (entry.note.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        entry.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onEdit(entry) }) {
                                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { onDelete(entry) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun AddMoodDialog(
                entry: MoodEntry? = null,
                onDismiss: () -> Unit,
                onSave: (String, String, String) -> Unit
            ) {
                var selectedMood by remember { mutableStateOf(entry?.mood ?: "") }
                var selectedEmoji by remember { mutableStateOf(entry?.emoji ?: "") }
                var note by remember { mutableStateOf(entry?.note ?: "") }

                val moods = listOf(
                    "Great" to "😄",
                    "Good" to "🙂",
                    "Okay" to "😐",
                    "Bad" to "😔",
                    "Anxious" to "😰"
                )

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(if (entry != null) "Edit Mood" else "How are you feeling?") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                moods.forEach { (mood, emoji) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedMood = mood
                                                selectedEmoji = emoji
                                            }
                                            .background(
                                                if (selectedMood == mood)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(emoji, style = MaterialTheme.typography.headlineMedium)
                                        Text(mood, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Add a note (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { onSave(selectedMood, selectedEmoji, note) },
                            enabled = selectedMood.isNotEmpty()
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun JournalScreen(userId: String) {
                val db = FirebaseFirestore.getInstance()
                var journalEntries by remember { mutableStateOf<List<JournalEntry>>(emptyList()) }
                var showDialog by remember { mutableStateOf(false) }
                var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
                var viewingEntry by remember { mutableStateOf<JournalEntry?>(null) }
                var showDeleteDialog by remember { mutableStateOf<JournalEntry?>(null) }

                LaunchedEffect(userId) {
                    db.collection("users").document(userId).collection("journals")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            journalEntries = snapshot?.documents?.mapNotNull { doc ->
                                JournalEntry(
                                    id = doc.id,
                                    userId = userId,
                                    title = doc.getString("title") ?: "",
                                    content = doc.getString("content") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0L
                                )
                            } ?: emptyList()
                        }
                }

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, "New Entry")
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "My Journal",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Your thoughts and reflections",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(journalEntries) { entry ->
                            JournalEntryCard(
                                entry = entry,
                                onView = { viewingEntry = it },
                                onEdit = { editingEntry = it },
                                onDelete = { showDeleteDialog = it }
                            )
                        }

                        if (journalEntries.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No journal entries yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Start writing your first entry",
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDialog) {
                    AddJournalDialog(
                        onDismiss = { showDialog = false },
                        onSave = { title, content ->
                            val journalData = hashMapOf(
                                "title" to title,
                                "content" to content,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("users").document(userId).collection("journals")
                                .add(journalData)
                            showDialog = false
                        }
                    )
                }

                editingEntry?.let { entry ->
                    AddJournalDialog(
                        entry = entry,
                        onDismiss = { editingEntry = null },
                        onSave = { title, content ->
                            db.collection("users").document(userId).collection("journals")
                                .document(entry.id)
                                .update(
                                    mapOf(
                                        "title" to title,
                                        "content" to content
                                    )
                                )
                            editingEntry = null
                        }
                    )
                }

                viewingEntry?.let { entry ->
                    ViewJournalDialog(
                        entry = entry,
                        onDismiss = { viewingEntry = null },
                        onEdit = {
                            viewingEntry = null
                            editingEntry = entry
                        }
                    )
                }

                showDeleteDialog?.let { entry ->
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = null },
                        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        title = { Text("Delete Journal Entry?") },
                        text = { Text("This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    db.collection("users").document(userId).collection("journals")
                                        .document(entry.id)
                                        .delete()
                                    showDeleteDialog = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            @Composable
            fun JournalEntryCard(
                entry: JournalEntry,
                onView: (JournalEntry) -> Unit,
                onEdit: (JournalEntry) -> Unit,
                onDelete: (JournalEntry) -> Unit
            ) {
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onView(entry) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    entry.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                        .format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.MoreVert, "More")
                            }
                        }

                        AnimatedVisibility(visible = expanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onEdit(entry) }) {
                                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { onDelete(entry) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun ViewJournalDialog(
                entry: JournalEntry,
                onDismiss: () -> Unit,
                onEdit: () -> Unit
            ) {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(entry.title) },
                    text = {
                        LazyColumn {
                            item {
                                Text(
                                    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                        .format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(entry.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onEdit) {
                            Text("Edit")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                )
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun AddJournalDialog(
                entry: JournalEntry? = null,
                onDismiss: () -> Unit,
                onSave: (String, String) -> Unit
            ) {
                var title by remember { mutableStateOf(entry?.title ?: "") }
                var content by remember { mutableStateOf(entry?.content ?: "") }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(if (entry != null) "Edit Journal Entry" else "New Journal Entry") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                label = { Text("What's on your mind?") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5,
                                maxLines = 10
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { onSave(title, content) },
                            enabled = title.isNotEmpty() && content.isNotEmpty()
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }

            @Composable
            fun ClientSessionsScreen(userProfile: UserProfile) {
                val db = FirebaseFirestore.getInstance()
                var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
                var psychologists by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
                var showBookingDialog by remember { mutableStateOf(false) }

                LaunchedEffect(userProfile.uid) {
                    db.collection("sessions")
                        .whereEqualTo("clientId", userProfile.uid)
                        .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            sessions = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Session::class.java)?.copy(id = doc.id)
                            } ?: emptyList()
                        }

                    db.collection("users")
                        .whereEqualTo("role", "psychologist")
                        .whereEqualTo("status", "active")
                        .addSnapshotListener { snapshot, _ ->
                            psychologists = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(UserProfile::class.java)
                            } ?: emptyList()
                        }
                }

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showBookingDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, "Book Session")
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "My Sessions",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Manage your therapy appointments",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(sessions) { session ->
                            SessionCard(session, isClient = true)
                        }

                        if (sessions.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No sessions booked",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Book your first session with a psychologist",
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showBookingDialog) {
                    BookSessionDialog(
                        psychologists = psychologists,
                        clientProfile = userProfile,
                        onDismiss = { showBookingDialog = false },
                        onBook = { psychId, psychName, date ->
                            val session = hashMapOf(
                                "psychologistId" to psychId,
                                "psychologistName" to psychName,
                                "clientId" to userProfile.uid,
                                "clientName" to userProfile.displayName,
                                "date" to date,
                                "duration" to 60,
                                "status" to "scheduled",
                                "notes" to "",
                                "meetingLink" to ""
                            )
                            db.collection("sessions").add(session)
                            showBookingDialog = false
                        }
                    )
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun BookSessionDialog(
                psychologists: List<UserProfile>,
                clientProfile: UserProfile,
                onDismiss: () -> Unit,
                onBook: (String, String, Long) -> Unit
            ) {
                var selectedPsych by remember { mutableStateOf<UserProfile?>(null) }
                var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
                var expanded by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text("Book a Session") },
                    text = {
                        Column {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedPsych?.displayName ?: "Select Psychologist",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    psychologists.forEach { psych ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(psych.displayName)
                                                    Text(
                                                        psych.specializations.joinToString(", "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedPsych = psych
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Select a date and time for your session",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                selectedPsych?.let { psych ->
                                    onBook(psych.uid, psych.displayName, selectedDate)
                                }
                            },
                            enabled = selectedPsych != null
                        ) {
                            Text("Book Session")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }

            @Composable
            fun SessionCard(session: Session, isClient: Boolean) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isClient) "with ${session.psychologistName}" else session.clientName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
                                        .format(Date(session.date)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${session.duration} minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = when (session.status) {
                                    "scheduled" -> Color(0xFFE3F2FD)
                                    "completed" -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFFFEBEE)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    // ✅ REPLACED CODE STARTS HERE:
                                    session.status.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                                    },
                                    // ✅ REPLACED CODE ENDS HERE
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (session.status) {
                                        "scheduled" -> Color(0xFF1976D2)
                                        "completed" -> Color(0xFF388E3C)
                                        else -> Color(0xFFD32F2F)
                                    }
                                )
                            }
                        }
                    }
                }
            }

// PSYCHOLOGIST SCREENS

            @Composable
            fun PsychologistHomeScreen(userProfile: UserProfile) {
                val db = FirebaseFirestore.getInstance()
                var todaySessions by remember { mutableStateOf<List<Session>>(emptyList()) }
                var totalClients by remember { mutableStateOf(0) }

                LaunchedEffect(userProfile.uid) {
                    val startOfDay = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis

                    val endOfDay = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis

                    db.collection("sessions")
                        .whereEqualTo("psychologistId", userProfile.uid)
                        .whereGreaterThanOrEqualTo("date", startOfDay)
                        .whereLessThanOrEqualTo("date", endOfDay)
                        .addSnapshotListener { snapshot, _ ->
                            todaySessions = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Session::class.java)?.copy(id = doc.id)
                            } ?: emptyList()
                        }

                    db.collection("sessions")
                        .whereEqualTo("psychologistId", userProfile.uid)
                        .addSnapshotListener { snapshot, _ ->
                            totalClients = snapshot?.documents?.map { it.getString("clientId") }?.distinct()?.size ?: 0
                        }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Welcome, Dr. ${userProfile.displayName.split(" ").last()}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        todaySessions.size.toString(),
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Text(
                                        "Today's Sessions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        totalClients.toString(),
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Text(
                                        "Total Clients",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Today's Schedule",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    items(todaySessions) { session ->
                        SessionCard(session, isClient = false)
                    }

                    if (todaySessions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No sessions scheduled for today",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            fun PsychologistSessionsScreen(userProfile: UserProfile) {
                val db = FirebaseFirestore.getInstance()
                var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }

                LaunchedEffect(userProfile.uid) {
                    db.collection("sessions")
                        .whereEqualTo("psychologistId", userProfile.uid)
                        .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            sessions = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Session::class.java)?.copy(id = doc.id)
                            } ?: emptyList()
                        }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "All Sessions",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Manage your client appointments",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(sessions) { session ->
                        SessionCard(session, isClient = false)
                    }

                    if (sessions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No sessions yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun SeminarsScreen(userProfile: UserProfile) {
                val db = FirebaseFirestore.getInstance()
                var seminars by remember { mutableStateOf<List<Seminar>>(emptyList()) }
                var showDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (userProfile.role == "psychologist") {
                        db.collection("seminars")
                            .whereEqualTo("psychologistId", userProfile.uid)
                            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, _ ->
                                seminars = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(Seminar::class.java)?.copy(id = doc.id)
                                } ?: emptyList()
                            }
                    } else {
                        db.collection("seminars")
                            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, _ ->
                                seminars = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(Seminar::class.java)?.copy(id = doc.id)
                                } ?: emptyList()
                            }
                    }
                }

                Scaffold(
                    floatingActionButton = {
                        if (userProfile.role == "psychologist") {
                            FloatingActionButton(
                                onClick = { showDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, "Create Seminar")
                            }
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                if (userProfile.role == "psychologist") "My Seminars" else "Available Seminars",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (userProfile.role == "psychologist")
                                    "Schedule and manage your seminars"
                                else
                                    "Register for upcoming seminars",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(seminars) { seminar ->
                            SeminarCard(
                                seminar = seminar,
                                userProfile = userProfile,
                                onRegister = { seminarId ->
                                    db.collection("seminars").document(seminarId).update(
                                        "registeredUsers",
                                        com.google.firebase.firestore.FieldValue.arrayUnion(userProfile.uid)
                                    )
                                }
                            )
                        }

                        if (seminars.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            if (userProfile.role == "psychologist")
                                                "No seminars created yet"
                                            else
                                                "No seminars available",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDialog && userProfile.role == "psychologist") {
                    CreateSeminarDialog(
                        userProfile = userProfile,
                        onDismiss = { showDialog = false },
                        onCreate = { title, description, date, duration, maxParticipants ->
                            val seminar = hashMapOf(
                                "psychologistId" to userProfile.uid,
                                "psychologistName" to userProfile.displayName,
                                "title" to title,
                                "description" to description,
                                "date" to date,
                                "duration" to duration,
                                "maxParticipants" to maxParticipants,
                                "registeredUsers" to emptyList<String>(),
                                "meetingLink" to ""
                            )
                            db.collection("seminars").add(seminar)
                            showDialog = false
                        }
                    )
                }
            }

            @Composable
            fun SeminarCard(
                seminar: Seminar,
                userProfile: UserProfile,
                onRegister: (String) -> Unit
            ) {
                val isRegistered = seminar.registeredUsers.contains(userProfile.uid)
                val isFull = seminar.registeredUsers.size >= seminar.maxParticipants

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            seminar.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "by ${seminar.psychologistName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            seminar.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                        .format(Date(seminar.date)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${seminar.registeredUsers.size}/${seminar.maxParticipants} registered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (userProfile.role == "client") {
                                Button(
                                    onClick = { onRegister(seminar.id) },
                                    enabled = !isRegistered && !isFull,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        when {
                                            isRegistered -> "Registered"
                                            isFull -> "Full"
                                            else -> "Register"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun CreateSeminarDialog(
                userProfile: UserProfile,
                onDismiss: () -> Unit,
                onCreate: (String, String, Long, Int, Int) -> Unit
            ) {
                var title by remember { mutableStateOf("") }
                var description by remember { mutableStateOf("") }
                var duration by remember { mutableStateOf("90") }
                var maxParticipants by remember { mutableStateOf("50") }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text("Create Seminar") },
                    text = {
                        LazyColumn {
                            item {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = duration,
                                    onValueChange = { duration = it },
                                    label = { Text("Duration (minutes)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = maxParticipants,
                                    onValueChange = { maxParticipants = it },
                                    label = { Text("Max Participants") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onCreate(
                                    title,
                                    description,
                                    System.currentTimeMillis() + (24 * 60 * 60 * 1000),
                                    duration.toIntOrNull() ?: 90,
                                    maxParticipants.toIntOrNull() ?: 50
                                )
                            },
                            enabled = title.isNotEmpty() && description.isNotEmpty()
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun ResourcesScreen(userProfile: UserProfile) {
                val db = FirebaseFirestore.getInstance()
                var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }
                var showDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (userProfile.role == "psychologist") {
                        db.collection("resources")
                            .whereEqualTo("psychologistId", userProfile.uid)
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, _ ->
                                resources = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(Resource::class.java)?.copy(id = doc.id)
                                } ?: emptyList()
                            }
                    } else {
                        db.collection("resources")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, _ ->
                                resources = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(Resource::class.java)?.copy(id = doc.id)
                                } ?: emptyList()
                            }
                    }
                }

                Scaffold(
                    floatingActionButton = {
                        if (userProfile.role == "psychologist") {
                            FloatingActionButton(
                                onClick = { showDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, "Add Resource")
                            }
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                if (userProfile.role == "psychologist") "My Resources" else "Resources",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (userProfile.role == "psychologist")
                                    "Share helpful content with clients"
                                else
                                    "Helpful resources from psychologists",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(resources) { resource ->
                            ResourceCard(resource)
                        }

                        if (resources.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            if (userProfile.role == "psychologist")
                                                "No resources added yet"
                                            else
                                                "No resources available",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDialog && userProfile.role == "psychologist") {
                    CreateResourceDialog(
                        userProfile = userProfile,
                        onDismiss = { showDialog = false },
                        onCreate = { title, description, type, url ->
                            val resource = hashMapOf(
                                "psychologistId" to userProfile.uid,
                                "psychologistName" to userProfile.displayName,
                                "title" to title,
                                "description" to description,
                                "type" to type,
                                "url" to url,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("resources").add(resource)
                            showDialog = false
                        }
                    )
                }
            }

            @Composable
            fun ResourceCard(resource: Resource) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                when (resource.type) {
                                    "video" -> Icons.Default.PlayArrow
                                    "article" -> Icons.Default.Info
                                    else -> Icons.Default.Favorite
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(resource.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "by ${resource.psychologistName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    resource.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (resource.url.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        resource.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun CreateResourceDialog(
                userProfile: UserProfile,
                onDismiss: () -> Unit,
                onCreate: (String, String, String, String) -> Unit
            ) {
                var title by remember { mutableStateOf("") }
                var description by remember { mutableStateOf("") }
                var type by remember { mutableStateOf("article") }
                var url by remember { mutableStateOf("") }
                var expanded by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text("Share a Resource") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = type.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Type") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("article", "video", "exercise").forEach { resourceType ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(resourceType.replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                                                })
                                            },
                                            onClick = {
                                                type = resourceType
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text("URL (optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { onCreate(title, description, type, url) },
                            enabled = title.isNotEmpty() && description.isNotEmpty()
                        ) {
                            Text("Share")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }
// Continue with remaining functions in next message...

// ADMIN SCREENS (Complete)

            @Composable
            fun AdminDashboardScreen() {
                val db = FirebaseFirestore.getInstance()
                var totalUsers by remember { mutableStateOf(0) }
                var totalPsychologists by remember { mutableStateOf(0) }
                var pendingApprovals by remember { mutableStateOf(0) }
                var totalSessions by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    db.collection("users").addSnapshotListener { snapshot, _ ->
                        totalUsers = snapshot?.size() ?: 0
                    }
                    db.collection("users")
                        .whereEqualTo("role", "psychologist")
                        .whereEqualTo("status", "active")
                        .addSnapshotListener { snapshot, _ ->
                            totalPsychologists = snapshot?.size() ?: 0
                        }
                    db.collection("users")
                        .whereEqualTo("role", "psychologist")
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { snapshot, _ ->
                            pendingApprovals = snapshot?.size() ?: 0
                        }
                    db.collection("sessions").addSnapshotListener { snapshot, _ ->
                        totalSessions = snapshot?.size() ?: 0
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Admin Dashboard",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Platform Overview",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatsCard(
                                title = "Total Users",
                                value = totalUsers.toString(),
                                icon = Icons.Default.Person,
                                modifier = Modifier.weight(1f)
                            )
                            StatsCard(
                                title = "Psychologists",
                                value = totalPsychologists.toString(),
                                icon = Icons.Default.Favorite,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatsCard(
                                title = "Pending",
                                value = pendingApprovals.toString(),
                                icon = Icons.Default.Info,
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFFFFF3E0)
                            )
                            StatsCard(
                                title = "Sessions",
                                value = totalSessions.toString(),
                                icon = Icons.Default.CalendarToday,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Quick Actions",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "• Review pending psychologist applications",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "• Monitor platform activity",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "• Manage user accounts",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            @Composable
            fun StatsCard(
                title: String,
                value: String,
                icon: androidx.compose.ui.graphics.vector.ImageVector,
                modifier: Modifier = Modifier,
                containerColor: Color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Card(
                    modifier = modifier,
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            value,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            @Composable
            fun AdminApprovalsScreen() {
                val db = FirebaseFirestore.getInstance()
                var pendingPsychologists by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

                LaunchedEffect(Unit) {
                    db.collection("users")
                        .whereEqualTo("role", "psychologist")
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { snapshot, _ ->
                            pendingPsychologists = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(UserProfile::class.java)
                            } ?: emptyList()
                        }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Pending Approvals",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Review psychologist applications",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(pendingPsychologists) { psychologist ->
                        PsychologistApprovalCard(
                            psychologist = psychologist,
                            onApprove = {
                                db.collection("users").document(psychologist.uid)
                                    .update("status", "active")
                            },
                            onReject = {
                                db.collection("users").document(psychologist.uid)
                                    .update("status", "rejected")
                            }
                        )
                    }

                    if (pendingPsychologists.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No pending approvals",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "All applications have been reviewed",
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }