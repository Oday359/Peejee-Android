package com.peejee.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun OtherUserProfilePage(
    person: PeejeePerson,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onMessage: (PeejeePerson) -> Unit
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var profileName by remember(person.uid) {
        mutableStateOf(person.name)
    }

    var bio by remember(person.uid) {
        mutableStateOf("")
    }

    var profilePhoto by remember(person.uid) {
        mutableStateOf("")
    }

    var coverPhoto by remember(person.uid) {
        mutableStateOf("")
    }

    var followersCount by remember(person.uid) {
        mutableStateOf(0)
    }

    var followingCount by remember(person.uid) {
        mutableStateOf(0)
    }

    var profilePosts by remember(person.uid) {
        mutableStateOf<List<PeejeePost>>(emptyList())
    }

    var loading by remember(person.uid) {
        mutableStateOf(true)
    }

    var errorMessage by remember(person.uid) {
        mutableStateOf("")
    }

    DisposableEffect(person.uid) {

        loading = true
        errorMessage = ""

        val profileRegistration =
            firestore
                .collection("users")
                .document(person.uid)
                .addSnapshotListener { document, error ->

                    if (error != null) {
                        loading = false
                        errorMessage =
                            error.message
                                ?: "Could not load this profile."
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {

                        profileName =
                            document.getString("name")
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: person.name

                        bio = document.getString("bio") ?: ""

                        profilePhoto =
                            document.getString("profilePhoto") ?: ""

                        coverPhoto =
                            document.getString("coverPhoto") ?: ""

                        followersCount =
                            document.getLong("followersCount")?.toInt() ?: 0

                        followingCount =
                            document.getLong("followingCount")?.toInt() ?: 0

                        loading = false

                    } else {
                        loading = false
                        errorMessage =
                            "This profile could not be found."
                    }
                }

        val postsRegistration =
            firestore
                .collection("posts")
                .whereEqualTo("userId", person.uid)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        profilePosts =
                            snapshot.documents
                                .mapNotNull { document ->

                                    PeejeePost(
                                        id = document.id,
                                        userId =
                                            document.getString("userId")
                                                ?: person.uid,
                                        userName =
                                            document.getString("userName")
                                                ?.takeIf { it.isNotBlank() }
                                                ?: profileName,
                                        text =
                                            document.getString("text") ?: "",
                                        timestamp =
                                            document.getLong("timestamp") ?: 0L,
                                        mediaUrl =
                                            document.getString("mediaUrl") ?: "",
                                        mediaType =
                                            document.getString("mediaType") ?: "",
                                        likeCount =
                                            document.getLong("likes")?.toInt() ?: 0,
                                        likedBy =
                                            getLikedBy(document),
                                        commentCount =
                                            document.getLong("commentCount")
                                                ?.toInt() ?: 0,
                                        shareCount =
                                            document.getLong("shareCount")
                                                ?.toInt() ?: 0,
                                        sharedFromPostId =
                                            document.getString("sharedFromPostId")
                                                ?: "",
                                        sharedFromUserName =
                                            document.getString("sharedFromUserName")
                                                ?: "",
                                        sharedFromText =
                                            document.getString("sharedFromText")
                                                ?: ""
                                    )
                                }
                                .sortedByDescending {
                                    it.timestamp
                                }
                    }
                }

        onDispose {
            profileRegistration.remove()
            postsRegistration.remove()
        }
    }

    val profileBitmap = remember(profilePhoto) {
        base64ToBitmap(profilePhoto)
    }

    val coverBitmap = remember(coverPhoto) {
        base64ToBitmap(coverPhoto)
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
                    vertical = 6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {
                Text("← Back")
            }

            Text(
                "Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider()

        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (errorMessage.isNotBlank()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(errorMessage)

                Spacer(
                    Modifier.height(16.dp)
                )

                Button(
                    onClick = onBack
                ) {
                    Text("Go Back")
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                            )
                    ) {

                        if (coverBitmap != null) {

                            Image(
                                bitmap =
                                    coverBitmap.asImageBitmap(),
                                contentDescription =
                                    "Cover photo",
                                modifier =
                                    Modifier.fillMaxSize(),
                                contentScale =
                                    ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(
                                    start = 20.dp,
                                    top = 130.dp
                                )
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            if (profileBitmap != null) {

                                Image(
                                    bitmap =
                                        profileBitmap
                                            .asImageBitmap(),
                                    contentDescription =
                                        "Profile photo",
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(
                                            CircleShape
                                        ),
                                    contentScale =
                                        ContentScale.Crop
                                )

                            } else {

                                DefaultProfileIcon(
                                    size = 84
                                )
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(52.dp)
                    )

                    Column(
                        modifier =
                            Modifier.padding(
                                horizontal = 20.dp
                            )
                    ) {

                        Text(
                            profileName,
                            fontSize = 27.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        if (bio.isNotBlank()) {

                            Text(
                                bio,
                                fontSize = 16.sp
                            )

                            Spacer(
                                Modifier.height(14.dp)
                            )
                        }

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceEvenly
                        ) {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    followersCount
                                        .toString(),
                                    fontSize = 20.sp,
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
                                    followingCount
                                        .toString(),
                                    fontSize = 20.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text("Following")
                            }
                        }

                        Spacer(
                            Modifier.height(18.dp)
                        )

                        Button(
                            onClick = {
                                onMessage(
                                    person.copy(
                                        name = profileName
                                    )
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text("💬 Message")
                        }

                        Spacer(
                            Modifier.height(22.dp)
                        )

                        HorizontalDivider()

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        Text(
                            "Posts (${profilePosts.size})",
                            fontSize = 21.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )
                    }
                }

                if (profilePosts.isEmpty()) {

                    item {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                "No posts yet."
                            )
                        }
                    }

                } else {

                    items(
                        profilePosts,
                        key = { post ->
                            post.id
                        }
                    ) { post ->

                        ProfilePostCard(post)
                    }
                }

                item {

                    Spacer(
                        Modifier.height(30.dp)
                    )
                }
            }
        }
    }
}
