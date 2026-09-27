package com.peejee.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
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
                        errorMessage = "Please enter your full name."
                    }

                    email.isBlank() -> {
                        errorMessage = "Please enter your email."
                    }

                    password.length < 6 -> {
                        errorMessage =
                            "Password must be at least 6 characters."
                    }

                    password != confirmPassword -> {
                        errorMessage = "Passwords do not match."
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

                                    val profile = hashMapOf(
                                        "uid" to user.uid,
                                        "name" to name.trim(),
                                        "email" to email.trim()
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
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
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
                        errorMessage = "Please enter your email."
                    }

                    password.isBlank() -> {
                        errorMessage = "Please enter your password."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    name: String
) {

    var selectedTab by remember {
        mutableStateOf(0)
    }

    var posts by remember {
        mutableStateOf(
            listOf(
                "Welcome to Peejee! 🎉",
                "Connect with people, share your moments and chat.",
                "Your Peejee community starts here."
            )
        )
    }

    var likes by remember {
        mutableStateOf(
            List(posts.size) { 0 }
        )
    }

    var liked by remember {
        mutableStateOf(
            List(posts.size) { false }
        )
    }

    var comments by remember {
        mutableStateOf(
            List(posts.size) { mutableListOf<String>() }
        )
    }

    var showCommentDialog by remember {
        mutableStateOf(false)
    }

    var selectedPost by remember {
        mutableStateOf(0)
    }

    var newComment by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

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
                    likes = likes,
                    liked = liked,
                    onLike = { index ->

                        val newLiked = liked.toMutableList()
                        val newLikes = likes.toMutableList()

                        if (newLiked[index]) {
                            newLiked[index] = false
                            newLikes[index] =
                                (newLikes[index] - 1).coerceAtLeast(0)
                        } else {
                            newLiked[index] = true
                            newLikes[index] =
                                newLikes[index] + 1
                        }

                        liked = newLiked
                        likes = newLikes
                    },
                    onComment = { index ->

                        selectedPost = index
                        newComment = ""
                        showCommentDialog = true
                    },
                    onShare = { index ->

                        val shareText =
                            "Check this out on Peejee:\n\n${posts[index]}"

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
                onPostCreated = { post ->

                    posts = posts + post
                    likes = likes + 0
                    liked = liked + false
                    comments = comments + mutableListOf()
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
                showCommentDialog = false
            },

            title = {
                Text("Comments")
            },

            text = {

                Column {

                    if (comments[selectedPost].isEmpty()) {

                        Text(
                            "No comments yet. Be the first!"
                        )

                    } else {

                        comments[selectedPost].forEach { comment ->

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

                            val newComments =
                                comments.toMutableList()

                            newComments[selectedPost] =
                                newComments[selectedPost].toMutableList()
                                    .apply {
                                        add(
                                            "$name: ${newComment.trim()}"
                                        )
                                    }

                            comments = newComments
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
    posts: List<String>,
    likes: List<Int>,
    liked: List<Boolean>,
    onLike: (Int) -> Unit,
    onComment: (Int) -> Unit,
    onShare: (Int) -> Unit,
    paddingValues: PaddingValues
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {

        item {

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome, $name 👋",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        itemsIndexed(posts) { index, post ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = post,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        TextButton(
                            onClick = {
                                onLike(index)
                            }
                        ) {

                            Text(
                                if (liked[index]) {
                                    "❤️ Liked ${likes[index]}"
                                } else {
                                    "♡ Like ${likes[index]}"
                                }
                            )
                        }

                        TextButton(
                            onClick = {
                                onComment(index)
                            }
                        ) {
                            Text("💬 Comment")
                        }

                        TextButton(
                            onClick = {
                                onShare(index)
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

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

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
                    verticalAlignment = Alignment.CenterVertically
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
    onPostCreated: (String) -> Unit,
    paddingValues: PaddingValues
) {

    var postText by remember {
        mutableStateOf("")
    }

    var selectedMedia by remember {
        mutableStateOf<android.net.Uri?>(null)
    }

    var mediaType by remember {
        mutableStateOf("")
    }

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                selectedMedia = uri
                mediaType = "photo"
            }
        }

    val videoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            if (mediaType == "photo") {
                                "Photo selected"
                            } else {
                                "Video selected"
                            },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = selectedMedia.toString(),
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

        Button(
            onClick = {

                if (postText.isNotBlank()) {

                    onPostCreated(
                        postText.trim()
                    )

                    postText = ""
                    selectedMedia = null
                    mediaType = ""
                }

            },
            enabled =
                postText.isNotBlank() ||
                selectedMedia != null,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Publish Post")
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

        Spacer(modifier = Modifier.height(20.dp))

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

    val email =
        auth.currentUser?.email
            ?: "No email available"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "👤",
            fontSize = 70.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = name,
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
            modifier = Modifier.height(30.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text("Name: $name")

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text("Email: $email")
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedButton(
            onClick = {
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Profile")
        }
    }
}
