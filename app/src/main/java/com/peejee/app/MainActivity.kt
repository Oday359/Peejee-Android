package com.peejee.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PeejeeTheme {
                PeejeeApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updatePeejeePresence(true)
    }

    override fun onStop() {
        updatePeejeePresence(false)
        super.onStop()
    }

    private fun updatePeejeePresence(isOnline: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val updates = hashMapOf<String, Any>(
            "isOnline" to isOnline,
            "lastSeen" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
    }
}

@Composable
fun PeejeeLogo(
    modifier: Modifier = Modifier,
    size: Int = 70
) {
    Image(
        painter = painterResource(R.drawable.peejee_app_icon_512),
        contentDescription = "Peejee",
        modifier = modifier.size(size.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DefaultProfileIcon(size: Int = 65) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👤",
            fontSize = (size * 0.55f).sp
        )
    }
}

fun bitmapToBase64(bitmap: Bitmap): String {
    return try {
        val maxSize = 400
        val width = bitmap.width
        val height = bitmap.height

        val scale = minOf(
            1f,
            maxSize.toFloat() / maxOf(width, height)
        )

        val resizedBitmap =
            if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

        val outputStream = ByteArrayOutputStream()

        resizedBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            70,
            outputStream
        )

        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }

        Base64.encodeToString(
            outputStream.toByteArray(),
            Base64.NO_WRAP
        )
    } catch (exception: Exception) {
        ""
    }
}

fun base64ToBitmap(base64: String): Bitmap? {
    return try {
        if (base64.isBlank()) {
            null
        } else {
            val imageBytes = Base64.decode(
                base64,
                Base64.DEFAULT
            )

            BitmapFactory.decodeByteArray(
                imageBytes,
                0,
                imageBytes.size
            )
        }
    } catch (exception: Exception) {
        null
    }
}

fun formatPostTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    return try {
        SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date(timestamp))
    } catch (exception: Exception) {
        ""
    }
}

fun getLikedBy(
    document: com.google.firebase.firestore.DocumentSnapshot
): Map<String, Boolean> {

    val raw = document.get("likedBy")

    return if (raw is Map<*, *>) {

        raw.entries.mapNotNull { entry ->

            val key = entry.key as? String
            val value = entry.value as? Boolean

            if (key != null && value != null) {
                key to value
            } else {
                null
            }
        }.toMap()

    } else {
        emptyMap()
    }
}

data class PeejeePost(
    val id: String,
    val userId: String,
    val userName: String,
    val text: String,
    val timestamp: Long,
    val mediaUrl: String = "",
    val mediaType: String = "",
    val likeCount: Int = 0,
    val likedBy: Map<String, Boolean> = emptyMap(),
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val sharedFromPostId: String = "",
    val sharedFromUserName: String = "",
    val sharedFromText: String = ""
)

data class PeejeeComment(
    val id: String,
    val userId: String,
    val userName: String,
    val text: String,
    val timestamp: Long
)

data class PeejeePerson(
    val uid: String,
    val name: String,
    val email: String = "",
    val isOnline: Boolean = false
)

data class PeejeeMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val read: Boolean = false
)

fun createPeejeeNotification(
    firestore: FirebaseFirestore,
    recipientUserId: String,
    type: String,
    actorId: String,
    actorName: String,
    text: String,
    postId: String = "",
    messageId: String = ""
) {
    if (
        recipientUserId.isBlank() ||
        actorId.isBlank() ||
        recipientUserId == actorId
    ) return

    val data = hashMapOf<String, Any>(
        "type" to type,
        "actorId" to actorId,
        "actorName" to actorName,
        "text" to text,
        "postId" to postId,
        "messageId" to messageId,
        "timestamp" to System.currentTimeMillis(),
        "read" to false
    )

    firestore
        .collection("users")
        .document(recipientUserId)
        .collection("notifications")
        .add(data)
}

fun findMentionedUsers(
    firestore: FirebaseFirestore,
    text: String,
    currentUserId: String,
    onFound: (List<PeejeePerson>) -> Unit
) {
    val mentions = Regex("@([A-Za-z0-9_]+)")
        .findAll(text)
        .map { it.groupValues[1].lowercase() }
        .toSet()

    if (mentions.isEmpty()) {
        onFound(emptyList())
        return
    }

    firestore
        .collection("users")
        .limit(200)
        .get()
        .addOnSuccessListener { snapshot ->
            val people = snapshot.documents.mapNotNull { document ->
                val uid = document.id
                if (uid == currentUserId) return@mapNotNull null

                val name = document.getString("name") ?: return@mapNotNull null
                val normalized = name.replace(" ", "").lowercase()

                if (mentions.contains(normalized)) {
                    PeejeePerson(uid = uid, name = name)
                } else {
                    null
                }
            }
            onFound(people)
        }
        .addOnFailureListener {
            onFound(emptyList())
        }
}

@Composable
fun PeejeeApp() {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var screen by remember {
        mutableStateOf(
            if (auth.currentUser != null) {
                "home"
            } else {
                "welcome"
            }
        )
    }

    var userName by remember {
        mutableStateOf("Peejee User")
    }

    LaunchedEffect(Unit) {

        val currentUser = auth.currentUser

        if (currentUser != null) {

            firestore
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->

                    userName =
                        document.getString("name")
                            ?: currentUser.displayName
                            ?: "Peejee User"

                    screen = "home"
                }
                .addOnFailureListener {

                    userName =
                        currentUser.displayName
                            ?: "Peejee User"

                    screen = "home"
                }
        }
    }

    when (screen) {

        "welcome" -> WelcomeScreen(
            onCreateAccount = {
                screen = "signup"
            },
            onLogin = {
                screen = "login"
            }
        )

        "signup" -> SignUpScreen(
            onAccountCreated = { name ->
                userName = name
                screen = "home"
            },
            onBack = {
                screen = "welcome"
            }
        )

        "login" -> LoginScreen(
            onLoginSuccess = { name ->
                userName = name
                screen = "home"
            },
            onBack = {
                screen = "welcome"
            }
        )

        "home" -> HomeScreen(
            name = userName,
            onProfileNameChanged = {
                userName = it
            },
            onLoggedOut = {
                userName = "Peejee User"
                screen = "welcome"
            }
        )
    }
}

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        PeejeeLogo(size = 130)

        Spacer(Modifier.height(12.dp))

        Text(
            "Connect. Chat. Share.",
            fontSize = 18.sp
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log In")
        }
    }
}

@Composable
fun SignUpScreen(
    onAccountCreated: (String) -> Unit,
    onBack: () -> Unit
) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        PeejeeLogo(size = 90)

        Spacer(Modifier.height(10.dp))

        Text(
            "Create Account",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {

            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {

                errorMessage = ""

                when {

                    name.isBlank() ->
                        errorMessage =
                            "Please enter your full name."

                    email.isBlank() ->
                        errorMessage =
                            "Please enter your email."

                    password.length < 6 ->
                        errorMessage =
                            "Password must be at least 6 characters."

                    password != confirmPassword ->
                        errorMessage =
                            "Passwords do not match."

                    else -> {

                        loading = true

                        auth.createUserWithEmailAndPassword(
                            email.trim(),
                            password
                        ).addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                val user = auth.currentUser

                                if (user != null) {

                                    val profile =
                                        hashMapOf<String, Any>(
                                            "uid" to user.uid,
                                            "name" to name.trim(),
                                            "email" to email.trim(),
                                            "bio" to "",
                                            "profilePhoto" to "",
                                            "followersCount" to 0,
                                            "followingCount" to 0,
                                            "updatedAt" to
                                                System.currentTimeMillis()
                                        )

                                    firestore
                                        .collection("users")
                                        .document(user.uid)
                                        .set(profile)
                                        .addOnSuccessListener {

                                            loading = false

                                            onAccountCreated(
                                                name.trim()
                                            )
                                        }
                                        .addOnFailureListener { exception ->

                                            loading = false

                                            errorMessage =
                                                exception.message
                                                    ?: "Could not save your profile."
                                        }

                                } else {

                                    loading = false

                                    errorMessage =
                                        "Account creation failed."
                                }

                            } else {

                                loading = false

                                errorMessage =
                                    task.exception?.message
                                        ?: "Could not create account."
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (loading) {
                    "Creating Account..."
                } else {
                    "Create Account"
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onBack: () -> Unit
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        PeejeeLogo(size = 90)

        Spacer(Modifier.height(10.dp))

        Text(
            "Log In",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {

            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {

                errorMessage = ""

                when {

                    email.isBlank() ->
                        errorMessage =
                            "Please enter your email."

                    password.isBlank() ->
                        errorMessage =
                            "Please enter your password."

                    else -> {

                        loading = true

                        auth.signInWithEmailAndPassword(
                            email.trim(),
                            password
                        ).addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                val user = auth.currentUser

                                if (user != null) {

                                    firestore
                                        .collection("users")
                                        .document(user.uid)
                                        .get()
                                        .addOnSuccessListener { document ->

                                            loading = false

                                            onLoginSuccess(
                                                document.getString("name")
                                                    ?: "Peejee User"
                                            )
                                        }
                                        .addOnFailureListener {

                                            loading = false
                                            onLoginSuccess("Peejee User")
                                        }

                                } else {

                                    loading = false
                                    errorMessage = "Login failed."
                                }

                            } else {

                                loading = false

                                errorMessage =
                                    task.exception?.message
                                        ?: "Could not log in."
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (loading) "Logging In..." else "Log In"
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    name: String,
    onProfileNameChanged: (String) -> Unit,
    onLoggedOut: () -> Unit
) {

    var selectedTab by remember { mutableStateOf(0) }

    var posts by remember {
        mutableStateOf<List<PeejeePost>>(emptyList())
    }

    var followedUserIds by remember {
        mutableStateOf(setOf<String>())
    }

    var processingFollowUserId by remember {
        mutableStateOf<String?>(null)
    }

    var loadingPosts by remember {
        mutableStateOf(true)
    }

    var refreshingPosts by remember {
        mutableStateOf(false)
    }

    var selectedChatUser by remember {
        mutableStateOf<PeejeePerson?>(null)
    }

    var selectedProfileUser by remember {
        mutableStateOf<PeejeePerson?>(null)
    }

    var showActivity by remember { mutableStateOf(false) }
    var unreadNotificationCount by remember { mutableStateOf(0) }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId =
        auth.currentUser?.uid ?: ""

    val context = LocalContext.current

    var selectedCommentPostId by remember { mutableStateOf("") }
    var showShareDialog by remember { mutableStateOf(false) }
    var sharePost by remember { mutableStateOf<PeejeePost?>(null) }
    var shareUsers by remember { mutableStateOf<List<PeejeePerson>>(emptyList()) }
    var loadingShareUsers by remember { mutableStateOf(false) }

    fun openShareDialog(post: PeejeePost) {
        sharePost = post
        showShareDialog = true
        loadingShareUsers = true
        shareUsers = emptyList()

        if (currentUserId.isBlank()) {
            loadingShareUsers = false
            return
        }

        firestore.collection("users")
            .limit(100)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snapshot ->
                shareUsers = snapshot.documents.mapNotNull { document ->
                    val uid = document.id
                    if (uid.isBlank() || uid == currentUserId) return@mapNotNull null
                    PeejeePerson(
                        uid = uid,
                        name = document.getString("name")
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Peejee User"
                    )
                }.sortedBy { it.name.lowercase(Locale.getDefault()) }
                loadingShareUsers = false
            }
            .addOnFailureListener {
                loadingShareUsers = false
            }
    }

    fun incrementShareCount(postId: String) {
        if (postId.isBlank()) return
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentShares = snapshot.getLong("shareCount")?.toInt() ?: 0
            transaction.update(postRef, "shareCount", currentShares + 1)
            null
        }
    }

    fun shareToSocialMedia(post: PeejeePost) {
        val shareText = buildString {
            append("Peejee post by ${post.userName}")
            if (post.text.isNotBlank()) {
                append(":\n")
                append(post.text)
            }
            append("\n\nShared from Peejee")
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Share with"))
            incrementShareCount(post.id)
        } catch (_: Exception) {
        }
    }

    fun repostToPeejee(post: PeejeePost) {
        if (currentUserId.isBlank() || post.id.isBlank()) return

        val postRef = firestore.collection("posts").document(post.id)
        val newPostRef = firestore.collection("posts").document()

        firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDocument ->
                val currentName = userDocument.getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: name

                firestore.runTransaction { transaction ->
                    val originalSnapshot = transaction.get(postRef)
                    val currentShares =
                        originalSnapshot.getLong("shareCount")?.toInt() ?: 0

                    val repostData = hashMapOf<String, Any>(
                        "postId" to newPostRef.id,
                        "userId" to currentUserId,
                        "userName" to currentName,
                        "text" to post.text,
                        "likes" to 0,
                        "likedBy" to emptyMap<String, Boolean>(),
                        "commentCount" to 0,
                        "shareCount" to 0,
                        "timestamp" to System.currentTimeMillis(),
                        "sharedFromPostId" to post.id,
                        "sharedFromUserName" to post.userName,
                        "sharedFromText" to post.text
                    )

                    if (post.mediaUrl.isNotBlank()) {
                        repostData["mediaUrl"] = post.mediaUrl
                        repostData["mediaType"] = post.mediaType
                    }

                    transaction.set(newPostRef, repostData)
                    transaction.update(postRef, "shareCount", currentShares + 1)
                    null
                }.addOnSuccessListener {
                    showShareDialog = false
                    sharePost = null
                }
            }
    }

    fun shareToPeejeeUser(post: PeejeePost, person: PeejeePerson) {
        if (person.uid.isBlank() || currentUserId.isBlank() || person.uid == currentUserId) return

        val messageRef = firestore.collection("messages").document()
        val text = buildString {
            append("I shared a Peejee post by ${post.userName} with you")
            if (post.text.isNotBlank()) {
                append(":\n")
                append(post.text)
            }
        }

        val data = hashMapOf<String, Any>(
            "messageId" to messageRef.id,
            "senderId" to currentUserId,
            "receiverId" to person.uid,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "read" to false,
            "sharedPostId" to post.id
        )

        messageRef.set(data)
            .addOnSuccessListener {
                incrementShareCount(post.id)
                createPeejeeNotification(
                    firestore = firestore,
                    recipientUserId = person.uid,
                    type = "share",
                    actorId = currentUserId,
                    actorName = name,
                    text = "shared a post with you",
                    postId = post.id,
                    messageId = messageRef.id
                )
                showShareDialog = false
                sharePost = null
            }
    }

    fun refreshHomePosts() {
        if (refreshingPosts) return

        refreshingPosts = true

        firestore
            .collection("posts")
            .orderBy(
                "timestamp",
                Query.Direction.DESCENDING
            )
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snapshot ->
                posts = snapshot.documents.mapNotNull { document ->
                    PeejeePost(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        userName = document.getString("userName") ?: "Peejee User",
                        text = document.getString("text") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0L,
                        mediaUrl = document.getString("mediaUrl") ?: "",
                        mediaType = document.getString("mediaType") ?: "",
                        likeCount = document.getLong("likes")?.toInt() ?: 0,
                        likedBy = getLikedBy(document),
                        commentCount = document.getLong("commentCount")?.toInt() ?: 0,
                        shareCount = document.getLong("shareCount")?.toInt() ?: 0,
                        sharedFromPostId = document.getString("sharedFromPostId") ?: "",
                        sharedFromUserName = document.getString("sharedFromUserName") ?: "",
                        sharedFromText = document.getString("sharedFromText") ?: ""
                    )
                }
                loadingPosts = false
                refreshingPosts = false
            }
            .addOnFailureListener {
                refreshingPosts = false
                loadingPosts = false
            }
    }

    DisposableEffect(currentUserId) {

        if (currentUserId.isBlank()) {
            unreadNotificationCount = 0
            onDispose { }
        } else {
            val registration =
                firestore
                    .collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .whereEqualTo("read", false)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null) {
                            unreadNotificationCount = snapshot?.size() ?: 0
                        }
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    DisposableEffect(Unit) {

        val registration =
            firestore
                .collection("posts")
                .orderBy(
                    "timestamp",
                    Query.Direction.DESCENDING
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        loadingPosts = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        posts =
                            snapshot.documents.mapNotNull { document ->

                                PeejeePost(
                                    id = document.id,
                                    userId =
                                        document.getString("userId")
                                            ?: "",
                                    userName =
                                        document.getString("userName")
                                            ?: "Peejee User",
                                    text =
                                        document.getString("text")
                                            ?: "",
                                    timestamp =
                                        document.getLong("timestamp")
                                            ?: 0L,
                                    mediaUrl =
                                        document.getString("mediaUrl")
                                            ?: "",
                                    mediaType =
                                        document.getString("mediaType")
                                            ?: "",
                                    likeCount =
                                        document.getLong("likes")
                                            ?.toInt()
                                            ?: 0,
                                    likedBy =
                                        getLikedBy(document),
                                    commentCount =
                                        document.getLong("commentCount")
                                            ?.toInt()
                                            ?: 0,
                                    shareCount =
                                        document.getLong("shareCount")
                                            ?.toInt()
                                            ?: 0,
                                    sharedFromPostId =
                                        document.getString(
                                            "sharedFromPostId"
                                        ) ?: "",
                                    sharedFromUserName =
                                        document.getString(
                                            "sharedFromUserName"
                                        ) ?: "",
                                    sharedFromText =
                                        document.getString(
                                            "sharedFromText"
                                        ) ?: ""
                                )
                            }
                    }

                    loadingPosts = false
                }

        onDispose {
            registration.remove()
        }
    }

    DisposableEffect(currentUserId) {

        if (currentUserId.isBlank()) {

            followedUserIds = emptySet()

            onDispose { }

        } else {

            val registration =
                firestore
                    .collection("users")
                    .document(currentUserId)
                    .collection("following")
                    .addSnapshotListener { snapshot, error ->

                        if (
                            error == null &&
                            snapshot != null
                        ) {

                            followedUserIds =
                                snapshot.documents
                                    .map { it.id }
                                    .toSet()
                        }
                    }

            onDispose {
                registration.remove()
            }
        }
    }


    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    PeejeeLogo(size = 48)
                },
                actions = {
                    TextButton(
                        onClick = {
                            showActivity = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔔", fontSize = 24.sp)

                            if (unreadNotificationCount > 0) {
                                Spacer(Modifier.width(4.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        if (unreadNotificationCount > 99) {
                                            "99+"
                                        } else {
                                            unreadNotificationCount.toString()
                                        },
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 5.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedChatUser = null
                    },
                    icon = { Text("🏠") },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedChatUser = null
                    },
                    icon = { Text("🧭") },
                    label = { Text("Discover") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        selectedChatUser = null
                    },
                    icon = { Text("➕") },
                    label = { Text("Post") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                    },
                    icon = { Text("💬") },
                    label = {
                        Text(
                            "Messages",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 10.sp
                        )
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        selectedChatUser = null
                    },
                    icon = { Text("👤") },
                    label = { Text("Profile") }
                )
            }
        }

    ) { paddingValues ->

        if (showActivity) {
            ActivityPage(
                paddingValues = paddingValues,
                onBack = {
                    showActivity = false
                }
            )
        } else if (
            selectedTab == 3 &&
            selectedChatUser != null
        ) {

            PrivateChatPageNew(
                person = selectedChatUser!!,
                paddingValues = paddingValues,
                onBack = {
                    selectedChatUser = null
                }
            )

        } else if (selectedProfileUser != null) {

            OtherUserProfilePage(
                person = selectedProfileUser!!,
                paddingValues = paddingValues,
                onBack = {
                    selectedProfileUser = null
                },
                onMessage = { person ->
                    selectedProfileUser = null
                    selectedChatUser = person
                    selectedTab = 3
                }
            )

        } else {

            when (selectedTab) {

                0 -> HomeFeed(
                    name = name,
                    currentUserId = currentUserId,
                    posts = posts,
                    followedUserIds = followedUserIds,
                    loadingPosts = loadingPosts,
                    processingFollowUserId = processingFollowUserId,
                    isRefreshing = refreshingPosts,
                    onRefresh = { refreshHomePosts() },

                    onLike = { postId ->

                        val userId =
                            auth.currentUser?.uid

                        if (userId != null) {

                            val postRef =
                                firestore
                                    .collection("posts")
                                    .document(postId)

                            var postOwnerId = ""
                            var postOwnerName = "Peejee User"
                            var wasAlreadyLiked = false

                            firestore.runTransaction { transaction ->

                                val snapshot =
                                    transaction.get(postRef)

                                val currentLikes =
                                    snapshot.getLong("likes")
                                        ?.toInt()
                                        ?: 0

                                val currentLikedBy =
                                    getLikedBy(snapshot).toMutableMap()

                                val alreadyLiked =
                                    currentLikedBy[userId] == true

                                wasAlreadyLiked = alreadyLiked
                                postOwnerId =
                                    snapshot.getString("userId") ?: ""
                                postOwnerName =
                                    snapshot.getString("userName")
                                        ?: "Peejee User"

                                if (alreadyLiked) {

                                    currentLikedBy.remove(userId)

                                    transaction.update(
                                        postRef,
                                        "likes",
                                        (currentLikes - 1)
                                            .coerceAtLeast(0)
                                    )

                                } else {

                                    currentLikedBy[userId] = true

                                    transaction.update(
                                        postRef,
                                        "likes",
                                        currentLikes + 1
                                    )
                                }

                                transaction.update(
                                    postRef,
                                    "likedBy",
                                    currentLikedBy
                                )

                                null
                            }.addOnSuccessListener {
                                if (!wasAlreadyLiked) {
                                    createPeejeeNotification(
                                        firestore = firestore,
                                        recipientUserId = postOwnerId,
                                        type = "like",
                                        actorId = userId,
                                        actorName = name,
                                        text = "liked your post",
                                        postId = postId
                                    )
                                }
                            }
                        }
                    },

                    onFollow = { targetUserId ->

                    val currentUser = auth.currentUser

                    if (
                        currentUser == null ||
                        targetUserId.isBlank() ||
                        targetUserId == currentUser.uid ||
                        processingFollowUserId == targetUserId
                    ) {
                        return@HomeFeed
                    }

                    processingFollowUserId = targetUserId

                    val currentUserRef =
                        firestore.collection("users").document(currentUser.uid)
                    val targetUserRef =
                        firestore.collection("users").document(targetUserId)
                    val followingRef =
                        currentUserRef.collection("following").document(targetUserId)
                    val followerRef =
                        targetUserRef.collection("followers").document(currentUser.uid)

                    followingRef.get(com.google.firebase.firestore.Source.SERVER)
                        .addOnSuccessListener { followingSnapshot ->

                            if (followingSnapshot.exists()) {
                                val batch = firestore.batch()
                                batch.delete(followingRef)
                                batch.delete(followerRef)

                                batch.commit()
                                    .addOnSuccessListener {
                                        followedUserIds = followedUserIds - targetUserId
                                        processingFollowUserId = null

                                        targetUserRef.set(
                                            mapOf(
                                                "followersCount" to FieldValue.increment(-1)
                                            ),
                                            SetOptions.merge()
                                        )
                                        currentUserRef.set(
                                            mapOf(
                                                "followingCount" to FieldValue.increment(-1)
                                            ),
                                            SetOptions.merge()
                                        )
                                    }
                                    .addOnFailureListener {
                                        processingFollowUserId = null
                                    }
                            } else {
                                targetUserRef.get(com.google.firebase.firestore.Source.SERVER)
                                    .addOnSuccessListener { targetSnapshot ->

                                        val currentUserName =
                                            name.trim().takeIf { it.isNotBlank() }
                                                ?: "Peejee User"
                                        val targetUserName =
                                            targetSnapshot.getString("name")
                                                ?.trim()
                                                ?.takeIf { it.isNotBlank() }
                                                ?: "Peejee User"
                                        val now = System.currentTimeMillis()

                                        val batch = firestore.batch()

                                        batch.set(
                                            followingRef,
                                            hashMapOf<String, Any>(
                                                "uid" to targetUserId,
                                                "name" to targetUserName,
                                                "timestamp" to now
                                            )
                                        )

                                        batch.set(
                                            followerRef,
                                            hashMapOf<String, Any>(
                                                "uid" to currentUser.uid,
                                                "name" to currentUserName,
                                                "timestamp" to now
                                            )
                                        )

                                        batch.commit()
                                            .addOnSuccessListener {
                                                followedUserIds = followedUserIds + targetUserId
                                                processingFollowUserId = null

                                                currentUserRef.set(
                                                    mapOf(
                                                        "followingCount" to FieldValue.increment(1)
                                                    ),
                                                    SetOptions.merge()
                                                )
                                                targetUserRef.set(
                                                    mapOf(
                                                        "followersCount" to FieldValue.increment(1)
                                                    ),
                                                    SetOptions.merge()
                                                )

                                                createPeejeeNotification(
                                                    firestore = firestore,
                                                    recipientUserId = targetUserId,
                                                    type = "follow",
                                                    actorId = currentUser.uid,
                                                    actorName = name,
                                                    text = "started following you"
                                                )
                                            }
                                            .addOnFailureListener {
                                                processingFollowUserId = null
                                            }
                                    }
                                    .addOnFailureListener {
                                        processingFollowUserId = null
                                    }
                            }
                        }
                        .addOnFailureListener {
                            processingFollowUserId = null
                        }
                },

                    onComment = { postId ->
                    selectedCommentPostId = postId
                },

                    onShare = { post ->
                        openShareDialog(post)
                    },

                    onProfile = { person ->
                        selectedProfileUser = person
                    },

                    paddingValues = paddingValues
                )

                1 -> PeejeeDiscoverPage(
                    onUserClick = { userId ->
                        firestore
                            .collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                selectedProfileUser = PeejeePerson(
                                    uid = document.id,
                                    name = document.getString("name")
                                        ?: "Peejee User",
                                    email = document.getString("email")
                                        ?: "",
                                    isOnline = document.getBoolean("isOnline")
                                        ?: false
                                )
                            }
                    },
                    onPostClick = { postId ->
                        selectedPostId = postId
                        selectedCommentPostId = postId
                    }
                )

                2 -> CreatePostPage(
                    onPostCreated = {
                        selectedTab = 0
                    },
                    paddingValues = paddingValues
                )

                3 -> MessagesPage(
                    onMessage = { person ->
                        selectedChatUser = person
                    },
                    paddingValues = paddingValues
                )

                4 -> ProfilePage(
                    name = name,
                    paddingValues = paddingValues,
                    onProfileNameChanged = onProfileNameChanged,
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }

    if (selectedCommentPostId.isNotBlank()) {
        PeejeeCommentsDialog(
            postId = selectedCommentPostId,
            onDismiss = {
                selectedCommentPostId = ""
            },
            onCountChanged = { }
        )
    }

    if (showShareDialog && sharePost != null) {
        val postToShare = sharePost!!

        AlertDialog(
            onDismissRequest = {
                showShareDialog = false
                sharePost = null
            },
            title = { Text("Share post") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Share with a Peejee user",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    if (loadingShareUsers) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (shareUsers.isEmpty()) {
                        Text("You are not following any Peejee users yet.")
                    } else {
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            itemsIndexed(shareUsers) { _, person ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            shareToPeejeeUser(postToShare, person)
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DefaultProfileIcon(size = 42)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        person.name,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("Share")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            repostToPeejee(postToShare)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔁 Repost to Peejee")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            shareToSocialMedia(postToShare)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↗️ Share to other social media")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        sharePost = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun HomeFeed(
    name: String,
    currentUserId: String,
    posts: List<PeejeePost>,
    followedUserIds: Set<String>,
    loadingPosts: Boolean,
    processingFollowUserId: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLike: (String) -> Unit,
    onFollow: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (PeejeePost) -> Unit,
    onProfile: (PeejeePerson) -> Unit,
    paddingValues: PaddingValues
) {

    val liveUsers = listOf(
        "Live",
        "David",
        "Sarah",
        "Mike",
        "Blessing"
    )

    var pullDistance by remember { mutableFloatStateOf(0f) }
    val pullThreshold = 90f
    val latestOnRefresh by rememberUpdatedState(onRefresh)

    val listState = rememberLazyListState()

    val refreshConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (
                    available.y > 0f &&
                    !isRefreshing &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                ) {
                    pullDistance =
                        (pullDistance + available.y).coerceAtMost(pullThreshold)

                    if (pullDistance >= pullThreshold) {
                        pullDistance = 0f
                        latestOnRefresh()
                    }
                } else if (available.y < 0f) {
                    pullDistance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(refreshConnection)
    ) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            item {

                Spacer(Modifier.height(16.dp))

                Text(
                    "Welcome, $name 👋",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "🔴 Live Now",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding =
                        PaddingValues(horizontal = 16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    itemsIndexed(liveUsers) { _, liveUser ->

                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .height(165.dp)
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape),
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    if (liveUser == "Live") {
                                        PeejeeLogo(size = 60)
                                    } else {
                                        DefaultProfileIcon(size = 60)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    if (liveUser == "Live") {
                                        "Peejee Live"
                                    } else {
                                        liveUser
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(6.dp))

                                Surface(
                                    shape =
                                        MaterialTheme.shapes.small
                                ) {

                                    Text(
                                        "🔴 LIVE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    "👁 0 viewers",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (loadingPosts) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                if (!loadingPosts && posts.isEmpty()) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            PeejeeLogo(size = 75)

                            Spacer(Modifier.height(8.dp))

                            Text("No posts yet")
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }

            itemsIndexed(
                posts,
                key = { _, post -> post.id }
            ) { _, post ->

                TikTokStylePost(
                    post = post,
                    likeCount = post.likeCount,
                    isLiked =
                        post.likedBy[currentUserId] == true,
                    isFollowing =
                        followedUserIds.contains(post.userId),
                    currentUserId = currentUserId,
                    commentCount = post.commentCount,
                    shareCount = post.shareCount,
                    isFollowProcessing = processingFollowUserId == post.userId,
                    onLike = {
                        onLike(post.id)
                    },
                    onFollow = {
                        onFollow(post.userId)
                    },
                    onComment = {
                        onComment(post.id)
                    },
                    onShare = {
                        onShare(post)
                    },
                    onProfile = {
                        onProfile(PeejeePerson(uid = post.userId, name = post.userName))
                    }
                )
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }

        if (pullDistance > 0f || isRefreshing) {
            val progress =
                (pullDistance / pullThreshold).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PeejeeLogo(size = 42)
                Text(
                    if (isRefreshing) "Refreshing..."
                    else if (progress >= 1f) "Release to refresh"
                    else "Pull to refresh",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TikTokStylePost(
    post: PeejeePost,
    likeCount: Int,
    isLiked: Boolean,
    isFollowing: Boolean,
    isFollowProcessing: Boolean,
    currentUserId: String,
    commentCount: Int,
    shareCount: Int,
    onLike: () -> Unit,
    onFollow: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onProfile: () -> Unit
) {

    var showPostMenu by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(18.dp)
    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                if (post.sharedFromPostId.isNotBlank()) {

                    Text(
                        "🔁 ${post.userName} shared a post",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            start = 14.dp,
                            top = 12.dp,
                            end = 14.dp
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 14.dp,
                            top = 14.dp,
                            end = 14.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    DefaultProfileIcon(size = 48)

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onProfile() }
                    ) {

                        Text(
                            post.userName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "@${post.userName
                                .replace(" ", "")
                                .lowercase()}",
                            fontSize = 12.sp
                        )
                    }

                    if (
                        post.userId.isNotEmpty() &&
                        post.userId != currentUserId
                    ) {

                        OutlinedButton(
                            onClick = onFollow,
                            enabled = !isFollowProcessing,
                            contentPadding =
                                PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 0.dp
                                ),
                            modifier = Modifier.height(36.dp)
                        ) {

                            Text(
                                when {
                                    isFollowProcessing -> "..."
                                    isFollowing -> "Following"
                                    else -> "Follow"
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (post.sharedFromPostId.isNotBlank()) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {

                            Text(
                                "Original post by ${post.sharedFromUserName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            if (
                                post.sharedFromText.isNotBlank()
                            ) {

                                Text(
                                    post.sharedFromText,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                if (post.mediaUrl.isNotBlank()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        if (
                            post.mediaType
                                .lowercase()
                                .contains("image") ||
                            post.mediaType
                                .lowercase() == "photo"
                        ) {

                            Text(
                                "🖼️",
                                fontSize = 70.sp
                            )

                        } else {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    "▶",
                                    fontSize = 70.sp
                                )

                                Text(
                                    "Video post",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            PeejeeLogo(size = 90)

                            Spacer(Modifier.height(8.dp))

                            Text("Post")
                        }
                    }
                }

                if (post.text.isNotBlank()) {

                    Spacer(Modifier.height(10.dp))

                    Text(
                        post.text,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(
                            horizontal = 16.dp
                        )
                    )
                }

                Spacer(Modifier.height(14.dp))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                ActionCircle(
                    icon =
                        if (isLiked) "❤️" else "♡",
                    label = likeCount.toString(),
                    onClick = onLike
                )

                Spacer(Modifier.height(14.dp))

                ActionCircle(
                    icon = "💬",
                    label = commentCount.toString(),
                    onClick = onComment
                )

                Spacer(Modifier.height(14.dp))

                ActionCircle(
                    icon = "↗️",
                    label = shareCount.toString(),
                    onClick = onShare
                )

                Spacer(Modifier.height(14.dp))

                ActionCircle(
                    icon = "⋮",
                    label = "",
                    onClick = {
                        showPostMenu = true
                    }
                )
            }
        }
    }

    if (showPostMenu) {

        PostMenu(
            post = post,
            onDismiss = {
                showPostMenu = false
            }
        )
    }
}

@Composable
fun ActionCircle(
    icon: String,
    label: String,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            onClick()
        }
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Color.Black.copy(alpha = 0.55f)
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                icon,
                fontSize = 25.sp
            )
        }

        if (label.isNotBlank()) {

            Spacer(Modifier.height(3.dp))

            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SearchPage(
    onMessage: (PeejeePerson) -> Unit,
    paddingValues: PaddingValues
) {

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf<List<PeejeePerson>>(emptyList())
    }

    var searching by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(searchText) {

        val queryText = searchText.trim()

        if (queryText.isBlank()) {

            results = emptyList()
            searching = false
            errorMessage = ""

        } else {

            searching = true
            errorMessage = ""

            firestore
                .collection("users")
                .orderBy("name")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .limit(30)
                .get()
                .addOnSuccessListener { snapshot ->

                    val currentId =
                        auth.currentUser?.uid

                    results =
                        snapshot.documents.mapNotNull { document ->

                            val uid =
                                document.id

                            if (uid == currentId) {
                                null
                            } else {

                                PeejeePerson(
                                    uid = uid,
                                    name =
                                        document.getString("name")
                                            ?: "Peejee User",
                                    email =
                                        document.getString("email")
                                            ?: "",
                                    isOnline =
                                        document.getBoolean("isOnline")
                                            ?: false
                                )
                            }
                        }

                    searching = false
                }
                .addOnFailureListener { exception ->

                    searching = false

                    errorMessage =
                        exception.message
                            ?: "Could not search users."
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {

        PeejeeLogo(size = 65)

        Spacer(Modifier.height(8.dp))

        Text(
            "Search Users",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            label = {
                Text("Search registered users")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (searching) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (errorMessage.isNotBlank()) {

            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )

        } else if (
            searchText.isNotBlank() &&
            results.isEmpty()
        ) {

            Text(
                "No Peejee user found."
            )

        } else {

            results.forEach { person ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        DefaultProfileIcon(size = 52)

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                person.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (person.email.isNotBlank()) {

                                Text(
                                    person.email,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onMessage(person)
                            }
                        ) {
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesPage(
    onMessage: (PeejeePerson) -> Unit,
    paddingValues: PaddingValues
) {

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf<List<PeejeePerson>>(emptyList())
    }

    var searching by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(searchText) {

        val queryText = searchText.trim()

        if (queryText.isBlank()) {

            results = emptyList()
            searching = false
            errorMessage = ""

        } else {

            searching = true
            errorMessage = ""

            firestore
                .collection("users")
                .orderBy("name")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .limit(30)
                .get()
                .addOnSuccessListener { snapshot ->

                    val currentId =
                        auth.currentUser?.uid

                    results =
                        snapshot.documents.mapNotNull { document ->

                            val uid =
                                document.id

                            if (uid == currentId) {
                                null
                            } else {

                                PeejeePerson(
                                    uid = uid,
                                    name =
                                        document.getString("name")
                                            ?: "Peejee User",
                                    email =
                                        document.getString("email")
                                            ?: "",
                                    isOnline =
                                        document.getBoolean("isOnline")
                                            ?: false
                                )
                            }
                        }

                    searching = false
                }
                .addOnFailureListener { exception ->

                    searching = false

                    errorMessage =
                        exception.message
                            ?: "Could not search users."
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {

        PeejeeLogo(size = 70)

        Spacer(Modifier.height(8.dp))

        Text(
            "Private Messages",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Search a Peejee user to start a private chat."
        )

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            label = {
                Text("Search user")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (searching) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (errorMessage.isNotBlank()) {

            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )

        } else if (
            searchText.isNotBlank() &&
            results.isEmpty()
        ) {

            Text("No user found.")

        } else if (searchText.isBlank()) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        "💬",
                        fontSize = 45.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Find someone and start chatting."
                    )
                }
            }

        } else {

            results.forEach { person ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        DefaultProfileIcon(size = 52)

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                person.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                onMessage(person)
                            }
                        ) {
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateChatPage(
    person: PeejeePerson,
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId =
        auth.currentUser?.uid ?: ""

    var messages by remember {
        mutableStateOf<List<PeejeeMessage>>(emptyList())
    }

    var newMessage by remember {
        mutableStateOf("")
    }

    var showEmojiPicker by remember {
        mutableStateOf(false)
    }

    var sending by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var personIsOnline by remember {
        mutableStateOf(person.isOnline)
    }

    DisposableEffect(person.uid) {

        if (person.uid.isBlank()) {
            onDispose { }
        } else {
            val presenceRegistration =
                firestore
                    .collection("users")
                    .document(person.uid)
                    .addSnapshotListener { document, error ->
                        if (error != null || document == null) return@addSnapshotListener
                        personIsOnline =
                            document.getBoolean("isOnline") ?: false
                    }

            onDispose {
                presenceRegistration.remove()
            }
        }
    }

    DisposableEffect(
        currentUserId,
        person.uid
    ) {

        if (
            currentUserId.isBlank() ||
            person.uid.isBlank()
        ) {

            onDispose { }

        } else {

            val sentRegistration =
                firestore
                    .collection("messages")
                    .whereEqualTo(
                        "senderId",
                        currentUserId
                    )
                    .whereEqualTo(
                        "receiverId",
                        person.uid
                    )
                    .addSnapshotListener { sentSnapshot, sentError ->

                        if (sentError != null) {
                            return@addSnapshotListener
                        }

                        firestore
                            .collection("messages")
                            .whereEqualTo(
                                "senderId",
                                person.uid
                            )
                            .whereEqualTo(
                                "receiverId",
                                currentUserId
                            )
                            .get()
                            .addOnSuccessListener { receivedSnapshot ->

                                val sentMessages =
                                    sentSnapshot
                                        ?.documents
                                        ?.mapNotNull {
                                            messageFromDocument(it)
                                        }
                                        ?: emptyList()

                                val receivedMessages =
                                    receivedSnapshot
                                        .documents
                                        .mapNotNull {
                                            messageFromDocument(it)
                                        }

                                messages =
                                    (
                                        sentMessages +
                                            receivedMessages
                                        )
                                        .distinctBy { it.id }
                                        .sortedBy {
                                            it.timestamp
                                        }
                            }
                    }

            val receivedRegistration =
                firestore
                    .collection("messages")
                    .whereEqualTo(
                        "senderId",
                        person.uid
                    )
                    .whereEqualTo(
                        "receiverId",
                        currentUserId
                    )
                    .addSnapshotListener { receivedSnapshot, receivedError ->

                        if (receivedError != null) {
                            return@addSnapshotListener
                        }

                        firestore
                            .collection("messages")
                            .whereEqualTo(
                                "senderId",
                                currentUserId
                            )
                            .whereEqualTo(
                                "receiverId",
                                person.uid
                            )
                            .get()
                            .addOnSuccessListener { sentSnapshot ->

                                val sentMessages =
                                    sentSnapshot
                                        .documents
                                        .mapNotNull {
                                            messageFromDocument(it)
                                        }

                                val receivedMessages =
                                    receivedSnapshot
                                        ?.documents
                                        ?.mapNotNull {
                                            messageFromDocument(it)
                                        }
                                        ?: emptyList()

                                messages =
                                    (
                                        sentMessages +
                                            receivedMessages
                                        )
                                        .distinctBy { it.id }
                                        .sortedBy {
                                            it.timestamp
                                        }
                            }
                    }

            onDispose {
                sentRegistration.remove()
                receivedRegistration.remove()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {
                Text("← Back")
            }

            DefaultProfileIcon(size = 45)

            Spacer(Modifier.width(10.dp))

            Column {

                Text(
                    person.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (personIsOnline) "🟢 Online" else "⚪ Offline",
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            reverseLayout = false
        ) {

            itemsIndexed(
                messages,
                key = { _, message -> message.id }
            ) { _, message ->

                val mine =
                    message.senderId == currentUserId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 5.dp
                        ),
                    horizontalArrangement =
                        if (mine) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                ) {

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color =
                            if (mine) {
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                            }
                    ) {

                        Column(
                            modifier = Modifier
                                .widthIn(max = 290.dp)
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 9.dp
                                )
                        ) {

                            Text(
                                message.text,
                                fontSize = 16.sp
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                formatPostTime(
                                    message.timestamp
                                ),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {

            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    horizontal = 12.dp
                )
            )
        }

        if (showEmojiPicker) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    itemsIndexed(
                        listOf(
                            "😀", "😂", "🤣", "😊", "😍",
                            "🥰", "😘", "😎", "🤔", "😢",
                            "😭", "😡", "👍", "👎", "👏",
                            "🙏", "❤️", "🔥", "🎉", "💯"
                        )
                    ) { _, emoji ->

                        TextButton(
                            onClick = {
                                newMessage += emoji
                                errorMessage = ""
                                showEmojiPicker = false
                            },
                            enabled = !sending
                        ) {
                            Text(
                                emoji,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    showEmojiPicker = !showEmojiPicker
                },
                enabled = !sending
            ) {
                Text(
                    "😊",
                    fontSize = 25.sp
                )
            }

            OutlinedTextField(
                value = newMessage,
                onValueChange = {
                    newMessage = it
                    errorMessage = ""
                },
                label = {
                    Text("Message")
                },
                modifier = Modifier.weight(1f),
                enabled = !sending
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {

                    val text =
                        newMessage.trim()

                    if (
                        currentUserId.isBlank() ||
                        person.uid.isBlank()
                    ) {

                        errorMessage =
                            "Please log in again."

                        return@Button
                    }

                    if (text.isBlank()) {
                        return@Button
                    }

                    sending = true
                    errorMessage = ""

                    val messageReference =
                        firestore
                            .collection("messages")
                            .document()

                    val messageData =
                        hashMapOf<String, Any>(
                            "messageId" to
                                messageReference.id,
                            "senderId" to
                                currentUserId,
                            "receiverId" to
                                person.uid,
                            "text" to text,
                            "timestamp" to
                                System.currentTimeMillis()
                        )

                    messageReference
                        .set(messageData)
                        .addOnSuccessListener {

                            sending = false

                            firestore
                                .collection("users")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    createPeejeeNotification(
                                        firestore = firestore,
                                        recipientUserId = person.uid,
                                        type = "message",
                                        actorId = currentUserId,
                                        actorName =
                                            userDocument.getString("name")
                                                ?: "Peejee User",
                                        text = "sent you a message",
                                        messageId = messageReference.id
                                    )
                                }

                            newMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            sending = false

                            errorMessage =
                                exception.message
                                    ?: "Could not send message."
                        }
                },
                enabled =
                    !sending &&
                    newMessage.isNotBlank()
            ) {
                Text(
                    if (sending) "..." else "Send"
                )
            }
        }
    }
}

fun messageFromDocument(
    document: com.google.firebase.firestore.DocumentSnapshot
): PeejeeMessage? {

    val senderId =
        document.getString("senderId")
            ?: return null

    val receiverId =
        document.getString("receiverId")
            ?: return null

    val text =
        document.getString("text")
            ?: return null

    return PeejeeMessage(
        id = document.id,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp =
            document.getLong("timestamp")
                ?: 0L,
        read =
            document.getBoolean("read")
                ?: false
    )
}

@Composable
fun CreatePostPage(
    onPostCreated: () -> Unit,
    paddingValues: PaddingValues
) {

    var postText by remember { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val photoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                selectedMedia = uri
                mediaType = "photo"
            }
        }

    val videoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                selectedMedia = uri
                mediaType = "video"
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {

        PeejeeLogo(size = 65)

        Spacer(Modifier.height(8.dp))

        Text(
            "Create Post",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = {
                    photoPicker.launch("image/*")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🖼️ Photo")
            }

            Button(
                onClick = {
                    videoPicker.launch("video/*")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🎥 Video")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedMedia != null) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        if (mediaType == "photo") {
                            "Photo selected"
                        } else {
                            "Video selected"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        selectedMedia.toString(),
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            selectedMedia = null
                            mediaType = ""
                        }
                    ) {
                        Text("Remove")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = postText,
            onValueChange = {
                postText = it
            },
            label = {
                Text("What's on your mind?")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {

            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {

                errorMessage = ""

                val currentUser =
                    auth.currentUser

                if (currentUser == null) {

                    errorMessage =
                        "Please log in again."

                    return@Button
                }

                if (
                    postText.isBlank() &&
                    selectedMedia == null
                ) {

                    errorMessage =
                        "Please write something or select media."

                    return@Button
                }

                publishing = true

                val postReference =
                    firestore
                        .collection("posts")
                        .document()

                val postData =
                    hashMapOf<String, Any>(
                        "postId" to postReference.id,
                        "userId" to currentUser.uid,
                        "userName" to "Peejee User",
                        "text" to postText.trim(),
                        "likes" to 0,
                        "likedBy" to
                            emptyMap<String, Boolean>(),
                        "commentCount" to 0,
                        "shareCount" to 0,
                        "timestamp" to
                            System.currentTimeMillis()
                    )

                if (mediaType.isNotBlank()) {

                    postData["mediaType"] =
                        mediaType

                    postData["mediaUrl"] =
                        selectedMedia?.toString() ?: ""
                }

                firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { userDocument ->

                        postData["userName"] =
                            userDocument.getString("name")
                                ?: "Peejee User"

                        postReference
                            .set(postData)
                            .addOnSuccessListener {

                                publishing = false

                                findMentionedUsers(
                                    firestore = firestore,
                                    text = postText.trim(),
                                    currentUserId = currentUser.uid
                                ) { mentionedUsers ->
                                    val actorName =
                                        userDocument.getString("name")
                                            ?: "Peejee User"

                                    mentionedUsers.forEach { mentioned ->
                                        createPeejeeNotification(
                                            firestore = firestore,
                                            recipientUserId = mentioned.uid,
                                            type = "mention",
                                            actorId = currentUser.uid,
                                            actorName = actorName,
                                            text = "mentioned you in a post",
                                            postId = postReference.id
                                        )
                                    }
                                }

                                postText = ""
                                selectedMedia = null
                                mediaType = ""

                                onPostCreated()
                            }
                            .addOnFailureListener { exception ->

                                publishing = false

                                errorMessage =
                                    exception.message
                                        ?: "Could not publish post."
                            }
                    }
                    .addOnFailureListener {

                        postReference
                            .set(postData)
                            .addOnSuccessListener {

                                publishing = false
                                postText = ""
                                selectedMedia = null
                                mediaType = ""

                                onPostCreated()
                            }
                            .addOnFailureListener { exception ->

                                publishing = false

                                errorMessage =
                                    exception.message
                                        ?: "Could not publish post."
                            }
                    }

            },
            enabled = !publishing,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (publishing) {
                    "Publishing..."
                } else {
                    "Publish Post"
                }
            )
        }
    }
}

@Composable
fun ProfilePage(
    name: String,
    paddingValues: PaddingValues,
    onProfileNameChanged: (String) -> Unit,
    onLoggedOut: () -> Unit
) {

    val context = LocalContext.current

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val scope = rememberCoroutineScope()

    val userId =
        auth.currentUser?.uid

    var bio by remember {
        mutableStateOf("")
    }

    var profileName by remember {
        mutableStateOf(name)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    var showFollowersDialog by remember {
        mutableStateOf(false)
    }

    var showFollowingDialog by remember {
        mutableStateOf(false)
    }

    var editedName by remember {
        mutableStateOf(name)
    }

    var editedBio by remember {
        mutableStateOf("")
    }

    var savingProfile by remember {
        mutableStateOf(false)
    }

    var selectedProfilePhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var profileBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var savedProfilePhoto by remember {
        mutableStateOf("")
    }

    var selectedCoverPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var coverBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var savedCoverPhoto by remember {
        mutableStateOf("")
    }

    var profileError by remember {
        mutableStateOf("")
    }

    var followersCount by remember {
        mutableStateOf(0)
    }

    var followingCount by remember {
        mutableStateOf(0)
    }

    var profilePosts by remember {
        mutableStateOf<List<PeejeePost>>(emptyList())
    }

    var followers by remember {
        mutableStateOf<List<PeejeePerson>>(emptyList())
    }

    var following by remember {
        mutableStateOf<List<PeejeePerson>>(emptyList())
    }

    val email =
        auth.currentUser?.email
            ?: "No email available"

    val profilePhotoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                selectedProfilePhotoUri = uri
                profileError = ""
            }
        }

    val coverPhotoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                selectedCoverPhotoUri = uri
                profileError = ""
            }
        }

    LaunchedEffect(
        selectedProfilePhotoUri,
        savedProfilePhoto
    ) {

        val selectedUri =
            selectedProfilePhotoUri

        profileBitmap =
            withContext(Dispatchers.IO) {

                if (selectedUri != null) {

                    try {

                        context
                            .contentResolver
                            .openInputStream(selectedUri)
                            ?.use {
                                BitmapFactory.decodeStream(it)
                            }

                    } catch (exception: Exception) {
                        null
                    }

                } else {

                    base64ToBitmap(
                        savedProfilePhoto
                    )
                }
            }
    }


    LaunchedEffect(
        selectedCoverPhotoUri,
        savedCoverPhoto
    ) {
        val selectedUri = selectedCoverPhotoUri
        coverBitmap =
            withContext(Dispatchers.IO) {
                if (selectedUri != null) {
                    try {
                        context
                            .contentResolver
                            .openInputStream(selectedUri)
                            ?.use {
                                BitmapFactory.decodeStream(it)
                            }
                    } catch (exception: Exception) {
                        null
                    }
                } else {
                    base64ToBitmap(savedCoverPhoto)
                }
            }
    }

    DisposableEffect(userId) {

        if (userId == null) {

            loading = false

            onDispose { }

        } else {

            loading = true

            val registration =
                firestore
                    .collection("users")
                    .document(userId)
                    .addSnapshotListener { document, error ->

                        if (error != null) {

                            loading = false

                            profileError =
                                error.message
                                    ?: "Could not load your profile."

                            return@addSnapshotListener
                        }

                        if (
                            document != null &&
                            document.exists()
                        ) {

                            val savedName =
                                document.getString("name")
                                    ?.trim()
                                    ?.takeIf { it.isNotBlank() }
                                    ?: name

                            val savedBio =
                                document.getString("bio")
                                    ?: ""

                            profileName = savedName
                            bio = savedBio
                            editedName = savedName
                            editedBio = savedBio

                            savedProfilePhoto =
                                document.getString("profilePhoto")
                                    ?: ""

                            savedCoverPhoto =
                                document.getString("coverPhoto")
                                    ?: ""

                            followersCount =
                                document
                                    .getLong("followersCount")
                                    ?.toInt()
                                    ?: 0

                            followingCount =
                                document
                                    .getLong("followingCount")
                                    ?.toInt()
                                    ?: 0

                            loading = false

                        } else {

                            loading = false
                        }
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    DisposableEffect(userId) {

        if (userId == null) {

            profilePosts = emptyList()

            onDispose { }

        } else {

            val registration =
                firestore
                    .collection("posts")
                    .whereEqualTo(
                        "userId",
                        userId
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (
                            error == null &&
                            snapshot != null
                        ) {

                            profilePosts =
                                snapshot.documents
                                    .mapNotNull { document ->

                                        PeejeePost(
                                            id = document.id,

                                            userId =
                                                document.getString(
                                                    "userId"
                                                ) ?: userId,

                                            userName =
                                                document.getString(
                                                    "userName"
                                                )
                                                    ?.takeIf {
                                                        it.isNotBlank()
                                                    }
                                                    ?: profileName,

                                            text =
                                                document.getString(
                                                    "text"
                                                ) ?: "",

                                            timestamp =
                                                document.getLong(
                                                    "timestamp"
                                                ) ?: 0L,

                                            mediaUrl =
                                                document.getString(
                                                    "mediaUrl"
                                                ) ?: "",

                                            mediaType =
                                                document.getString(
                                                    "mediaType"
                                                ) ?: "",

                                            likeCount =
                                                document.getLong(
                                                    "likes"
                                                )?.toInt() ?: 0,

                                            likedBy =
                                                getLikedBy(document),

                                            commentCount =
                                                document.getLong(
                                                    "commentCount"
                                                )?.toInt() ?: 0,

                                            shareCount =
                                                document.getLong(
                                                    "shareCount"
                                                )?.toInt() ?: 0,

                                            sharedFromPostId =
                                                document.getString(
                                                    "sharedFromPostId"
                                                ) ?: "",

                                            sharedFromUserName =
                                                document.getString(
                                                    "sharedFromUserName"
                                                ) ?: "",

                                            sharedFromText =
                                                document.getString(
                                                    "sharedFromText"
                                                ) ?: ""
                                        )
                                    }
                                    .sortedByDescending {
                                        it.timestamp
                                    }
                        }
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    DisposableEffect(
        userId,
        showFollowersDialog
    ) {

        if (
            userId == null ||
            !showFollowersDialog
        ) {

            followers = emptyList()

            onDispose { }

        } else {

            val registration =
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("followers")
                    .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (
                            error == null &&
                            snapshot != null
                        ) {

                            followers =
                                snapshot.documents.mapNotNull { document ->

                                    val uid =
                                        document.getString("uid")
                                            ?: document.id

                                    val savedName =
                                        document.getString("name")
                                            ?.trim()
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: "Peejee User"

                                    PeejeePerson(
                                        uid = uid,
                                        name = savedName
                                    )
                                }
                        }
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    DisposableEffect(
        userId,
        showFollowingDialog
    ) {

        if (
            userId == null ||
            !showFollowingDialog
        ) {

            following = emptyList()

            onDispose { }

        } else {

            val registration =
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("following")
                    .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (
                            error == null &&
                            snapshot != null
                        ) {

                            following =
                                snapshot.documents.mapNotNull { document ->

                                    val uid =
                                        document.getString("uid")
                                            ?: document.id

                                    val savedName =
                                        document.getString("name")
                                            ?.trim()
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: "Peejee User"

                                    PeejeePerson(
                                        uid = uid,
                                        name = savedName
                                    )
                                }
                        }
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    if (showSettings) {

        SettingsPage(
            email = email,
            paddingValues = paddingValues,
            onBack = {
                showSettings = false
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            item {

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap!!.asImageBitmap(),
                            contentDescription = "Cover photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Add a cover photo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentAlignment =
                        Alignment.Center
                ) {

                    if (profileBitmap != null) {

                        Image(
                            bitmap =
                                profileBitmap!!
                                    .asImageBitmap(),
                            contentDescription =
                                "Profile photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale =
                                ContentScale.Crop
                        )

                    } else {

                        DefaultProfileIcon(size = 90)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    profileName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    email,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                if (loading) {

                    CircularProgressIndicator()

                } else if (bio.isBlank()) {

                    Text(
                        "No bio yet.",
                        fontSize = 16.sp
                    )

                } else {

                    Text(
                        bio,
                        fontSize = 16.sp
                    )
                }

                if (profileError.isNotEmpty()) {

                    Spacer(Modifier.height(8.dp))

                    Text(
                        profileError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    ProfileStat(
                        count = profilePosts.size,
                        label = "Posts"
                    )

                    ProfileStat(
                        count = followersCount,
                        label = "Followers",
                        onClick = {
                            showFollowersDialog = true
                        }
                    )

                    ProfileStat(
                        count = followingCount,
                        label = "Following",
                        onClick = {
                            showFollowingDialog = true
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {

                        editedName = profileName
                        editedBio = bio
                        profileError = ""
                        selectedProfilePhotoUri = null
                        selectedCoverPhotoUri = null
                        showEditDialog = true

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✏️ Edit Profile")
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        showSettings = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔒 Account Settings")
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {

                        val shareText =
                            "Check out my profile on Peejee: $profileName"

                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    shareText
                                )
                            }

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share Profile"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("↗️ Share Profile")
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚪 Log Out")
                }

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "My Posts",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "(${profilePosts.size})",
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            if (profilePosts.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(25.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            PeejeeLogo(size = 70)

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "You haven't posted yet."
                            )
                        }
                    }
                }

            } else {

                itemsIndexed(
                    profilePosts,
                    key = { _, post -> post.id }
                ) { _, post ->

                    ProfilePostCard(post)
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    if (showEditDialog) {

        AlertDialog(

            onDismissRequest = {

                if (!savingProfile) {

                    selectedProfilePhotoUri = null
                    selectedCoverPhotoUri = null
                    profileError = ""
                    showEditDialog = false
                }
            },

            title = {
                Text("Edit Profile")
            },

            text = {

                Column {

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = {
                            editedName = it
                            profileError = ""
                        },
                        label = {
                            Text("Name")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editedBio,
                        onValueChange = {
                            editedBio = it
                            profileError = ""
                        },
                        label = {
                            Text("Bio")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            profilePhotoPicker.launch("image/*")
                        },
                        enabled = !savingProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            if (selectedProfilePhotoUri == null) {
                                "🖼️ Choose Profile Photo"
                            } else {
                                "🖼️ Change Profile Photo"
                            }
                        )
                    }

                    if (selectedProfilePhotoUri != null) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Photo selected",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            coverPhotoPicker.launch("image/*")
                        },
                        enabled = !savingProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selectedCoverPhotoUri == null) {
                                "🖼️ Choose Cover Photo"
                            } else {
                                "🖼️ Change Cover Photo"
                            }
                        )
                    }

                    if (selectedCoverPhotoUri != null) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Cover photo selected",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (profileError.isNotEmpty()) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            profileError,
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(

                    enabled =
                        !savingProfile &&
                        editedName.isNotBlank(),

                    onClick = {

                        if (userId == null) {

                            profileError =
                                "Please log in again."

                            return@TextButton
                        }

                        savingProfile = true
                        profileError = ""

                        val selectedUri =
                            selectedProfilePhotoUri

                        val newName =
                            editedName.trim()

                        val newBio =
                            editedBio.trim()

                        scope.launch {

                            try {

                                val photoBase64 =
                                    if (selectedUri != null) {

                                        withContext(
                                            Dispatchers.IO
                                        ) {

                                            try {

                                                val bitmap =
                                                    context
                                                        .contentResolver
                                                        .openInputStream(
                                                            selectedUri
                                                        )
                                                        ?.use {
                                                            BitmapFactory
                                                                .decodeStream(it)
                                                        }

                                                if (bitmap != null) {
                                                    bitmapToBase64(bitmap)
                                                } else {
                                                    ""
                                                }

                                            } catch (
                                                exception: Exception
                                            ) {
                                                ""
                                            }
                                        }

                                    } else {
                                        savedProfilePhoto
                                    }

                                if (
                                    selectedUri != null &&
                                    photoBase64.isBlank()
                                ) {

                                    savingProfile = false

                                    profileError =
                                        "Could not read the selected photo."

                                    return@launch
                                }

                                val coverBase64 =
                                    if (selectedCoverPhotoUri != null) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val bitmap =
                                                    context
                                                        .contentResolver
                                                        .openInputStream(
                                                            selectedCoverPhotoUri!!
                                                        )
                                                        ?.use {
                                                            BitmapFactory.decodeStream(it)
                                                        }

                                                if (bitmap != null) {
                                                    bitmapToBase64(bitmap)
                                                } else {
                                                    ""
                                                }
                                            } catch (exception: Exception) {
                                                ""
                                            }
                                        }
                                    } else {
                                        savedCoverPhoto
                                    }

                                if (
                                    selectedCoverPhotoUri != null &&
                                    coverBase64.isBlank()
                                ) {
                                    savingProfile = false
                                    profileError =
                                        "Could not read the selected cover photo."
                                    return@launch
                                }

                                val updates =
                                    hashMapOf<String, Any>(
                                        "name" to newName,
                                        "bio" to newBio,
                                        "profilePhoto" to
                                            photoBase64,
                                        "coverPhoto" to
                                            coverBase64,
                                        "updatedAt" to
                                            System.currentTimeMillis()
                                    )

                                firestore
                                    .collection("users")
                                    .document(userId)
                                    .set(
                                        updates,
                                        SetOptions.merge()
                                    )
                                    .addOnSuccessListener {

                                        profileName = newName
                                        bio = newBio
                                        savedProfilePhoto =
                                            photoBase64
                                        savedCoverPhoto =
                                            coverBase64
                                        editedName = newName
                                        editedBio = newBio
                                        selectedProfilePhotoUri = null
                                        selectedCoverPhotoUri = null
                                        savingProfile = false
                                        showEditDialog = false

                                        onProfileNameChanged(
                                            newName
                                        )
                                    }
                                    .addOnFailureListener { exception ->

                                        savingProfile = false

                                        profileError =
                                            exception.message
                                                ?: "Could not save your profile."
                                    }

                            } catch (exception: Exception) {

                                savingProfile = false

                                profileError =
                                    exception.message
                                        ?: "Could not save your profile."
                            }
                        }
                    }
                ) {

                    Text(
                        if (savingProfile) {
                            "Saving..."
                        } else {
                            "Save"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !savingProfile,
                    onClick = {

                        selectedProfilePhotoUri = null
                        selectedCoverPhotoUri = null
                        profileError = ""
                        showEditDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {

        AlertDialog(

            onDismissRequest = {
                showLogoutDialog = false
            },

            title = {
                Text("Log Out?")
            },

            text = {
                Text(
                    "Are you sure you want to log out of Peejee?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showLogoutDialog = false

                        auth.signOut()

                        onLoggedOut()
                    }
                ) {
                    Text("Log Out")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFollowersDialog) {

        PersonListDialog(
            title = "Followers",
            people = followers,
            onDismiss = {
                showFollowersDialog = false
            }
        )
    }

    if (showFollowingDialog) {

        PersonListDialog(
            title = "Following",
            people = following,
            onDismiss = {
                showFollowingDialog = false
            }
        )
    }
}

@Composable
fun ProfileStat(
    count: Int,
    label: String,
    onClick: (() -> Unit)? = null
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            if (onClick != null) {
                Modifier.clickable {
                    onClick()
                }
            } else {
                Modifier
            }
    ) {

        Text(
            count.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(label)
    }
}

@Composable
fun ProfilePostCard(
    post: PeejeePost
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            if (post.sharedFromPostId.isNotBlank()) {

                Text(
                    "🔁 Shared from ${post.sharedFromUserName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))
            }

            if (post.text.isNotBlank()) {

                Text(
                    post.text,
                    fontSize = 17.sp
                )
            }

            if (post.mediaType.isNotBlank()) {

                Spacer(Modifier.height(10.dp))

                Text(
                    if (
                        post.mediaType
                            .lowercase()
                            .contains("image") ||
                        post.mediaType
                            .lowercase() == "photo"
                    ) {
                        "🖼️ Photo post"
                    } else {
                        "🎥 Video post"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                formatPostTime(post.timestamp),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "❤️ ${post.likeCount} likes",
                fontSize = 13.sp
            )

            Text(
                "💬 ${post.commentCount} comments",
                fontSize = 13.sp
            )

            Text(
                "↗️ ${post.shareCount} shares",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun PersonListDialog(
    title: String,
    people: List<PeejeePerson>,
    onDismiss: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(title)
        },

        text = {

            if (people.isEmpty()) {

                Text(
                    if (title == "Followers") {
                        "No followers yet."
                    } else {
                        "You are not following anyone yet."
                    }
                )

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {

                    itemsIndexed(people) { _, person ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            DefaultProfileIcon(size = 45)

                            Spacer(Modifier.width(12.dp))

                            Text(
                                person.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
