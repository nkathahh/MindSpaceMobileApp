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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.util.*

val db = FirebaseFirestore.getInstance("mindspacedb")

// Profile sync function to ensure all auth users have Firestore profiles
suspend fun ensureUserProfileExists(
    user: com.google.firebase.auth.FirebaseUser,
    db: FirebaseFirestore
): UserProfile? {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val docRef = db.collection("users").document(user.uid)

        println("🔍 Checking profile for user: ${user.uid}")
        println("📧 Email: ${user.email}")
        println("👤 Display Name: ${user.displayName}")

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Profile exists, return it
                    println("✅ Profile found for user: ${user.uid}")
                    val profile = document.toObject(UserProfile::class.java)
                    continuation.resume(profile) {}
                } else {
                    // Profile doesn't exist, create it
                    println("⚠️ Profile NOT found, creating new profile for: ${user.uid}")
                    val isGoogleAuth = user.providerData.any { it.providerId == "google.com" }
                    val newProfile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: user.email?.split("@")?.first() ?: "User",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        bio = "",
                        role = "client",
                        status = "active",
                        authProvider = if (isGoogleAuth) "google" else "email",
                        createdAt = com.google.firebase.Timestamp.now()
                    )

                    println("📝 Attempting to save profile to Firestore...")
                    println("📄 Profile data: $newProfile")

                    docRef.set(newProfile)
                        .addOnSuccessListener {
                            println("✅ SUCCESS! Profile created for user: ${user.uid}")
                            continuation.resume(newProfile) {}
                        }
                        .addOnFailureListener { e ->
                            println("❌ FAILED to create profile!")
                            println("❌ Error: ${e.message}")
                            println("❌ Error details: ${e.localizedMessage}")
                            e.printStackTrace()
                            continuation.resume(null) {}
                        }
                }
            }
            .addOnFailureListener { e ->
                println("❌ FAILED to check profile existence!")
                println("❌ Error: ${e.message}")
                println("❌ Error details: ${e.localizedMessage}")
                e.printStackTrace()
                continuation.resume(null) {}
            }
    }
}


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
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val authProvider: String = "email" // email, google
)

data class MoodEntry(
    val id: String = "",
    val userId: String = "",
    val mood: String = "",
    val emoji: String = "",
    val note: String = "",
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

data class Session(
    val id: String = "",
    val psychologistId: String = "",
    val psychologistName: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val date: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
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
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

data class Seminar(
    val id: String = "",
    val psychologistId: String = "",
    val psychologistName: String = "",
    val title: String = "",
    val description: String = "",
    val date: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val duration: Int = 90,
    val maxParticipants: Int = 50,
    val registeredUsers: List<String> = emptyList(),
    val meetingLink: String = ""
)



fun getDailyQuote(quotes: List<String>): String {
    // Get the day of the year (1-366) as a consistent seed
    val calendar = Calendar.getInstance()
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

    val index = dayOfYear % quotes.size

    return quotes[index]
}

data class SessionRequest(
    val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val psychologistId: String = "",
    val psychologistName: String = "",
    val requestedDate: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val duration: Int = 60,
    val status: String = "pending", // pending, approved, rejected, cancelled
    val clientNotes: String = "",
    val psychologistResponse: String = "",
    val finalDate: com.google.firebase.Timestamp? = null,
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

data class AvailableSlot(
    val id: String = "",
    val psychologistId: String = "",
    val date: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val duration: Int = 60,
    val isBooked: Boolean = false,
    val bookedBy: String = ""
)


@Composable
fun DetailedStatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun UserCard(
    userProfile: UserProfile,
    onUpdateStatus: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val isPsychologist = userProfile.role == "psychologist"
    val isAccountActive = userProfile.status == "active"
    // Requires java.util.Locale import
    val cardColor = if (isAccountActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon/Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPsychologist) Icons.Default.Favorite else Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                // User Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        userProfile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        userProfile.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Status: ${userProfile.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAccountActive) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                    )
                }

                // Details Toggle
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        "Details",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Bio
                    Text("Bio:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(userProfile.bio.ifEmpty { "No bio provided" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (isPsychologist) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Specializations:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(userProfile.specializations.joinToString(", ").ifEmpty { "N/A" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("License:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(userProfile.licenseNumber.ifEmpty { "N/A" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newStatus = if (isAccountActive) "deactivated" else "active"
                                onUpdateStatus(userProfile.uid, newStatus)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccountActive) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                            )
                        ) {
                            Text(if (isAccountActive) "Deactivate" else "Activate")
                        }
                        OutlinedButton(
                            onClick = { onDelete(userProfile.uid) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickLinkCard(
    modifier: Modifier = Modifier, // MODIFIER PARAMETER MUST BE THE FIRST ARGUMENT
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier // Apply the external modifier (including weight) here
            .height(150.dp)
            .clickable { /* Handle navigation to Mood/Journal screen */ },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f)) // Push subtitle to bottom
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}


@Composable
fun AdminUserManagementScreen() {
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .orderBy("role", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                allUsers = snapshot?.documents?.mapNotNull { doc ->
                    // FIX: Manual conversion for createdAt field to handle old Long data gracefully
                    val createdAtValue = doc.get("createdAt")
                    val createdAtTimestamp = when (createdAtValue) {
                        is com.google.firebase.Timestamp -> createdAtValue
                        is Long -> com.google.firebase.Timestamp(Date(createdAtValue))
                        else -> com.google.firebase.Timestamp.now() // Fallback
                    }

                    // Manually map fields (mandatory because the primary model field 'createdAt' has old data)
                    val profile = UserProfile(
                        uid = doc.getString("uid") ?: "",
                        email = doc.getString("email") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        bio = doc.getString("bio") ?: "",
                        role = doc.getString("role") ?: "client",
                        status = doc.getString("status") ?: "active",
                        specializations = doc.get("specializations") as? List<String> ?: emptyList(),
                        experience = doc.getString("experience") ?: "",
                        licenseNumber = doc.getString("licenseNumber") ?: "",
                        certificateUrls = doc.get("certificateUrls") as? List<String> ?: emptyList(),
                        createdAt = createdAtTimestamp, // Use the converted Timestamp
                        authProvider = doc.getString("authProvider") ?: "email"
                    )
                    profile // Return the manually constructed profile
                }?.filter { it.role != "admin" } ?: emptyList()
            }
    }

    // Function to update user status (deactivate/activate)
    val onUpdateStatus: (String, String) -> Unit = { uid, newStatus ->
        db.collection("users").document(uid).update("status", newStatus)
    }

    // Function to delete user (Note: This only deletes the Firestore profile, not Firebase Auth user)
    val onDeleteUser: (String) -> Unit = { uid ->
        db.collection("users").document(uid).delete()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "User Management",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "View, manage, and moderate all platform users (excluding admins).",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(allUsers) { userProfile ->
            UserCard(
                userProfile = userProfile,
                onUpdateStatus = onUpdateStatus,
                onDelete = onDeleteUser
            )
        }

        if (allUsers.isEmpty()) {
            item {
                Text(
                    "No non-admin users found.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isHighlight: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isHighlight) Color(0xFFF57C00) else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            color = if (isHighlight) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AdminApprovalsScreen() {
    val db = FirebaseFirestore.getInstance("mindspacedb")
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

// --- END OF UNRESOLVED REFERENCE DEFINITIONS ---


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
                        // --- NEW ADMIN ITEM START ---
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Group, "Users") },
                            label = { Text("Users") },
                            selected = currentScreen == "users",
                            onClick = { currentScreen = "users" }
                        )
                        // --- NEW ADMIN ITEM END ---
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
                        "users" -> AdminUserManagementScreen()
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
    val db = FirebaseFirestore.getInstance("mindspacedb")

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

    // Listen to user profile changes AND clean up the listener properly
    DisposableEffect(user?.uid) {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        // Only run logic if a user is authenticated
        if (user != null) {
            // Set loading flag initially
            isLoading = true

            // Launch a coroutine to first ensure the profile exists, then set up the listener.
            val fetchJob = GlobalScope.launch {
                val fetchedUser = user!!
                val syncedProfile = ensureUserProfileExists(fetchedUser, db)

                // CRITICAL: We need a copy of the final profile before setting up the listener
                userProfile = syncedProfile

                val docRef = db.collection("users").document(fetchedUser.uid)

                // Set up the real-time listener
                listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                    isLoading = false

                    if (error != null) {
                        println("❌ Firestore listener error: ${error.localizedMessage}")
                        // Fallback state if listener breaks
                        needsProfileCompletion = userProfile?.role == "psychologist" &&
                                (userProfile!!.specializations.isEmpty() || userProfile!!.licenseNumber.isEmpty())
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val profile = snapshot.toObject(UserProfile::class.java)

                        if (profile != null) {
                            userProfile = profile

                            // Recalculate based on the live profile
                            needsProfileCompletion = profile.role == "psychologist" &&
                                    (profile.specializations.isEmpty() || profile.licenseNumber.isEmpty())
                            isGoogleSignIn = profile.authProvider == "google"
                        }
                    }
                }
            }

            // Cleanup block for DisposableEffect
            onDispose {
                fetchJob.cancel() // Cancel the coroutine job if UID changes or component disposed
                listenerRegistration?.remove() // Remove the Firestore listener
            }
        } else {
            // User is signed out, reset state
            isLoading = false
            userProfile = null
            needsProfileCompletion = false
            isGoogleSignIn = false

            // Cleanup block
            onDispose { /* No cleanup needed when user is null */ }
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

    val db = FirebaseFirestore.getInstance("mindspacedb")

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
                                        // Create user profile in Firestore FIRST
                                        val userProfile = UserProfile(
                                            uid = user.uid,
                                            email = email,
                                            displayName = displayName,
                                            photoUrl = "",
                                            bio = "",
                                            role = if (registerAsPsychologist) "psychologist" else "client",
                                            status = if (registerAsPsychologist) "pending" else "active",
                                            authProvider = "email",
                                            createdAt = com.google.firebase.Timestamp.now()
                                        )

                                        // Save to Firestore
                                        db.collection("users").document(user.uid)
                                            .set(userProfile)
                                            .addOnSuccessListener {
                                                // THEN update Firebase Auth profile
                                                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                    .setDisplayName(displayName)
                                                    .build()

                                                user.updateProfile(profileUpdates)
                                                    .addOnSuccessListener {
                                                        isLoading = false
                                                        // Force reload to trigger auth state listener
                                                        user.reload().addOnCompleteListener {
                                                            onRegisterSuccess()
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isLoading = false
                                                        errorMessage = "Profile update failed: ${e.message}"
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "Failed to create profile: ${e.localizedMessage}"
                                                // Delete auth user if Firestore save fails
                                                user.delete()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = when {
                                        e.message?.contains("email address is already in use") == true ->
                                            "This email is already registered"
                                        e.message?.contains("email address is badly formatted") == true ->
                                            "Invalid email format"
                                        e.message?.contains("network") == true ->
                                            "Network error. Please check your connection"
                                        else -> "Registration failed: ${e.localizedMessage}"
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val certificatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        certificateUris = uris
        errorMessage = null  // Clear error when files are selected
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
                    onClick = {
                        selectedRole = "client"
                        errorMessage = null
                    },
                    label = { Text("Client") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRole == "psychologist",
                    onClick = {
                        selectedRole = "psychologist"
                        errorMessage = null
                    },
                    label = { Text("Psychologist") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
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

        if (selectedRole == "psychologist") {
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

        // ✅ ERROR MESSAGE DISPLAY
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Button(
                onClick = {
                    when {
                        bio.isBlank() -> errorMessage = "Please enter your bio"
                        selectedRole == "psychologist" && specializations.isBlank() ->
                            errorMessage = "Please enter your specializations"
                        selectedRole == "psychologist" && experience.isBlank() ->
                            errorMessage = "Please enter your years of experience"
                        selectedRole == "psychologist" && licenseNumber.isBlank() ->
                            errorMessage = "Please enter your license number"
                        selectedRole == "psychologist" && certificateUris.isEmpty() ->
                            errorMessage = "Please upload at least one certificate"
                        else -> {
                            isUploading = true
                            errorMessage = null

                            val profile = UserProfile(
                                uid = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: "",
                                photoUrl = user.photoUrl?.toString() ?: "",
                                bio = bio,
                                role = selectedRole,
                                status = if (selectedRole == "psychologist") "pending" else "active",
                                specializations = if (selectedRole == "psychologist")
                                    specializations.split(",").map { it.trim() }
                                else emptyList(),
                                experience = experience,
                                licenseNumber = licenseNumber,
                                authProvider = "google",
                                createdAt = com.google.firebase.Timestamp.now()
                            )

                            if (selectedRole == "psychologist" && certificateUris.isNotEmpty()) {
                                val storage = FirebaseStorage.getInstance()
                                val uploadedUrls = mutableListOf<String>()
                                var uploadCount = 0

                                certificateUris.forEachIndexed { index, uri ->
                                    val ref = storage.reference
                                        .child("certificates/${user.uid}/${System.currentTimeMillis()}_$index.jpg")

                                    ref.putFile(uri)
                                        .addOnSuccessListener {
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
                            } else {
                                // For clients or if no certificates
                                db.collection("users").document(user.uid)
                                    .set(profile)
                                    .addOnSuccessListener {
                                        isUploading = false
                                        onProfileCreated(profile)
                                    }
                                    .addOnFailureListener { e ->
                                        isUploading = false
                                        errorMessage = "Failed to save profile: ${e.localizedMessage}"
                                    }
                            }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Uploading...")
                    }
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
    val context = LocalContext.current // Get Context to launch URL intents

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture (Placeholder or image)
                Image(
                    painter = rememberAsyncImagePainter(psychologist.photoUrl),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        psychologist.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        psychologist.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        "Details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // --- Professional Details Section ---
                    Text(
                        "Professional Details:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Specializations
                    InfoRow(
                        icon = Icons.Default.Star,
                        label = "Specializations",
                        value = psychologist.specializations.joinToString(", ")
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Experience
                    InfoRow(
                        icon = Icons.Default.DateRange,
                        label = "Experience",
                        value = "${psychologist.experience} years"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // License
                    InfoRow(
                        icon = Icons.Default.VpnKey,
                        label = "License Number",
                        value = psychologist.licenseNumber
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Bio
                    Text(
                        "Bio:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        psychologist.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Certificates Section with clickable links ---
                    Text(
                        "Certificates & Documents:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    psychologist.certificateUrls.forEachIndexed { index, url ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // ACTION: Open the URL in a browser
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    // Necessary flag when launching activity from a non-activity context
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Certificate ${index + 1} (Click to View)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Approval Buttons ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, "Approve", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approve")
                        }
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, "Reject", modifier = Modifier.size(20.dp))
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var isEditingBio by remember { mutableStateOf(false) }
    var editedBio by remember { mutableStateOf(userProfile.bio) }
    var showSignOutDialog by remember { mutableStateOf(false) }

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
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section with Profile Picture
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Picture
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6750A4),
                                        Color(0xFF7D5260)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(user.photoUrl),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                user.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    Text(
                        user.displayName ?: "User",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email
                    Text(
                        user.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (userProfile.role) {
                                    "client" -> Icons.Default.Face
                                    "psychologist" -> Icons.Default.Favorite
                                    "admin" -> Icons.Default.Settings
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                userProfile.role.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Content Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Bio Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "About Me",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { isEditingBio = !isEditingBio },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isEditingBio) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    if (isEditingBio) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = if (isEditingBio) "Cancel" else "Edit Bio",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isEditingBio) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isEditingBio) {
                            OutlinedTextField(
                                value = editedBio,
                                onValueChange = { editedBio = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    db.collection("users").document(user.uid)
                                        .update("bio", editedBio)
                                    isEditingBio = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, "Save", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Changes")
                            }
                        } else {
                            Text(
                                userProfile.bio.ifEmpty { "Tell us about yourself..." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (userProfile.bio.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Professional Info Card (for psychologists)
                if (userProfile.role == "psychologist") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Professional Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Specializations
                            InfoRow(
                                icon = Icons.Default.Star,
                                label = "Specializations",
                                value = userProfile.specializations.joinToString(", ")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Experience
                            InfoRow(
                                icon = Icons.Default.DateRange,
                                label = "Experience",
                                value = "${userProfile.experience} years"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // License
                            InfoRow(
                                icon = Icons.Default.CheckCircle,
                                label = "License Number",
                                value = userProfile.licenseNumber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // App Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "About MindSpace",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Your personal companion for mental wellness, connecting clients with professional psychologists for comprehensive mental health support.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Button
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Sign Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Sign Out?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to sign out of your account?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        auth.signOut()
                        googleSignInClient.signOut()
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Sign Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// CLIENT SCREENS

@Composable
fun ClientHomeScreen(userProfile: UserProfile) {
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var upcomingSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var latestMood by remember { mutableStateOf<MoodEntry?>(null) }
    var latestJournal by remember { mutableStateOf<JournalEntry?>(null) }
    var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }

    // Use the function to get a quote that is constant for the day
    val motivationalQuotes = remember {
        listOf(
            "Every day is a fresh start. 🌅",
            "You are stronger than you think. 💪",
            "Progress, not perfection. 🌱",
            "Be kind to yourself today. 💚",
            "Your mental health matters. 🧠",
            "Take it one step at a time. 👣",
            "You've got this! ⭐",
            "Breathe in peace, breathe out stress. 🌬️"
        )
    }
    val dailyQuote = remember { getDailyQuote(motivationalQuotes) }

    LaunchedEffect(userProfile.uid) {
        // Fetch Upcoming Sessions (Manual Deserialization for robustness)
        db.collection("sessions")
            .whereEqualTo("clientId", userProfile.uid)
            .whereEqualTo("status", "scheduled")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limit(3)
            .addSnapshotListener { snapshot, _ ->
                upcomingSessions = snapshot?.documents?.mapNotNull { doc ->
                    val dateValue = doc.get("date")
                    val dateTimestamp = when (dateValue) {
                        is com.google.firebase.Timestamp -> dateValue
                        is Long -> com.google.firebase.Timestamp(Date(dateValue))
                        else -> com.google.firebase.Timestamp.now()
                    }
                    Session(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        date = dateTimestamp,
                        duration = doc.getLong("duration")?.toInt() ?: 60,
                        status = doc.getString("status") ?: "scheduled",
                        notes = doc.getString("notes") ?: "",
                        meetingLink = doc.getString("meetingLink") ?: ""
                    )
                } ?: emptyList()
            }

        // Fetch Latest Mood Entry (Manual Deserialization for robustness)
        db.collection("users").document(userProfile.uid).collection("moods")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                latestMood = snapshot?.documents?.mapNotNull { doc ->
                    val timestampValue = doc.get("timestamp")
                    val timestamp = when (timestampValue) {
                        is com.google.firebase.Timestamp -> timestampValue
                        is Long -> com.google.firebase.Timestamp(Date(timestampValue))
                        else -> com.google.firebase.Timestamp.now()
                    }
                    MoodEntry(
                        id = doc.id,
                        userId = userProfile.uid,
                        mood = doc.getString("mood") ?: "",
                        emoji = doc.getString("emoji") ?: "",
                        note = doc.getString("note") ?: "",
                        timestamp = timestamp
                    )
                }?.firstOrNull()
            }

        // Fetch Latest Journal Entry (Manual Deserialization for robustness)
        db.collection("users").document(userProfile.uid).collection("journals")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                latestJournal = snapshot?.documents?.mapNotNull { doc ->
                    val timestampValue = doc.get("timestamp")
                    val timestamp = when (timestampValue) {
                        is com.google.firebase.Timestamp -> timestampValue
                        is Long -> com.google.firebase.Timestamp(Date(timestampValue))
                        else -> com.google.firebase.Timestamp.now()
                    }
                    JournalEntry(
                        id = doc.id,
                        userId = userProfile.uid,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = timestamp
                    )
                }?.firstOrNull()
            }


        // Fetch Top Resources
        db.collection("resources")
            .limit(3)
            .addSnapshotListener { snapshot, _ ->
                resources = snapshot?.documents?.mapNotNull { doc ->
                    val timestampValue = doc.get("timestamp")
                    val timestamp = when (timestampValue) {
                        is com.google.firebase.Timestamp -> timestampValue
                        is Long -> com.google.firebase.Timestamp(Date(timestampValue))
                        else -> com.google.firebase.Timestamp.now()
                    }
                    Resource(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        type = doc.getString("type") ?: "",
                        url = doc.getString("url") ?: "",
                        timestamp = timestamp
                    )
                } ?: emptyList()
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Welcome back, ${userProfile.displayName.split(" ").first()}!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your path to wellness.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Daily Motivation Card (Modern, fixed-daily content)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Quote of the Day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        dailyQuote,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 3. Wellness Summary Cards (Side-by-side metrics)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latest Mood Quick View
                QuickLinkCard(
                    title = "My Latest Mood",
                    value = latestMood?.emoji ?: "❓",
                    subtitle = latestMood?.mood ?: "No Entry Today",
                    icon = Icons.Default.Face,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Latest Journal Quick View
                QuickLinkCard(
                    title = "My Latest Entry",
                    value = latestJournal?.title ?: "✍️",
                    subtitle = latestJournal?.title?.take(15) + if ((latestJournal?.title?.length ?: 0) > 15) "..." else "" ?: "Write Something",
                    icon = Icons.Default.Edit,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // 4. Upcoming Sessions
        item {
            Text(
                "Upcoming Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (upcomingSessions.isNotEmpty()) {
            items(upcomingSessions) { session ->
                SessionCard(session, isClient = true)
            }
        } else {
            item {
                EmptyStateCard(
                    message = "You have no scheduled sessions.",
                    actionText = "Book a Session",
                    icon = Icons.Default.CalendarToday,
                    onAction = { /* TODO: Navigate to Sessions screen */ }
                )
            }
        }

        // 5. Featured Resources
        item {
            Text(
                "Featured Resources",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var moodEntries by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MoodEntry?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MoodEntry?>(null) }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("moods")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                moodEntries = snapshot?.documents?.mapNotNull { doc ->
                    // FIX: Manual conversion for MoodEntry timestamp field to handle old Long data gracefully
                    val timestampValue = doc.get("timestamp")
                    val timestamp = when (timestampValue) {
                        is com.google.firebase.Timestamp -> timestampValue
                        is Long -> com.google.firebase.Timestamp(Date(timestampValue))
                        else -> doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now()
                    }

                    MoodEntry(
                        id = doc.id,
                        userId = userId,
                        mood = doc.getString("mood") ?: "",
                        emoji = doc.getString("emoji") ?: "",
                        note = doc.getString("note") ?: "",
                        timestamp = timestamp
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
                    "timestamp" to com.google.firebase.Timestamp.now()
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
                            .format(entry.timestamp.toDate()),
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
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
                    // FIX: Manual conversion for JournalEntry timestamp field to handle old Long data gracefully
                    val timestampValue = doc.get("timestamp")
                    val timestamp = when (timestampValue) {
                        is com.google.firebase.Timestamp -> timestampValue
                        is Long -> com.google.firebase.Timestamp(Date(timestampValue))
                        else -> doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now()
                    }

                    JournalEntry(
                        id = doc.id,
                        userId = userId,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = timestamp
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
                    "timestamp" to com.google.firebase.Timestamp.now()
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
                            .format(entry.timestamp.toDate()),
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
                            .format(entry.timestamp.toDate()),
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var psychologists by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showBookingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile.uid) {
        db.collection("sessions")
            .whereEqualTo("clientId", userProfile.uid)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                sessions = snapshot?.documents?.mapNotNull { doc ->
                    // FIX: Manual conversion for Session date field to handle old Long data gracefully
                    val dateValue = doc.get("date")
                    val dateTimestamp = when (dateValue) {
                        is com.google.firebase.Timestamp -> dateValue
                        is Long -> com.google.firebase.Timestamp(Date(dateValue))
                        else -> com.google.firebase.Timestamp.now() // Fallback
                    }

                    Session(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        date = dateTimestamp,
                        duration = doc.getLong("duration")?.toInt() ?: 60,
                        status = doc.getString("status") ?: "scheduled",
                        notes = doc.getString("notes") ?: "",
                        meetingLink = doc.getString("meetingLink") ?: ""
                    )
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
    onBook: (String, String, com.google.firebase.Timestamp) -> Unit
) {
    var selectedPsych by remember { mutableStateOf<UserProfile?>(null) }
    var selectedDate by remember { mutableStateOf(com.google.firebase.Timestamp.now()) }
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
                            .format(session.date.toDate()),
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var todaySessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var upcomingSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var totalClients by remember { mutableStateOf(0) }
    var completedSessionsThisWeek by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userProfile.uid) {
        // Get today's date range
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val startTimestamp = com.google.firebase.Timestamp(Date(startOfToday))
        val endTimestamp = com.google.firebase.Timestamp(Date(endOfToday))

        // Today's sessions
        db.collection("sessions")
            .whereEqualTo("psychologistId", userProfile.uid)
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .addSnapshotListener { snapshot, _ ->
                todaySessions = snapshot?.documents?.mapNotNull { doc ->
                    val dateValue = doc.get("date")
                    val dateTimestamp = when (dateValue) {
                        is com.google.firebase.Timestamp -> dateValue
                        is Long -> com.google.firebase.Timestamp(Date(dateValue))
                        else -> com.google.firebase.Timestamp.now()
                    }
                    Session(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        date = dateTimestamp,
                        duration = doc.getLong("duration")?.toInt() ?: 60,
                        status = doc.getString("status") ?: "scheduled",
                        notes = doc.getString("notes") ?: "",
                        meetingLink = doc.getString("meetingLink") ?: ""
                    )
                }?.sortedBy { it.date.toDate() } ?: emptyList()
                isLoading = false
            }

        // Upcoming sessions (next 7 days, excluding today)
        val tomorrowStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val next7Days = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        db.collection("sessions")
            .whereEqualTo("psychologistId", userProfile.uid)
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(Date(tomorrowStart)))
            .whereLessThanOrEqualTo("date", com.google.firebase.Timestamp(Date(next7Days)))
            .addSnapshotListener { snapshot, _ ->
                upcomingSessions = snapshot?.documents?.mapNotNull { doc ->
                    val dateValue = doc.get("date")
                    val dateTimestamp = when (dateValue) {
                        is com.google.firebase.Timestamp -> dateValue
                        is Long -> com.google.firebase.Timestamp(Date(dateValue))
                        else -> com.google.firebase.Timestamp.now()
                    }
                    Session(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        date = dateTimestamp,
                        duration = doc.getLong("duration")?.toInt() ?: 60,
                        status = doc.getString("status") ?: "scheduled",
                        notes = doc.getString("notes") ?: "",
                        meetingLink = doc.getString("meetingLink") ?: ""
                    )
                }?.sortedBy { it.date.toDate() }?.take(5) ?: emptyList()
            }

        // Total unique clients
        db.collection("sessions")
            .whereEqualTo("psychologistId", userProfile.uid)
            .addSnapshotListener { snapshot, _ ->
                totalClients = snapshot?.documents?.map { it.getString("clientId") }?.distinct()?.size ?: 0
            }

        // Completed sessions this week
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        db.collection("sessions")
            .whereEqualTo("psychologistId", userProfile.uid)
            .whereEqualTo("status", "completed")
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(Date(startOfWeek)))
            .addSnapshotListener { snapshot, _ ->
                completedSessionsThisWeek = snapshot?.size() ?: 0
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF3E5F5),
                                Color(0xFFFFFBFE)
                            )
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Header Section
                item {
                    Column {
                        Text(
                            "Good ${getTimeOfDay()},",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Dr. ${userProfile.displayName.split(" ").lastOrNull() ?: userProfile.displayName}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Stats Cards Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Today's Sessions
                        ModernStatCard(
                            title = "Today",
                            value = todaySessions.size.toString(),
                            subtitle = if (todaySessions.size == 1) "session" else "sessions",
                            icon = Icons.Default.CalendarToday,
                            color = Color(0xFF6750A4),
                            modifier = Modifier.weight(1f)
                        )

                        // Total Clients
                        ModernStatCard(
                            title = "Clients",
                            value = totalClients.toString(),
                            subtitle = "total",
                            icon = Icons.Default.Face,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // This Week
                        ModernStatCard(
                            title = "This Week",
                            value = completedSessionsThisWeek.toString(),
                            subtitle = "completed",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )

                        // Upcoming
                        ModernStatCard(
                            title = "Upcoming",
                            value = upcomingSessions.size.toString(),
                            subtitle = "next 7 days",
                            icon = Icons.Default.DateRange,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Today's Schedule Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Today's Schedule",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (todaySessions.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "${todaySessions.size} ${if (todaySessions.size == 1) "session" else "sessions"}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (todaySessions.isNotEmpty()) {
                    items(todaySessions) { session ->
                        ModernSessionCard(session)
                    }
                } else {
                    item {
                        EmptyScheduleCard()
                    }
                }

                // Upcoming Sessions Section
                if (upcomingSessions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Upcoming Sessions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(upcomingSessions.take(3)) { session ->
                        CompactSessionCard(session)
                    }
                }

                // Bottom Spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Helper function for greeting
private fun getTimeOfDay(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }
}

@Composable
fun ModernStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = color.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                }
            }

            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModernSessionCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Badge
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(70.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6750A4),
                                Color(0xFF7D5260)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(session.date.toDate()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        SimpleDateFormat("a", Locale.getDefault()).format(session.date.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Session Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.clientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${session.duration} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Status Badge
            Surface(
                color = when (session.status) {
                    "scheduled" -> Color(0xFFE3F2FD)
                    "completed" -> Color(0xFFE8F5E9)
                    else -> Color(0xFFFFEBEE)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (session.status) {
                                    "scheduled" -> Color(0xFF1976D2)
                                    "completed" -> Color(0xFF388E3C)
                                    else -> Color(0xFFD32F2F)
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        session.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
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

@Composable
fun CompactSessionCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(session.date.toDate()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    session.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(session.date.toDate()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyScheduleCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(0xFF6750A4).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF6750A4)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Sessions Today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enjoy your free day! Check upcoming sessions below.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PsychologistSessionsScreen(userProfile: UserProfile) {
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }

    LaunchedEffect(userProfile.uid) {
        db.collection("sessions")
            .whereEqualTo("psychologistId", userProfile.uid)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                sessions = snapshot?.documents?.mapNotNull { doc ->
                    // FIX: Manual conversion for Session date field to handle old Long data gracefully
                    val dateValue = doc.get("date")
                    val dateTimestamp = when (dateValue) {
                        is com.google.firebase.Timestamp -> dateValue
                        is Long -> com.google.firebase.Timestamp(Date(dateValue))
                        else -> com.google.firebase.Timestamp.now() // Fallback
                    }

                    Session(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        date = dateTimestamp,
                        duration = doc.getLong("duration")?.toInt() ?: 60,
                        status = doc.getString("status") ?: "scheduled",
                        notes = doc.getString("notes") ?: "",
                        meetingLink = doc.getString("meetingLink") ?: ""
                    )
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var seminars by remember { mutableStateOf<List<Seminar>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val query = if (userProfile.role == "psychologist") {
            db.collection("seminars")
                .whereEqualTo("psychologistId", userProfile.uid)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
        } else {
            db.collection("seminars")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snapshot, _ ->
            seminars = snapshot?.documents?.mapNotNull { doc ->
                // FIX: Manual conversion for Seminar date field to handle old Long data gracefully
                val dateValue = doc.get("date")
                val dateTimestamp = when (dateValue) {
                    is com.google.firebase.Timestamp -> dateValue
                    is Long -> com.google.firebase.Timestamp(Date(dateValue))
                    else -> com.google.firebase.Timestamp.now() // Fallback
                }

                Seminar(
                    id = doc.id,
                    psychologistId = doc.getString("psychologistId") ?: "",
                    psychologistName = doc.getString("psychologistName") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    date = dateTimestamp,
                    duration = doc.getLong("duration")?.toInt() ?: 90,
                    maxParticipants = doc.getLong("maxParticipants")?.toInt() ?: 50,
                    registeredUsers = doc.get("registeredUsers") as? List<String> ?: emptyList(),
                    meetingLink = doc.getString("meetingLink") ?: ""
                )
            } ?: emptyList()
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
                            .format(seminar.date.toDate()),
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
    onCreate: (String, String, com.google.firebase.Timestamp, Int, Int) -> Unit
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
                        com.google.firebase.Timestamp.now(),
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
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val query = if (userProfile.role == "psychologist") {
            db.collection("resources")
                .whereEqualTo("psychologistId", userProfile.uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        } else {
            db.collection("resources")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snapshot, _ ->
            resources = snapshot?.documents?.mapNotNull { doc ->
                // FIX: Manual conversion for Resource timestamp field to handle old Long data gracefully
                val timestampValue = doc.get("timestamp")
                val timestamp = when (timestampValue) {
                    is com.google.firebase.Timestamp -> timestampValue
                    is Long -> com.google.firebase.Timestamp(Date(timestampValue))
                    else -> com.google.firebase.Timestamp.now()
                }

                Resource(
                    id = doc.id,
                    psychologistId = doc.getString("psychologistId") ?: "",
                    psychologistName = doc.getString("psychologistName") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    type = doc.getString("type") ?: "",
                    url = doc.getString("url") ?: "",
                    timestamp = timestamp
                )
            } ?: emptyList()
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
                    "timestamp" to com.google.firebase.Timestamp.now()
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCalendarScreen(userProfile: UserProfile) {
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var psychologists by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedPsychologist by remember { mutableStateOf<UserProfile?>(null) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var showPsychologistDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var clientNotes by remember { mutableStateOf("") }
    var existingSessions by remember { mutableStateOf<List<SessionRequest>>(emptyList()) }

    // Standard time slots
    val standardTimeSlots = listOf("8:30 AM", "10:00 AM", "1:00 PM", "3:00 PM")

    LaunchedEffect(Unit) {
        // Load all active psychologists
        db.collection("users")
            .whereEqualTo("role", "psychologist")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->
                psychologists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                } ?: emptyList()
            }

        // Load client's existing sessions to prevent double-booking
        db.collection("sessionRequests")
            .whereEqualTo("clientId", userProfile.uid)
            .addSnapshotListener { snapshot, _ ->
                existingSessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SessionRequest::class.java)
                } ?: emptyList()
            }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedPsychologist != null && selectedDate != null && selectedTime != null) {
                FloatingActionButton(
                    onClick = {
                        // Check if client already has a session on this date
                        val hasSessionOnDate = existingSessions.any { session ->
                            val sessionDate = Calendar.getInstance().apply {
                                time = session.requestedDate.toDate()
                            }
                            isSameDay(sessionDate, selectedDate!!)
                        }

                        if (hasSessionOnDate) {
                            // Show error - max 1 session per day
                            println("❌ Client already has a session booked on this date")
                            return@FloatingActionButton
                        }

                        // Create the actual timestamp with selected time
                        val finalDateTime = Calendar.getInstance().apply {
                            time = selectedDate!!.time
                            // Parse the selected time and set hours/minutes
                            val timeParts = selectedTime!!.split(":", " ")
                            var hour = timeParts[0].toInt()
                            val minute = timeParts[1].toInt()
                            val period = timeParts[2]

                            // Convert to 24-hour format
                            if (period == "PM" && hour < 12) hour += 12
                            if (period == "AM" && hour == 12) hour = 0

                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val sessionRequest = SessionRequest(
                            clientId = userProfile.uid,
                            clientName = userProfile.displayName,
                            psychologistId = selectedPsychologist!!.uid,
                            psychologistName = selectedPsychologist!!.displayName,
                            requestedDate = com.google.firebase.Timestamp(finalDateTime.time),
                            duration = 60,
                            status = "pending",
                            clientNotes = clientNotes,
                            psychologistResponse = "",
                            finalDate = null,
                            createdAt = com.google.firebase.Timestamp.now()
                        )

                        // REMOVE THE DUPLICATE LINE - ONLY CALL THIS ONCE
                        db.collection("sessionRequests").add(sessionRequest)
                            .addOnSuccessListener {
                                // Reset form
                                selectedPsychologist = null
                                selectedDate = null
                                selectedTime = null
                                clientNotes = ""
                                println("✅ Session request submitted successfully!")
                            }
                            .addOnFailureListener { e ->
                                println("❌ Failed to submit session request: ${e.message}")
                            }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Send, "Request Session")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Book a Session",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Find and book sessions with our psychologists",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Psychologist Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "1. Choose a Psychologist",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedPsychologist != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedPsychologist!!.photoUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedPsychologist!!.displayName)
                                    Text(
                                        selectedPsychologist!!.specializations.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = {
                                    selectedPsychologist = null
                                    selectedDate = null
                                    selectedTime = null
                                }) {
                                    Text("Change")
                                }
                            }
                        } else {
                            Button(
                                onClick = { showPsychologistDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select Psychologist")
                            }
                        }
                    }
                }
            }
// Date Selection (only show if psychologist is selected)
            if (selectedPsychologist != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "2. Choose Date & Time",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (selectedDate != null) {
                                Text(
                                    "Selected: ${SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!.time)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Check if client already has session on this date
                                val hasSessionOnDate = existingSessions.any { session ->
                                    val sessionDate = Calendar.getInstance().apply {
                                        time = session.requestedDate.toDate()
                                    }
                                    isSameDay(sessionDate, selectedDate!!)
                                }

                                if (hasSessionOnDate) {
                                    Text(
                                        "You already have a session booked on this date",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Maximum 1 session per day allowed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        "Available Time Slots:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Show standard time slots
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(standardTimeSlots) { timeSlot ->
                                            OutlinedButton(
                                                onClick = { selectedTime = timeSlot },
                                                modifier = Modifier.width(100.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (selectedTime == timeSlot)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        Color.Transparent
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selectedTime == timeSlot)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.outline
                                                )
                                            ) {
                                                Text(
                                                    timeSlot,
                                                    color = if (selectedTime == timeSlot)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showDateDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Change Date")
                                }
                            } else {
                                // THIS WAS MISSING - shows Select Date button when no date chosen
                                Button(
                                    onClick = { showDateDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Select Date")
                                }
                            }
                        }
                    }
                }
            }
            // Notes Section (only show if time is selected)
            if (selectedTime != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "3. Session Notes (Optional)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = clientNotes,
                                onValueChange = { clientNotes = it },
                                label = { Text("Any specific concerns or topics?") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ready to book! Tap the + button below to submit your request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Psychologist Selection Dialog
    if (showPsychologistDialog) {
        AlertDialog(
            onDismissRequest = { showPsychologistDialog = false },
            title = { Text("Choose a Psychologist") },
            text = {
                LazyColumn {
                    items(psychologists) { psychologist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedPsychologist = psychologist
                                    showPsychologistDialog = false
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(psychologist.photoUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(psychologist.displayName)
                                    Text(
                                        psychologist.specializations.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${psychologist.experience} years experience",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPsychologistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDateDialog) {
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("Select Date") },
            text = {
                Column {
                    Text("Select a date for your session:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Show next 14 days as options
                    val next14Days = List(14) { i ->
                        Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, i)
                        }
                    }

                    LazyColumn {
                        items(next14Days) { date ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedDate = date
                                        selectedTime = null // Reset time when date changes
                                        showDateDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDate?.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR))
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(date.time),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        SimpleDateFormat("yyyy", Locale.getDefault()).format(date.time),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper function
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}
@Composable
fun SessionRequestCard(request: SessionRequest, isClient: Boolean) {
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
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isClient) "with ${request.psychologistName}" else request.clientName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                            .format(request.requestedDate.toDate()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (request.clientNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            request.clientNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = when (request.status) {
                        "pending" -> Color(0xFFFFF3E0)
                        "approved" -> Color(0xFFE8F5E9)
                        "rejected" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        request.status.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (request.status) {
                            "pending" -> Color(0xFFF57C00)
                            "approved" -> Color(0xFF388E3C)
                            "rejected" -> Color(0xFFD32F2F)
                            else -> Color(0xFF757575)
                        }
                    )
                }
            }
        }
    }
}


// ADMIN SCREENS (Complete)
// Admin utility - Manual User Profile Creator
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualUserCreator(
    db: FirebaseFirestore,
    onUserCreated: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var uid by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("client") }
    var status by remember { mutableStateOf("active") }
    var authProvider by remember { mutableStateOf("email") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Add, "Create User Profile")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Manually Create User Profile")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isCreating) showDialog = false
            },
            title = { Text("Create User Profile") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Get UIDs from Firebase Auth Console",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uid,
                            onValueChange = {
                                uid = it
                                errorMessage = null
                            },
                            label = { Text("User UID *") },
                            placeholder = { Text("From Authentication tab") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                errorMessage = null
                            },
                            label = { Text("Display Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        ExposedDropdownMenuBox(
                            expanded = roleExpanded,
                            onExpandedChange = { roleExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = role.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false }
                            ) {
                                listOf("client", "psychologist", "admin").forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            role = r
                                            roleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = status.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                listOf("active", "pending", "rejected").forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            status = s
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = authProvider.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Auth Provider") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                listOf("email", "google").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            authProvider = p
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Text(
                                errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            uid.isBlank() -> errorMessage = "UID is required"
                            email.isBlank() -> errorMessage = "Email is required"
                            displayName.isBlank() -> errorMessage = "Display name is required"
                            else -> {
                                isCreating = true
                                errorMessage = null

                                val newProfile = UserProfile(
                                    uid = uid.trim(),
                                    email = email.trim(),
                                    displayName = displayName.trim(),
                                    photoUrl = "",
                                    bio = "",
                                    role = role,
                                    status = status,
                                    specializations = emptyList(),
                                    experience = "",
                                    licenseNumber = "",
                                    certificateUrls = emptyList(),
                                    createdAt = com.google.firebase.Timestamp.now(),
                                    authProvider = authProvider
                                )

                                db.collection("users").document(uid.trim())
                                    .set(newProfile)
                                    .addOnSuccessListener {
                                        isCreating = false
                                        showDialog = false
                                        uid = ""
                                        email = ""
                                        displayName = ""
                                        role = "client"
                                        status = "active"
                                        authProvider = "email"
                                        onUserCreated()
                                    }
                                    .addOnFailureListener { e ->
                                        isCreating = false
                                        errorMessage = "Failed: ${e.localizedMessage}"
                                    }
                            }
                        }
                    },
                    enabled = !isCreating
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    enabled = !isCreating
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminDashboardScreen() {
    val db = FirebaseFirestore.getInstance("mindspacedb")
    var totalUsers by remember { mutableStateOf(0) }
    var totalClients by remember { mutableStateOf(0) }
    var activePsychologists by remember { mutableStateOf(0) }
    var pendingApprovals by remember { mutableStateOf(0) }
    var rejectedPsychologists by remember { mutableStateOf(0) }
    var totalSessions by remember { mutableStateOf(0) }
    var totalResources by remember { mutableStateOf(0) }
    var totalSeminars by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Total users - Real-time listener
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            totalUsers = snapshot?.size() ?: 0
            isLoading = false
        }

        // Total clients
        db.collection("users")
            .whereEqualTo("role", "client")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                totalClients = snapshot?.size() ?: 0
            }

        // Active psychologists
        db.collection("users")
            .whereEqualTo("role", "psychologist")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                activePsychologists = snapshot?.size() ?: 0
            }

        // Pending approvals
        db.collection("users")
            .whereEqualTo("role", "psychologist")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                pendingApprovals = snapshot?.size() ?: 0
            }

        // Rejected psychologists
        db.collection("users")
            .whereEqualTo("role", "psychologist")
            .whereEqualTo("status", "rejected")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                rejectedPsychologists = snapshot?.size() ?: 0
            }

        // Total sessions
        db.collection("sessions").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            totalSessions = snapshot?.size() ?: 0
        }

        // Total resources
        db.collection("resources").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            totalResources = snapshot?.size() ?: 0
        }

        // Total seminars
        db.collection("seminars").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            totalSeminars = snapshot?.size() ?: 0
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Admin Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Platform Overview & Statistics",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pending Approvals Alert (if any)
            if (pendingApprovals > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFF57C00)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "⚠️ Action Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = Color(0xFFF57C00)
                                )
                                Text(
                                    "You have $pendingApprovals psychologist${if (pendingApprovals > 1) "s" else ""} waiting for approval",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFF57C00)
                            )
                        }
                    }
                }
            }

            // User Statistics Section
            item {
                Text(
                    "User Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailedStatsCard(
                        title = "Total Users",
                        value = totalUsers.toString(),
                        icon = Icons.Default.Person,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.weight(1f)
                    )
                    DetailedStatsCard(
                        title = "Clients",
                        value = totalClients.toString(),
                        icon = Icons.Default.Face,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Psychologist Statistics Section
            item {
                Text(
                    "Psychologist Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailedStatsCard(
                        title = "Active",
                        value = activePsychologists.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    DetailedStatsCard(
                        title = "Pending",
                        value = pendingApprovals.toString(),
                        icon = Icons.Default.Info,
                        color = Color(0xFFFFA726),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailedStatsCard(
                        title = "Rejected",
                        value = rejectedPsychologists.toString(),
                        icon = Icons.Default.Close,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                    DetailedStatsCard(
                        title = "Total Psychologists",
                        value = (activePsychologists + pendingApprovals + rejectedPsychologists).toString(),
                        icon = Icons.Default.Favorite,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Platform Activity Section
            item {
                Text(
                    "Platform Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailedStatsCard(
                        title = "Sessions",
                        value = totalSessions.toString(),
                        icon = Icons.Default.CalendarToday,
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.weight(1f)
                    )
                    DetailedStatsCard(
                        title = "Resources",
                        value = totalResources.toString(),
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                DetailedStatsCard(
                    title = "Seminars",
                    value = totalSeminars.toString(),
                    icon = Icons.Default.DateRange,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        QuickActionItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Review pending psychologist applications",
                            isHighlight = pendingApprovals > 0
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        QuickActionItem(
                            icon = Icons.Default.Person,
                            text = "View all user accounts and activity"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        QuickActionItem(
                            icon = Icons.Default.CalendarToday,
                            text = "Monitor sessions and platform usage"
                        )
                    }
                }
            }


            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}