package com.peejee.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        "home" -> HomeScreen(
            name = userName
        )
    }
}

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit
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
            onClick = { },
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
}@Composable
fun HomeScreen(
    name: String
) {    var selectedTab by remember {
        mutableStateOf(0)
    }

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(
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

            0 -> HomeFeed(
                name = name,
                paddingValues = paddingValues
            )

            1 -> SimplePage(
                title = "Search",
                message = "Search for people and posts."
            )

            2 -> SimplePage(
                title = "Create Post",
                message = "Create and share your first post."
            )

            3 -> SimplePage(
                title = "Messages",
                message = "Your conversations will appear here."
            )

            4 -> SimplePage(
                title = "Profile",
                message = "Your Peejee profile."
            )
        }
    }
}

@Composable
fun HomeFeed(
    name: String,
    paddingValues: PaddingValues
) {

    val posts = listOf(
        "Welcome to Peejee! 🎉",
        "Connect with people, share your moments and chat.",
        "Your Peejee community starts here."
    )

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
        }

        items(posts) { post ->

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
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = post,
                        fontSize = 17.sp
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row {

                        TextButton(
                            onClick = { }
                        ) {
                            Text("❤️ Like")
                        }

                        TextButton(
                            onClick = { }
                        ) {
                            Text("💬 Comment")
                        }

                        TextButton(
                            onClick = { }
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
fun SimplePage(
    title: String,
    message: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = message,
            fontSize = 17.sp
        )
    }
}
