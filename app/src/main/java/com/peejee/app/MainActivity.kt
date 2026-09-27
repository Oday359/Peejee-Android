package com.peejee.app

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PeejeeApp()
        }
    }
}

@Composable
fun PeejeeApp() {

    var screen by remember { mutableStateOf("welcome") }
    var userName by remember { mutableStateOf("") }

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

        Text(
            text = "Peejee",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Connect. Chat. Share.",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Text(
            text = "Create Account",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
            },
            label = {
                Text("Confirm Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))
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
                                            "bio" to ""
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

        Spacer(modifier = Modifier.height(12.dp))

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

        Text(
            text = "Log In",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))
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

        Spacer(modifier = Modifier.height(12.dp))

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

    var likedPostIds by remember {
        mutableStateOf(setOf<String>())
    }

    var likeCounts by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }

    var comments by remember {
        mutableStateOf<Map<String, List<String>>>(emptyMap())
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

    val context = LocalContext.current

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

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
                                        ?: return@mapNotNull null

                                val userId =
                                    document.getString("userId")
                                        ?: ""

                                val userName =
                                    document.getString("userName")
                                        ?: "Peejee User"

                                val timestamp =
                                    document.getLong("timestamp")
                                        ?: 0L

                                PeejeePost(
                                    id = document.id,
                                    userId = userId,
                                    userName = userName,
                                    text = text,
                                    timestamp = timestamp
                                )
                            }

                        posts = loadedPosts

                        val loadedLikes =
                            mutableMapOf<String, Int>()

                        loadedPosts.forEach { post ->

                            val document =
                                snapshot.documents.firstOrNull {
                                    it.id == post.id
                                }

                            loadedLikes[post.id] =
                                document
                                    ?.getLong("likes")
                                    ?.toInt()
                                    ?: 0
                        }

                        likeCounts = loadedLikes
                    }

                    loadingPosts = false
                }

        onDispose {
            registration.remove()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(
                title = {

                    Text(
                        text = "Peejee",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
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
                    posts = posts,
                    likes = likeCounts,
                    likedPostIds = likedPostIds,
                    loadingPosts = loadingPosts,

                    onLike = { postId ->

                        val currentlyLiked =
                            likedPostIds.contains(postId)

                        val newLiked =
                            likedPostIds.toMutableSet()

                        if (currentlyLiked) {
                            newLiked.remove(postId)
                        } else {
                            newLiked.add(postId)
                        }

                        likedPostIds = newLiked

                        val currentCount =
                            likeCounts[postId] ?: 0

                        val newCount =
                            if (currentlyLiked) {
                                (currentCount - 1)
                                    .coerceAtLeast(0)
                            } else {
                                currentCount + 1
                            }

                        likeCounts =
                            likeCounts.toMutableMap().apply {
                                this[postId] = newCount
                            }

                        firestore
                            .collection("posts")
                            .document(postId)
                            .update(
                                "likes",
                                newCount
                            )
                    },

                    onComment = { postId ->

                        selectedPostId = postId
                        newComment = ""
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

        val selectedComments =
            comments[selectedPostId] ?: emptyList()

        AlertDialog(
            onDismissRequest = {
                showCommentDialog = false
            },

            title = {
                Text("Comments")
            },

            text = {

                Column {

                    if (selectedComments.isEmpty()) {

                        Text(
                            "No comments yet. Be the first!"
                        )

                    } else {

                        selectedComments.forEach { comment ->

                            Text(
                                text = comment,
                                modifier = Modifier.padding(
                                    vertical = 5.dp
                                )
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = newComment,
                        onValueChange = {
                            newComment = it
                        },
                        label = {
                            Text("Write a comment")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        if (newComment.isNotBlank()) {

                            val updatedComments =
                                comments.toMutableMap()

                            val currentComments =
                                updatedComments[
                                    selectedPostId
                                ]?.toMutableList()
                                    ?: mutableListOf()

                            currentComments.add(
                                "$name: ${newComment.trim()}"
                            )

                            updatedComments[
                                selectedPostId
                            ] = currentComments

                            comments =
                                updatedComments

                            newComment = ""
                        }
                    }
                ) {
                    Text("Comment")
                }
            },

            dismissButton = {

                TextButton(
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
    posts: List<PeejeePost>,
    likes: Map<String, Int>,
    likedPostIds: Set<String>,
    loadingPosts: Boolean,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (PeejeePost) -> Unit,
    paddingValues: PaddingValues
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {

        item {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Welcome, $name 👋",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (loadingPosts) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator()
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }

            if (!loadingPosts && posts.isEmpty()) {

                Text(
                    text = "No posts yet. Create the first post!",
                    fontSize = 17.sp
                )

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = post.userName,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = post.text,
                        fontSize = 17.sp
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        TextButton(
                            onClick = {
                                onLike(post.id)
                            }
                        ) {

                            val count =
                                likes[post.id] ?: 0

                            Text(
                                if (
                                    likedPostIds.contains(
                                        post.id
                                    )
                                ) {
                                    "❤️ Liked $count"
                                } else {
                                    "♡ Like $count"
                                }
                            )
                        }

                        TextButton(
                            onClick = {
                                onComment(post.id)
                            }
                        ) {
                            Text("💬 Comment")
                        }

                        TextButton(
                            onClick = {
                                onShare(post)
                            }
                        ) {
                            Text("↗ Share")
                        }
                    }
                }
            }
        }
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

    val results = people.filter {
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

                    Text(
                        text = "👤",
                        fontSize = 28.sp
                    )

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Text(
                        text = person,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
                color = MaterialTheme.colorScheme.error
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

                val postId =
                    firestore
                        .collection("posts")
                        .document()
                        .id

                val postData =
                    hashMapOf<String, Any>(
                        "postId" to postId,
                        "userId" to currentUser.uid,
                        "userName" to (
                            currentUser.displayName
                                ?: "Peejee User"
                            ),
                        "text" to postText.trim(),
                        "likes" to 0,
                        "timestamp" to
                            System.currentTimeMillis()
                    )

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

                        firestore
                            .collection("posts")
                            .document(postId)
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

                        firestore
                            .collection("posts")
                            .document(postId)
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
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Peejee",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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

    val email =
        auth.currentUser?.email
            ?: "No email available"

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
            modifier = Modifier.size(120.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "👤",
                    fontSize = 65.sp
                )
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
                    fontWeight = FontWeight.Bold
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
                    fontWeight = FontWeight.Bold
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
                    fontWeight = FontWeight.Bold
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
                }
            },

            confirmButton = {

                TextButton(
                    enabled = !savingProfile,
                    onClick = {

                        if (
                            userId != null &&
                            editedName.isNotBlank()
                        ) {

                            savingProfile = true

                            val updates =
                                hashMapOf<String, Any>(
                                    "name" to
                                        editedName.trim(),
                                    "bio" to
                                        editedBio.trim()
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

                                    savingProfile = false

                                    showEditDialog =
                                        false
                                }
                                .addOnFailureListener {

                                    savingProfile = false
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
                        showEditDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
