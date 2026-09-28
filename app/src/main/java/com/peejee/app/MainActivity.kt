package com.peejee.app

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PeejeeApp()
        }
    }
}

@Composable
fun PeejeeLogo(
    modifier: Modifier = Modifier,
    size: Int = 70
) {
    Image(
        painter = painterResource(
            id = R.drawable.peejee_app_icon_512
        ),
        contentDescription = "Peejee",
        modifier = modifier.size(size.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DefaultProfileIcon(
    size: Int = 65
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👤",
            fontSize = (size * 0.55f).sp
        )
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
            name = userName
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

        PeejeeLogo(
            size = 130
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Connect. Chat. Share.",
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        Button(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

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

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

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

        PeejeeLogo(
            size = 90
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "Create Account",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text("Full Name")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            visualTransformation =
                PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
            },
            label = {
                Text("Confirm Password")
            },
            visualTransformation =
                PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        Button(
            onClick = {

                errorMessage = ""

                when {

                    name.isBlank() -> {
                        errorMessage =
                            "Please enter your full name."
                    }

                    email.isBlank() -> {
                        errorMessage =
                            "Please enter your email."
                    }

                    password.length < 6 -> {
                        errorMessage =
                            "Password must be at least 6 characters."
                    }

                    password != confirmPassword -> {
                        errorMessage =
                            "Passwords do not match."
                    }

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
                                        hashMapOf(
                                            "uid" to user.uid,
                                            "name" to name.trim(),
                                            "email" to email.trim(),
                                            "bio" to "",
                                            "profilePhoto" to ""
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

        Spacer(
            modifier = Modifier.height(12.dp)
        )

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

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

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

        PeejeeLogo(
            size = 90
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "Log In",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            visualTransformation =
                PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        Button(
            onClick = {

                errorMessage = ""

                when {

                    email.isBlank() -> {
                        errorMessage =
                            "Please enter your email."
                    }

                    password.isBlank() -> {
                        errorMessage =
                            "Please enter your password."
                    }

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

                                            val name =
                                                document.getString("name")
                                                    ?: "Peejee User"

                                            onLoginSuccess(name)
                                        }
                                        .addOnFailureListener {

                                            loading = false

                                            onLoginSuccess(
                                                "Peejee User"
                                            )
                                        }

                                } else {

                                    loading = false

                                    errorMessage =
                                        "Login failed."
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
                if (loading) {
                    "Logging In..."
                } else {
                    "Log In"
                }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Back")
        }
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
    val likedBy: Map<String, Boolean> = emptyMap()
)

data class PeejeeComment(
    val id: String,
    val userId: String,
    val userName: String,
    val text: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    name: String
) {

    var selectedTab by remember {
        mutableStateOf(0)
    }

    var posts by remember {
        mutableStateOf<List<PeejeePost>>(emptyList())
    }

    var followedUserIds by remember {
        mutableStateOf(setOf<String>())
    }

    var selectedComments by remember {
        mutableStateOf<List<PeejeeComment>>(emptyList())
    }

    var showCommentDialog by remember {
        mutableStateOf(false)
    }

    var selectedPostId by remember {
        mutableStateOf("")
    }

    var newComment by remember {
        mutableStateOf("")
    }

    var loadingPosts by remember {
        mutableStateOf(true)
    }

    var loadingComments by remember {
        mutableStateOf(false)
    }

    var sendingComment by remember {
        mutableStateOf(false)
    }

    var commentError by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId =
        auth.currentUser?.uid ?: ""

    DisposableEffect(Unit) {

        val registration: ListenerRegistration =
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

                        val loadedPosts =
                            snapshot.documents.mapNotNull { document ->

                                val text =
                                    document.getString("text")
                                        ?: ""

                                val userId =
                                    document.getString("userId")
                                        ?: ""

                                val userName =
                                    document.getString("userName")
                                        ?: "Peejee User"

                                val timestamp =
                                    document.getLong("timestamp")
                                        ?: 0L

                                val mediaUrl =
                                    document.getString("mediaUrl")
                                        ?: ""

                                val mediaType =
                                    document.getString("mediaType")
                                        ?: ""

                                val likeCount =
                                    document.getLong("likes")
                                        ?.toInt()
                                        ?: 0

                                val likedByRaw =
                                    document.get("likedBy")

                                val likedBy =
                                    if (likedByRaw is Map<*, *>) {

                                        likedByRaw
                                            .entries
                                            .mapNotNull { entry ->

                                                val key =
                                                    entry.key as? String

                                                val value =
                                                    entry.value as? Boolean

                                                if (
                                                    key != null &&
                                                    value != null
                                                ) {
                                                    key to value
                                                } else {
                                                    null
                                                }
                                            }
                                            .toMap()

                                    } else {
                                        emptyMap()
                                    }

                                PeejeePost(
                                    id = document.id,
                                    userId = userId,
                                    userName = userName,
                                    text = text,
                                    timestamp = timestamp,
                                    mediaUrl = mediaUrl,
                                    mediaType = mediaType,
                                    likeCount = likeCount,
                                    likedBy = likedBy
                                )
                            }

                        posts = loadedPosts
                    }

                    loadingPosts = false
                }

        onDispose {
            registration.remove()
        }
    }

    DisposableEffect(
        showCommentDialog,
        selectedPostId
    ) {

        if (
            !showCommentDialog ||
            selectedPostId.isBlank()
        ) {

            selectedComments = emptyList()
            loadingComments = false
            commentError = ""

            onDispose { }

        } else {

            loadingComments = true
            commentError = ""

            val commentRegistration =
                firestore
                    .collection("posts")
                    .document(selectedPostId)
                    .collection("comments")
                    .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {

                            loadingComments = false

                            commentError =
                                error.message
                                    ?: "Could not load comments."

                            return@addSnapshotListener
                        }

                        if (snapshot != null) {

                            selectedComments =
                                snapshot.documents.mapNotNull { document ->

                                    val userId =
                                        document.getString("userId")
                                            ?: ""

                                    val userName =
                                        document.getString("userName")
                                            ?: "Peejee User"

                                    val text =
                                        document.getString("text")
                                            ?: ""

                                    val timestamp =
                                        document.getLong("timestamp")
                                            ?: 0L

                                    if (text.isBlank()) {
                                        null
                                    } else {
                                        PeejeeComment(
                                            id = document.id,
                                            userId = userId,
                                            userName = userName,
                                            text = text,
                                            timestamp = timestamp
                                        )
                                    }
                                }
                        }

                        loadingComments = false
                    }

            onDispose {
                commentRegistration.remove()
            }
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    PeejeeLogo(
                        size = 48
                    )
                }
            )
        },

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    icon = {
                        Text("🏠")
                    },
                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    icon = {
                        Text("🔍")
                    },
                    label = {
                        Text("Search")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    icon = {
                        Text("➕")
                    },
                    label = {
                        Text("Post")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                    },
                    icon = {
                        Text("💬")
                    },
                    label = {
                        Text("Messages")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                    },
                    icon = {
                        Text("👤")
                    },
                    label = {
                        Text("Profile")
                    }
                )
            }
        }

    ) { paddingValues ->

        when (selectedTab) {

            0 -> {

                HomeFeed(
                    name = name,
                    currentUserId = currentUserId,
                    posts = posts,
                    followedUserIds = followedUserIds,
                    loadingPosts = loadingPosts,

                    onLike = { postId ->

                        val userId =
                            auth.currentUser?.uid

                        if (userId != null) {

                            val postRef =
                                firestore
                                    .collection("posts")
                                    .document(postId)

                            firestore.runTransaction { transaction ->

                                val snapshot =
                                    transaction.get(postRef)

                                val currentLikes =
                                    snapshot
                                        .getLong("likes")
                                        ?.toInt()
                                        ?: 0

                                val currentLikedBy =
                                    mutableMapOf<String, Boolean>()

                                val rawLikedBy =
                                    snapshot.get("likedBy")

                                if (rawLikedBy is Map<*, *>) {

                                    for (entry in rawLikedBy.entries) {

                                        val key =
                                            entry.key as? String

                                        val value =
                                            entry.value as? Boolean

                                        if (
                                            key != null &&
                                            value != null
                                        ) {

                                            currentLikedBy[key] =
                                                value
                                        }
                                    }
                                }

                                val alreadyLiked =
                                    currentLikedBy[userId] == true

                                if (alreadyLiked) {

                                    currentLikedBy.remove(
                                        userId
                                    )

                                    transaction.update(
                                        postRef,
                                        "likes",
                                        (currentLikes - 1)
                                            .coerceAtLeast(0)
                                    )

                                    transaction.update(
                                        postRef,
                                        "likedBy",
                                        currentLikedBy
                                    )

                                } else {

                                    currentLikedBy[userId] =
                                        true

                                    transaction.update(
                                        postRef,
                                        "likes",
                                        currentLikes + 1
                                    )

                                    transaction.update(
                                        postRef,
                                        "likedBy",
                                        currentLikedBy
                                    )
                                }

                                null

                            }.addOnFailureListener {
                            }
                        }
                    },

                    onFollow = { userId ->

                        val newFollowing =
                            followedUserIds.toMutableSet()

                        if (
                            newFollowing.contains(userId)
                        ) {

                            newFollowing.remove(userId)

                        } else {

                            newFollowing.add(userId)
                        }

                        followedUserIds =
                            newFollowing
                    },

                    onComment = { postId ->

                        selectedPostId = postId
                        newComment = ""
                        commentError = ""
                        showCommentDialog = true
                    },

                    onShare = { post ->

                        val shareText =
                            "Check this out on Peejee:\n\n${post.text}"

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
                                "Share Peejee post"
                            )
                        )
                    },

                    paddingValues = paddingValues
                )
            }

            1 -> SearchPage(
                name = name,
                paddingValues = paddingValues
            )

            2 -> CreatePostPage(
                onPostCreated = {
                    selectedTab = 0
                },
                paddingValues = paddingValues
            )

            3 -> MessagesPage(
                name = name,
                paddingValues = paddingValues
            )

            4 -> ProfilePage(
                name = name,
                paddingValues = paddingValues
            )
        }
    }

    if (showCommentDialog) {

        AlertDialog(

            onDismissRequest = {

                if (!sendingComment) {
                    showCommentDialog = false
                }
            },

            title = {
                Text("Comments")
            },

            text = {

                Column {

                    if (loadingComments) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.Center
                        ) {

                            CircularProgressIndicator()
                        }

                    } else if (
                        selectedComments.isEmpty()
                    ) {

                        Text(
                            "No comments yet. Be the first!"
                        )

                    } else {

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = 260.dp
                                )
                        ) {

                            itemsIndexed(
                                selectedComments
                            ) { _, comment ->

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp
                                        )
                                ) {

                                    Text(
                                        text = comment.userName,
                                        fontWeight =
                                            FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(2.dp)
                                    )

                                    Text(
                                        text = comment.text,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    if (commentError.isNotEmpty()) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text = commentError,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = newComment,
                        onValueChange = {
                            newComment = it
                            commentError = ""
                        },
                        label = {
                            Text("Write a comment")
                        },
                        enabled = !sendingComment,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {

                TextButton(

                    enabled =
                        !sendingComment &&
                        newComment.isNotBlank(),

                    onClick = {

                        val currentUser =
                            auth.currentUser

                        if (currentUser == null) {

                            commentError =
                                "Please log in again."

                            return@TextButton
                        }

                        if (selectedPostId.isBlank()) {

                            commentError =
                                "Post not found."

                            return@TextButton
                        }

                        sendingComment = true
                        commentError = ""

                        firestore
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { userDocument ->

                                val savedUserName =
                                    userDocument
                                        .getString("name")
                                        ?: currentUser.displayName
                                        ?: "Peejee User"

                                val commentReference =
                                    firestore
                                        .collection("posts")
                                        .document(
                                            selectedPostId
                                        )
                                        .collection("comments")
                                        .document()

                                val commentData =
                                    hashMapOf<String, Any>(

                                        "commentId" to
                                            commentReference.id,

                                        "postId" to
                                            selectedPostId,

                                        "userId" to
                                            currentUser.uid,

                                        "userName" to
                                            savedUserName,

                                        "text" to
                                            newComment.trim(),

                                        "timestamp" to
                                            System.currentTimeMillis()
                                    )

                                commentReference
                                    .set(commentData)
                                    .addOnSuccessListener {

                                        sendingComment = false
                                        newComment = ""
                                    }
                                    .addOnFailureListener { exception ->

                                        sendingComment = false

                                        commentError =
                                            exception.message
                                                ?: "Could not post comment."
                                    }
                            }
                            .addOnFailureListener { exception ->

                                sendingComment = false

                                commentError =
                                    exception.message
                                        ?: "Could not load your profile."
                            }
                    }
                ) {

                    Text(
                        if (sendingComment) {
                            "Posting..."
                        } else {
                            "Comment"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(

                    enabled = !sendingComment,

                    onClick = {
                        showCommentDialog = false
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
    onLike: (String) -> Unit,
    onFollow: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (PeejeePost) -> Unit,
    paddingValues: PaddingValues
) {

    val liveUsers = listOf(
        "Live",
        "David",
        "Sarah",
        "Mike",
        "Blessing"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        item {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Welcome, $name 👋",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "🔴 Live Now",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp
                ),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                itemsIndexed(
                    liveUsers
                ) { _, liveUser ->

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

                                    PeejeeLogo(
                                        size = 60
                                    )

                                } else {

                                    DefaultProfileIcon(
                                        size = 60
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            if (liveUser == "Live") {

                                Text(
                                    text = "Peejee Live",
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                            } else {

                                Text(
                                    text = liveUser,
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Surface(
                                shape = MaterialTheme
                                    .shapes
                                    .small
                            ) {

                                Text(
                                    text = "🔴 LIVE",
                                    fontSize = 12.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = "👁 0 viewers",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

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

            if (
                !loadingPosts &&
                posts.isEmpty()
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        PeejeeLogo(
                            size = 75
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Post",
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        itemsIndexed(
            posts,
            key = { _, post ->
                post.id
            }
        ) { _, post ->

            TikTokStylePost(
                post = post,
                likeCount = post.likeCount,
                isLiked =
                    post.likedBy[currentUserId] == true,
                isFollowing =
                    followedUserIds.contains(
                        post.userId
                    ),
                currentUserName = name,

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
                }
            )
        }

        item {

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

@Composable
fun TikTokStylePost(
    post: PeejeePost,
    likeCount: Int,
    isLiked: Boolean,
    isFollowing: Boolean,
    currentUserName: String,
    onLike: () -> Unit,
    onFollow: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {

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

                    DefaultProfileIcon(
                        size = 48
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = post.userName,
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "@${post.userName.replace(" ", "").lowercase()}",
                            fontSize = 12.sp
                        )
                    }

                    if (
                        post.userId.isNotEmpty() &&
                        post.userName != currentUserName
                    ) {

                        OutlinedButton(
                            onClick = onFollow,
                            contentPadding =
                                PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 0.dp
                                ),
                            modifier =
                                Modifier.height(36.dp)
                        ) {

                            Text(
                                if (isFollowing) {
                                    "Following"
                                } else {
                                    "Follow"
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

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
                                .contains("image")
                        ) {

                            Text(
                                text = "🖼️",
                                fontSize = 70.sp
                            )

                        } else {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "▶",
                                    fontSize = 70.sp
                                )

                                Text(
                                    text = "Video post",
                                    fontSize = 18.sp,
                                    fontWeight =
                                        FontWeight.Bold
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

                            PeejeeLogo(
                                size = 90
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text = "Post",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                if (post.text.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = post.text,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(
                            horizontal = 16.dp
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
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
                        if (isLiked) {
                            "❤️"
                        } else {
                            "♡"
                        },
                    label = likeCount.toString(),
                    onClick = onLike
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ActionCircle(
                    icon = "💬",
                    label = "Comment",
                    onClick = onComment
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ActionCircle(
                    icon = "↗️",
                    label = "Share",
                    onClick = onShare
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ActionCircle(
                    icon = "🔊",
                    label = "Sound",
                    onClick = {
                    }
                )
            }
        }
    }
}

@Composable
fun ActionCircle(
    icon: String,
    label: String,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,
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
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = icon,
                fontSize = 25.sp
            )
        }

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SearchPage(
    name: String,
    paddingValues: PaddingValues
) {

    var searchText by remember {
        mutableStateOf("")
    }

    val people = listOf(
        name,
        "Peejee User",
        "New Friend"
    )

    val results =
        people.filter {
            it.contains(
                searchText,
                ignoreCase = true
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {

        PeejeeLogo(
            size = 65
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Search",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            label = {
                Text("Search people")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        results.forEach { person ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    DefaultProfileIcon(
                        size = 45
                    )

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Text(
                        text = person,
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePostPage(
    onPostCreated: () -> Unit,
    paddingValues: PaddingValues
) {

    var postText by remember {
        mutableStateOf("")
    }

    var selectedMedia by remember {
        mutableStateOf<Uri?>(null)
    }

    var mediaType by remember {
        mutableStateOf("")
    }

    var publishing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val photoPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                selectedMedia = uri
                mediaType = "photo"
            }
        }

    val videoPicker =
        rememberLauncherForActivityResult(
            contract =
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

        PeejeeLogo(
            size = 65
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Create Post",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

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

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (selectedMedia != null) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            if (
                                mediaType == "photo"
                            ) {
                                "Photo selected"
                            } else {
                                "Video selected"
                            },
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            selectedMedia.toString(),
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

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

            Spacer(
                modifier = Modifier.height(16.dp)
            )
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

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color =
                    MaterialTheme.colorScheme.error
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
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

                val postId =
                    postReference.id

                val postData =
                    hashMapOf<String, Any>(

                        "postId" to
                            postId,

                        "userId" to
                            currentUser.uid,

                        "userName" to
                            (
                                currentUser.displayName
                                    ?: "Peejee User"
                            ),

                        "text" to
                            postText.trim(),

                        "likes" to
                            0,

                        "likedBy" to
                            emptyMap<String, Boolean>(),

                        "timestamp" to
                            System.currentTimeMillis()
                    )

                if (mediaType.isNotBlank()) {

                    postData["mediaType"] =
                        mediaType

                    postData["mediaUrl"] =
                        selectedMedia?.toString()
                            ?: ""
                }

                firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { userDocument ->

                        val savedName =
                            userDocument
                                .getString("name")
                                ?: "Peejee User"

                        postData["userName"] =
                            savedName

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
fun MessagesPage(
    name: String,
    paddingValues: PaddingValues
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {

        PeejeeLogo(
            size = 70
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Messages",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                PeejeeLogo(
                    size = 70
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    "Welcome $name! Your conversations will appear here."
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Button(
                    onClick = {
                    }
                ) {

                    Text("Start a Chat")
                }
            }
        }
    }
}

@Composable
fun ProfilePage(
    name: String,
    paddingValues: PaddingValues
) {

    val context = LocalContext.current

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

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

    val email =
        auth.currentUser?.email
            ?: "No email available"

    val profilePhotoPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                selectedProfilePhotoUri = uri
            }
        }

    LaunchedEffect(selectedProfilePhotoUri) {

        val uri = selectedProfilePhotoUri

        if (uri == null) {

            if (savedProfilePhoto.isBlank()) {
                profileBitmap = null
            }

        } else {

            profileBitmap =
                withContext(Dispatchers.IO) {

                    try {

                        context
                            .contentResolver
                            .openInputStream(uri)
                            ?.use { inputStream ->

                                BitmapFactory
                                    .decodeStream(
                                        inputStream
                                    )
                            }

                    } catch (exception: Exception) {

                        null
                    }
                }
        }
    }

    LaunchedEffect(userId) {

        if (userId != null) {

            firestore
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->

                    bio =
                        document.getString("bio")
                            ?: ""

                    editedBio = bio

                    profileName =
                        document.getString("name")
                            ?: name

                    editedName =
                        profileName

                    savedProfilePhoto =
                        document.getString(
                            "profilePhoto"
                        ) ?: ""

                    if (
                        savedProfilePhoto.isNotBlank()
                    ) {

                        try {

                            val imageBytes =
                                Base64.decode(
                                    savedProfilePhoto,
                                    Base64.DEFAULT
                                )

                            profileBitmap =
                                BitmapFactory.decodeByteArray(
                                    imageBytes,
                                    0,
                                    imageBytes.size
                                )

                        } catch (exception: Exception) {

                            profileBitmap = null
                        }
                    }

                    loading = false
                }
                .addOnFailureListener {

                    loading = false
                }

        } else {

            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
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

                    DefaultProfileIcon(
                        size = 90
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = profileName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = email,
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (loading) {

            CircularProgressIndicator()

        } else {

            if (bio.isBlank()) {

                Text(
                    text = "No bio yet.",
                    fontSize = 16.sp
                )

            } else {

                Text(
                    text = bio,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "0",
                    fontSize = 22.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text("Posts")
            }

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "0",
                    fontSize = 22.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text("Followers")
            }

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "0",
                    fontSize = 22.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text("Following")
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Button(
            onClick = {

                editedName = profileName
                editedBio = bio
                showEditDialog = true

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Edit Profile")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

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

            Text("Share Profile")
        }
    }

    if (showEditDialog) {

        AlertDialog(

            onDismissRequest = {

                if (!savingProfile) {
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
                        },
                        label = {
                            Text("Name")
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = editedBio,
                        onValueChange = {
                            editedBio = it
                        },
                        label = {
                            Text("Bio")
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {

                            profilePhotoPicker.launch(
                                "image/*"
                            )

                        },
                        enabled = !savingProfile,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            if (
                                selectedProfilePhotoUri ==
                                null
                            ) {

                                "🖼️ Choose Profile Photo"

                            } else {

                                "🖼️ Change Profile Photo"
                            }
                        )
                    }

                    if (
                        selectedProfilePhotoUri != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text = "Photo selected",
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
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

                            return@TextButton
                        }

                        savingProfile = true

                        val selectedUri =
                            selectedProfilePhotoUri

                        if (selectedUri != null) {

                            LaunchedEffect(Unit) {
                            }
                        }

                        Thread {

                            try {

                                var photoBase64 =
                                    savedProfilePhoto

                                if (selectedUri != null) {

                                    val inputStream =
                                        context
                                            .contentResolver
                                            .openInputStream(
                                                selectedUri
                                            )

                                    val originalBitmap =
                                        inputStream?.use {
                                            BitmapFactory
                                                .decodeStream(it)
                                        }

                                    if (
                                        originalBitmap != null
                                    ) {

                                        val maxSize = 600

                                        val width =
                                            originalBitmap.width

                                        val height =
                                            originalBitmap.height

                                        val scale =
                                            minOf(
                                                1f,
                                                maxSize.toFloat() /
                                                    maxOf(
                                                        width,
                                                        height
                                                    )
                                            )

                                        val resizedBitmap =
                                            if (scale < 1f) {

                                                Bitmap.createScaledBitmap(
                                                    originalBitmap,
                                                    (
                                                        width *
                                                            scale
                                                    ).toInt()
                                                        .coerceAtLeast(1),
                                                    (
                                                        height *
                                                            scale
                                                    ).toInt()
                                                        .coerceAtLeast(1),
                                                    true
                                                )

                                            } else {

                                                originalBitmap
                                            }

                                        val outputStream =
                                            ByteArrayOutputStream()

                                        resizedBitmap.compress(
                                            Bitmap.CompressFormat.JPEG,
                                            70,
                                            outputStream
                                        )

                                        val imageBytes =
                                            outputStream
                                                .toByteArray()

                                        photoBase64 =
                                            Base64.encodeToString(
                                                imageBytes,
                                                Base64.NO_WRAP
                                            )

                                        if (
                                            resizedBitmap !==
                                            originalBitmap
                                        ) {

                                            resizedBitmap.recycle()
                                        }

                                        if (
                                            originalBitmap
                                                .isRecycled
                                                .not()
                                        ) {

                                            originalBitmap.recycle()
                                        }
                                    }
                                }

                                val finalPhoto =
                                    photoBase64

                                val updates =
                                    hashMapOf<String, Any>(
                                        "name" to
                                            editedName.trim(),
                                        "bio" to
                                            editedBio.trim(),
                                        "profilePhoto" to
                                            finalPhoto
                                    )

                                firestore
                                    .collection("users")
                                    .document(userId)
                                    .update(updates)
                                    .addOnSuccessListener {

                                        profileName =
                                            editedName.trim()

                                        bio =
                                            editedBio.trim()

                                        savedProfilePhoto =
                                            finalPhoto

                                        selectedProfilePhotoUri =
                                            null

                                        if (
                                            finalPhoto.isNotBlank()
                                        ) {

                                            try {

                                                val imageBytes =
                                                    Base64.decode(
                                                        finalPhoto,
                                                        Base64.DEFAULT
                                                    )

                                                profileBitmap =
                                                    BitmapFactory
                                                        .decodeByteArray(
                                                            imageBytes,
                                                            0,
                                                            imageBytes.size
                                                        )

                                            } catch (
                                                exception: Exception
                                            ) {

                                                profileBitmap =
                                                    null
                                            }
                                        }

                                        savingProfile = false

                                        showEditDialog =
                                            false
                                    }
                                    .addOnFailureListener { exception ->

                                        savingProfile = false
                                    }

                            } catch (exception: Exception) {

                                savingProfile = false
                            }

                        }.start()
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
                        showEditDialog = false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}
