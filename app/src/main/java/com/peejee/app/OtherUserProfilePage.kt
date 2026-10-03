package com.peejee.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.os.Environment
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun OtherUserProfilePage(
person: PeejeePerson,
paddingValues: PaddingValues,
onBack: () -> Unit,
onMessage: (PeejeePerson) -> Unit
) {
val firestore = remember { FirebaseFirestore.getInstance() }
val auth = remember { FirebaseAuth.getInstance() }

var profileName by remember(person.uid) { mutableStateOf(person.name) }  
var bio by remember(person.uid) { mutableStateOf("") }  
var profilePhoto by remember(person.uid) { mutableStateOf("") }  
var coverPhoto by remember(person.uid) { mutableStateOf("") }  
var followersCount by remember(person.uid) { mutableStateOf(0) }  
var followingCount by remember(person.uid) { mutableStateOf(0) }  
var profilePosts by remember(person.uid) {  
    mutableStateOf<List<PeejeePost>>(emptyList())  
}  
var loading by remember(person.uid) { mutableStateOf(true) }  
var errorMessage by remember(person.uid) { mutableStateOf("") }  

DisposableEffect(person.uid) {  
    loading = true  
    errorMessage = ""  

    val profileRegistration = firestore  
        .collection("users")  
        .document(person.uid)  
        .addSnapshotListener { document, error ->  
            if (error != null) {  
                loading = false  
                errorMessage = error.message ?: "Could not load this profile."  
                return@addSnapshotListener  
            }  

            if (document != null && document.exists()) {  
                profileName = document.getString("name")  
                    ?.trim()  
                    ?.takeIf { it.isNotBlank() }  
                    ?: person.name  
                bio = document.getString("bio") ?: ""  
                profilePhoto = document.getString("profilePhoto") ?: ""  
                coverPhoto = document.getString("coverPhoto") ?: ""  
                followersCount =  
                    document.getLong("followersCount")?.toInt() ?: 0  
                followingCount =  
                    document.getLong("followingCount")?.toInt() ?: 0  
                loading = false  
            } else {  
                loading = false  
                errorMessage = "This profile could not be found."  
            }  
        }  

    val postsRegistration = firestore  
        .collection("posts")  
        .whereEqualTo("userId", person.uid)  
        .addSnapshotListener { snapshot, error ->  
            if (error != null) {  
                return@addSnapshotListener  
            }  

            if (snapshot != null) {  
                profilePosts = snapshot.documents  
                    .mapNotNull { document ->  
                        PeejeePost(  
                            id = document.id,  
                            userId = document.getString("userId") ?: person.uid,  
                            userName = document.getString("userName")  
                                ?.takeIf { it.isNotBlank() }  
                                ?: profileName,  
                            text = document.getString("text") ?: "",  
                            timestamp = document.getLong("timestamp") ?: 0L,  
                            mediaUrl = document.getString("mediaUrl") ?: "",  
                            mediaType = document.getString("mediaType") ?: "",  
                            likeCount = document.getLong("likes")?.toInt() ?: 0,  
                            likedBy = getLikedBy(document),  
                            commentCount =  
                                document.getLong("commentCount")?.toInt() ?: 0,  
                            shareCount =  
                                document.getLong("shareCount")?.toInt() ?: 0,  
                            sharedFromPostId =  
                                document.getString("sharedFromPostId") ?: "",  
                            sharedFromUserName =  
                                document.getString("sharedFromUserName") ?: "",  
                            sharedFromText =  
                                document.getString("sharedFromText") ?: ""  
                        )  
                    }  
                    .sortedByDescending { it.timestamp }  
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
            .padding(horizontal = 12.dp, vertical = 6.dp),  
        verticalAlignment = Alignment.CenterVertically  
    ) {  
        TextButton(onClick = onBack) {  
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
            contentAlignment = Alignment.Center  
        ) {  
            CircularProgressIndicator()  
        }  
    } else if (errorMessage.isNotBlank()) {  
        Column(  
            modifier = Modifier  
                .fillMaxSize()  
                .padding(24.dp),  
            horizontalAlignment = Alignment.CenterHorizontally,  
            verticalArrangement = Arrangement.Center  
        ) {  
            Text(errorMessage)  
            Spacer(Modifier.height(16.dp))  
            Button(onClick = onBack) {  
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)  
                ) {  
                    if (coverBitmap != null) {  
                        Image(  
                            bitmap = coverBitmap.asImageBitmap(),  
                            contentDescription = "Cover photo",  
                            modifier = Modifier.fillMaxSize(),  
                            contentScale = ContentScale.Crop  
                        )  
                    }  

                    Box(  
                        modifier = Modifier  
                            .padding(start = 20.dp, top = 130.dp)  
                            .size(92.dp)  
                            .clip(CircleShape)  
                            .background(MaterialTheme.colorScheme.surface),  
                        contentAlignment = Alignment.Center  
                    ) {  
                        if (profileBitmap != null) {  
                            Image(  
                                bitmap = profileBitmap.asImageBitmap(),  
                                contentDescription = "Profile photo",  
                                modifier = Modifier  
                                    .size(84.dp)  
                                    .clip(CircleShape),  
                                contentScale = ContentScale.Crop  
                            )  
                        } else {  
                            DefaultProfileIcon(size = 84)  
                        }  
                    }  
                }  

                Spacer(Modifier.height(52.dp))  

                Column(  
                    modifier = Modifier.padding(horizontal = 20.dp)  
                ) {  
                    Text(  
                        profileName,  
                        fontSize = 27.sp,  
                        fontWeight = FontWeight.Bold  
                    )  

                    Spacer(Modifier.height(6.dp))  

                    if (bio.isNotBlank()) {  
                        Text(bio, fontSize = 16.sp)  
                        Spacer(Modifier.height(14.dp))  
                    }  

                    Row(  
                        modifier = Modifier.fillMaxWidth(),  
                        horizontalArrangement = Arrangement.SpaceEvenly  
                    ) {  
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {  
                            Text(  
                                followersCount.toString(),  
                                fontSize = 20.sp,  
                                fontWeight = FontWeight.Bold  
                            )  
                            Text("Followers")  
                        }  

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {  
                            Text(  
                                followingCount.toString(),  
                                fontSize = 20.sp,  
                                fontWeight = FontWeight.Bold  
                            )  
                            Text("Following")  
                        }  
                    }  

                    Spacer(Modifier.height(18.dp))  

                    Button(  
                        onClick = {  
                            onMessage(person.copy(name = profileName))  
                        },  
                        modifier = Modifier.fillMaxWidth()  
                    ) {  
                        Text("💬 Message")  
                    }  

                    Spacer(Modifier.height(22.dp))  
                    HorizontalDivider()  
                    Spacer(Modifier.height(16.dp))  

                    Text(  
                        "Posts (${profilePosts.size})",  
                        fontSize = 21.sp,  
                        fontWeight = FontWeight.Bold  
                    )  

                    Spacer(Modifier.height(8.dp))  
                }  
            }  

            if (profilePosts.isEmpty()) {  
                item {  
                    Column(  
                        modifier = Modifier  
                            .fillMaxWidth()  
                            .padding(30.dp),  
                        horizontalAlignment = Alignment.CenterHorizontally  
                    ) {  
                        Text("No posts yet.")  
                    }  
                }  
            } else {  
                items(  
                    profilePosts,  
                    key = { post -> post.id }  
                ) { post ->  
                    OtherProfilePostCard(  
                        post = post,  
                        profileName = profileName  
                    )  
                }  
            }  

            item {  
                Spacer(Modifier.height(30.dp))  
            }  
        }  
    }  
}

}

@Composable
private fun OtherProfilePostCard(
post: PeejeePost,
profileName: String
) {
val context = LocalContext.current
val firestore = remember { FirebaseFirestore.getInstance() }
val auth = remember { FirebaseAuth.getInstance() }
val currentUserId = auth.currentUser?.uid ?: ""

var liked by remember(post.id, currentUserId) {  
    mutableStateOf(post.likedBy.containsKey(currentUserId))  
}  
var likeCount by remember(post.id) { mutableStateOf(post.likeCount) }  
var shareCount by remember(post.id) { mutableStateOf(post.shareCount) }  
var commentCount by remember(post.id) { mutableStateOf(post.commentCount) }  

var showComments by remember(post.id) { mutableStateOf(false) }  
var showPostMenu by remember(post.id) { mutableStateOf(false) }  
var showShareDialog by remember(post.id) { mutableStateOf(false) }  
var message by remember(post.id) { mutableStateOf("") }  

LaunchedEffect(  
    post.likeCount,  
    post.shareCount,  
    post.commentCount,  
    post.likedBy,  
    currentUserId  
) {  
    likeCount = post.likeCount  
    shareCount = post.shareCount  
    commentCount = post.commentCount  
    liked = post.likedBy.containsKey(currentUserId)  
}  

Card(  
    modifier = Modifier  
        .fillMaxWidth()  
        .padding(horizontal = 8.dp, vertical = 6.dp),  
    shape = RoundedCornerShape(18.dp)  
) {  
    Column(  
        modifier = Modifier.fillMaxWidth()  
    ) {  
        if (post.sharedFromPostId.isNotBlank()) {  
            Text(  
                "🔁 $profileName shared a post",  
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
                .padding(14.dp),  
            verticalAlignment = Alignment.CenterVertically  
        ) {  
            OtherProfilePostAvatar(  
                userId = post.userId,  
                size = 46  
            )  

            Spacer(Modifier.size(10.dp))  

            Column(modifier = Modifier.weight(1f)) {  
                Text(  
                    post.userName,  
                    fontSize = 17.sp,  
                    fontWeight = FontWeight.Bold  
                )  
                Text(  
                    formatPostTime(post.timestamp),  
                    fontSize = 12.sp  
                )  
            }  

            TextButton(onClick = { showPostMenu = true }) {  
                Text("⋮", fontSize = 25.sp)  
            }  
        }  

        if (post.sharedFromPostId.isNotBlank()) {  
            Card(  
                modifier = Modifier  
                    .fillMaxWidth()  
                    .padding(horizontal = 12.dp),  
                shape = RoundedCornerShape(14.dp)  
            ) {  
                Column(modifier = Modifier.padding(14.dp)) {  
                    Text(  
                        "Original post by ${post.sharedFromUserName}",  
                        fontSize = 13.sp,  
                        fontWeight = FontWeight.Bold  
                    )  

                    if (post.sharedFromText.isNotBlank()) {  
                        Spacer(Modifier.height(6.dp))  
                        Text(post.sharedFromText, fontSize = 15.sp)  
                    }  
                }  
            }  

            Spacer(Modifier.height(10.dp))  
        }  

        OtherProfileMedia(  
            post = post,  
            onDownload = {  
                val result = downloadPeejeeMedia(  
                    context = context,  
                    mediaUrl = post.mediaUrl,  
                    mediaType = post.mediaType  
                )  
                message = result  
            }  
        )  

        if (post.text.isNotBlank()) {  
            Spacer(Modifier.height(10.dp))  
            Text(  
                post.text,  
                fontSize = 16.sp,  
                modifier = Modifier.padding(horizontal = 16.dp)  
            )  
        }  

        Spacer(Modifier.height(14.dp))  

        Row(  
            modifier = Modifier  
                .fillMaxWidth()  
                .padding(horizontal = 12.dp, vertical = 4.dp),  
            horizontalArrangement = Arrangement.SpaceEvenly  
        ) {  
            ProfilePostAction(  
                icon = if (liked) "❤️" else "♡",  
                label = likeCount.toString(),  
                onClick = {  
                    if (currentUserId.isBlank()) return@ProfilePostAction  

                    val postRef = firestore  
                        .collection("posts")  
                        .document(post.id)  

                    firestore.runTransaction { transaction ->  
                        val snapshot = transaction.get(postRef)  
                        val likedBy =  
                            snapshot.get("likedBy") as? Map<*, *>  
                        val currentLiked =  
                            likedBy?.containsKey(currentUserId) == true  

                        val updates = HashMap<String, Any>()  

                        if (currentLiked) {  
                            transaction.update(  
                                postRef,  
                                "likedBy.$currentUserId",  
                                FieldValue.delete()  
                            )  
                            val currentLikes =  
                                snapshot.getLong("likes")?.toInt() ?: 0  
                            updates["likes"] = (currentLikes - 1).coerceAtLeast(0)  
                        } else {  
                            transaction.update(  
                                postRef,  
                                "likedBy.$currentUserId",  
                                true  
                            )  
                            val currentLikes =  
                                snapshot.getLong("likes")?.toInt() ?: 0  
                            updates["likes"] = currentLikes + 1  
                        }  

                        transaction.update(postRef, updates)  
                        null  
                    }.addOnSuccessListener {  
                        liked = !liked  
                        likeCount =  
                            if (liked) likeCount + 1  
                            else (likeCount - 1).coerceAtLeast(0)  
                    }  
                }  
            )  

            ProfilePostAction(  
                icon = "💬",  
                label = commentCount.toString(),  
                onClick = {  
                    showComments = true  
                }  
            )  

            ProfilePostAction(  
                icon = "↗️",  
                label = shareCount.toString(),  
                onClick = {  
                    showShareDialog = true  
                }  
            )  

            ProfilePostAction(  
                icon = "⬇️",  
                label = "Save",  
                onClick = {  
                    val result = downloadPeejeeMedia(  
                        context = context,  
                        mediaUrl = post.mediaUrl,  
                        mediaType = post.mediaType  
                    )  
                    message = if (post.mediaUrl.isBlank()) {  
                        "This post has no downloadable media."  
                    } else {  
                        result  
                    }  
                }  
            )  

            ProfilePostAction(  
                icon = "⋮",  
                label = "More",  
                onClick = {  
                    showPostMenu = true  
                }  
            )  
        }  

        if (message.isNotBlank()) {  
            Text(  
                message,  
                modifier = Modifier.padding(  
                    horizontal = 16.dp,  
                    vertical = 8.dp  
                ),  
                color = MaterialTheme.colorScheme.primary,  
                fontWeight = FontWeight.Bold  
            )  
        }  

        Spacer(Modifier.height(10.dp))  
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

if (showComments) {  
    OtherProfileCommentsDialog(  
        postId = post.id,  
        onDismiss = {  
            showComments = false  
        },  
        onCountChanged = {  
            commentCount = it  
        }  
    )  
}  

if (showShareDialog) {  
    AlertDialog(  
        onDismissRequest = {  
            showShareDialog = false  
        },  
        title = {  
            Text("Share Post")  
        },  
        text = {  
            Text(  
                "Share this post with other people or share it outside Peejee."  
            )  
        },  
        confirmButton = {  
            TextButton(  
                onClick = {  
                    showShareDialog = false  

                    val newPostRef = firestore  
                        .collection("posts")  
                        .document()  

                    firestore  
                        .collection("users")  
                        .document(currentUserId)  
                        .get()  
                        .addOnSuccessListener { userDocument ->  
                            val currentName =  
                                userDocument.getString("name")  
                                    ?: "Peejee User"  

                            val sharedPostData =  
                                hashMapOf<String, Any>(  
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
                                sharedPostData["mediaUrl"] = post.mediaUrl  
                                sharedPostData["mediaType"] = post.mediaType  
                            }  

                            newPostRef  
                                .set(sharedPostData)  
                                .addOnSuccessListener {  
                                    firestore  
                                        .collection("posts")  
                                        .document(post.id)  
                                        .update(  
                                            "shareCount",  
                                            FieldValue.increment(1)  
                                        )  

                                    shareCount += 1  

                                    val shareText =  
                                        "Check out this post from ${post.userName} on Peejee:\n\n${post.text}"  

                                    val intent = Intent(Intent.ACTION_SEND).apply {  
                                        type = "text/plain"  
                                        putExtra(Intent.EXTRA_TEXT, shareText)  
                                    }  

                                    context.startActivity(  
                                        Intent.createChooser(  
                                            intent,  
                                            "Share Peejee Post"  
                                        )  
                                    )  
                                }  
                        }  
                }  
            ) {  
                Text("Share")  
            }  
        },  
        dismissButton = {  
            TextButton(  
                onClick = {  
                    showShareDialog = false  
                }  
            ) {  
                Text("Cancel")  
            }  
        }  
    )  
}

}

@Composable
private fun OtherProfileMedia(
post: PeejeePost,
onDownload: () -> Unit
) {
if (post.mediaUrl.isBlank()) {
return
}

val context = LocalContext.current  
val uri = remember(post.mediaUrl) {  
    runCatching { Uri.parse(post.mediaUrl) }.getOrNull()  
}  

val isPhoto =  
    post.mediaType.lowercase().contains("image") ||  
        post.mediaType.lowercase() == "photo"  

if (isPhoto && uri != null) {  
    val bitmap = remember(post.mediaUrl) {  
        runCatching {  
            context.contentResolver.openInputStream(uri)?.use {  
                BitmapFactory.decodeStream(it)  
            }  
        }.getOrNull()  
    }  

    if (bitmap != null) {  
        Image(  
            bitmap = bitmap.asImageBitmap(),  
            contentDescription = "Post photo",  
            modifier = Modifier  
                .fillMaxWidth()  
                .height(420.dp),  
            contentScale = ContentScale.Crop  
        )  
    } else {  
        MediaUnavailableCard(  
            icon = "🖼️",  
            text = "Photo is not available on this device.",  
            onDownload = onDownload  
        )  
    }  
} else if (uri != null) {  
    AndroidView(  
        modifier = Modifier  
            .fillMaxWidth()  
            .height(420.dp),  
        factory = { viewContext ->  
            VideoView(viewContext).apply {  
                layoutParams = ViewGroup.LayoutParams(  
                    ViewGroup.LayoutParams.MATCH_PARENT,  
                    ViewGroup.LayoutParams.MATCH_PARENT  
                )  
                setMediaController(MediaController(viewContext))  
                setVideoURI(uri)  
                setOnPreparedListener { player ->  
                    player.isLooping = false  
                }  
            }  
        },  
        update = { videoView ->  
            videoView.setVideoURI(uri)  
        }  
    )  

    TextButton(  
        onClick = onDownload,  
        modifier = Modifier.fillMaxWidth()  
    ) {  
        Text("⬇️ Download video")  
    }  
} else {  
    MediaUnavailableCard(  
        icon = "🎥",  
        text = "Video is not available.",  
        onDownload = onDownload  
    )  
}

}

@Composable
private fun MediaUnavailableCard(
icon: String,
text: String,
onDownload: () -> Unit
) {
Column(
modifier = Modifier
.fillMaxWidth()
.padding(14.dp),
horizontalAlignment = Alignment.CenterHorizontally
) {
Box(
modifier = Modifier
.fillMaxWidth()
.height(220.dp)
.background(MaterialTheme.colorScheme.surfaceVariant),
contentAlignment = Alignment.Center
) {
Text(icon, fontSize = 70.sp)
}

Spacer(Modifier.height(6.dp))  

    Text(text)  

    TextButton(onClick = onDownload) {  
        Text("⬇️ Download")  
    }  
}

}

@Composable
private fun OtherProfilePostAvatar(
userId: String,
size: Int
) {
var profilePhoto by remember(userId) { mutableStateOf("") }

LaunchedEffect(userId) {  
    if (userId.isBlank()) {  
        profilePhoto = ""  
        return@LaunchedEffect  
    }  

    FirebaseFirestore.getInstance()  
        .collection("users")  
        .document(userId)  
        .get()  
        .addOnSuccessListener { document ->  
            profilePhoto = document.getString("profilePhoto") ?: ""  
        }  
}  

val bitmap = remember(profilePhoto) {  
    base64ToBitmap(profilePhoto)  
}  

Box(  
    modifier = Modifier  
        .size(size.dp)  
        .clip(CircleShape)  
        .background(MaterialTheme.colorScheme.surfaceVariant),  
    contentAlignment = Alignment.Center  
) {  
    if (bitmap != null) {  
        Image(  
            bitmap = bitmap.asImageBitmap(),  
            contentDescription = "Profile photo",  
            modifier = Modifier  
                .fillMaxSize()  
                .clip(CircleShape),  
            contentScale = ContentScale.Crop  
        )  
    } else {  
        DefaultProfileIcon(size = size)  
    }  
}

}

@Composable
private fun ProfilePostAction(
icon: String,
label: String,
onClick: () -> Unit
) {
Column(
modifier = Modifier.clickable { onClick() },
horizontalAlignment = Alignment.CenterHorizontally
) {
Text(icon, fontSize = 24.sp)
Text(
label,
fontSize = 11.sp,
fontWeight = FontWeight.Bold
)
}
}

@Composable
private fun OtherProfileCommentsDialog(
postId: String,
onDismiss: () -> Unit,
onCountChanged: (Int) -> Unit
) {
val firestore = remember { FirebaseFirestore.getInstance() }
val auth = remember { FirebaseAuth.getInstance() }

var comments by remember(postId) {  
    mutableStateOf<List<PeejeeComment>>(emptyList())  
}  
var newComment by remember(postId) { mutableStateOf("") }  
var loading by remember(postId) { mutableStateOf(true) }  
var sending by remember(postId) { mutableStateOf(false) }  
var error by remember(postId) { mutableStateOf("") }  

DisposableEffect(postId) {  
    val registration = firestore  
        .collection("posts")  
        .document(postId)  
        .collection("comments")  
        .orderBy("timestamp", Query.Direction.ASCENDING)  
        .addSnapshotListener { snapshot, exception ->  
            if (exception != null) {  
                loading = false  
                error = exception.message ?: "Could not load comments."  
                return@addSnapshotListener  
            }  

            comments = snapshot?.documents?.mapNotNull { document ->  
                PeejeeComment(  
                    id = document.id,  
                    userId = document.getString("userId") ?: "",  
                    userName = document.getString("userName") ?: "Peejee User",  
                    text = document.getString("text") ?: "",  
                    timestamp = document.getLong("timestamp") ?: 0L  
                )  
            } ?: emptyList()  

            loading = false  
            onCountChanged(comments.size)  
        }  

    onDispose {  
        registration.remove()  
    }  
}  

AlertDialog(  
    onDismissRequest = {  
        if (!sending) onDismiss()  
    },  
    title = {  
        Text("Comments")  
    },  
    text = {  
        Column {  
            if (loading) {  
                Row(  
                    modifier = Modifier.fillMaxWidth(),  
                    horizontalArrangement = Arrangement.Center  
                ) {  
                    CircularProgressIndicator()  
                }  
            } else if (comments.isEmpty()) {  
                Text("No comments yet. Be the first!")  
            } else {  
                LazyColumn(  
                    modifier = Modifier  
                        .fillMaxWidth()  
                        .heightIn(max = 300.dp)  
                ) {  
                    items(comments) { comment ->  
                        Column(  
                            modifier = Modifier  
                                .fillMaxWidth()  
                                .padding(vertical = 7.dp)  
                        ) {  
                            Text(  
                                comment.userName,  
                                fontWeight = FontWeight.Bold,  
                                fontSize = 14.sp  
                            )  
                            Text(  
                                comment.text,  
                                fontSize = 15.sp  
                            )  
                            Text(  
                                formatPostTime(comment.timestamp),  
                                fontSize = 11.sp  
                            )  
                        }  
                    }  
                }  
            }  

            if (error.isNotBlank()) {  
                Spacer(Modifier.height(8.dp))  
                Text(  
                    error,  
                    color = MaterialTheme.colorScheme.error  
                )  
            }  

            Spacer(Modifier.height(10.dp))  

            OutlinedTextField(  
                value = newComment,  
                onValueChange = {  
                    newComment = it  
                    error = ""  
                },  
                label = {  
                    Text("Write a comment")  
                },  
                modifier = Modifier.fillMaxWidth(),  
                enabled = !sending  
            )  
        }  
    },  
    confirmButton = {  
        Button(  
            onClick = {  
                val currentUser = auth.currentUser  

                if (currentUser == null) {  
                    error = "Please log in again."  
                    return@Button  
                }  

                if (newComment.isBlank()) {  
                    error = "Please write a comment."  
                    return@Button  
                }  

                sending = true  

                firestore  
                    .collection("users")  
                    .document(currentUser.uid)  
                    .get()  
                    .addOnSuccessListener { userDocument ->  
                        val userName =  
                            userDocument.getString("name")  
                                ?: "Peejee User"  

                        val commentReference = firestore  
                            .collection("posts")  
                            .document(postId)  
                            .collection("comments")  
                            .document()  

                        val commentData =  
                            hashMapOf<String, Any>(  
                                "commentId" to commentReference.id,  
                                "postId" to postId,  
                                "userId" to currentUser.uid,  
                                "userName" to userName,  
                                "text" to newComment.trim(),  
                                "timestamp" to System.currentTimeMillis()  
                            )  

                        commentReference  
                            .set(commentData)  
                            .addOnSuccessListener {  
                                firestore  
                                    .collection("posts")  
                                    .document(postId)  
                                    .update(  
                                        "commentCount",  
                                        FieldValue.increment(1)  
                                    )  

                                newComment = ""  
                                sending = false  
                            }  
                            .addOnFailureListener { exception ->  
                                sending = false  
                                error =  
                                    exception.message  
                                        ?: "Could not send comment."  
                            }  
                    }  
                    .addOnFailureListener { exception ->  
                        sending = false  
                        error =  
                            exception.message  
                                ?: "Could not load your account."  
                    }  
            },  
            enabled = !sending  
        ) {  
            Text(if (sending) "Sending..." else "Comment")  
        }  
    },  
    dismissButton = {  
        TextButton(  
            onClick = {  
                if (!sending) onDismiss()  
            }  
        ) {  
            Text("Close")  
        }  
    }  
)

}

private fun downloadPeejeeMedia(
context: Context,
mediaUrl: String,
mediaType: String
): String {
if (mediaUrl.isBlank()) {
return "This post has no downloadable media."
}

return try {  
    val sourceUri = Uri.parse(mediaUrl)  
    val resolver = context.contentResolver  
    val isPhoto =  
        mediaType.lowercase().contains("image") ||  
            mediaType.lowercase() == "photo"  

    val extension = if (isPhoto) "jpg" else "mp4"  
    val mimeType = if (isPhoto) "image/jpeg" else "video/mp4"  
    val folder =  
        if (isPhoto) {  
            Environment.DIRECTORY_PICTURES  
        } else {  
            Environment.DIRECTORY_MOVIES  
        }  

    val values = ContentValues().apply {  
        put(  
            MediaStore.MediaColumns.DISPLAY_NAME,  
            "Peejee_${System.currentTimeMillis()}.$extension"  
        )  
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)  

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  
            put(  
                MediaStore.MediaColumns.RELATIVE_PATH,  
                "$folder/Peejee"  
            )  
            put(  
                MediaStore.MediaColumns.IS_PENDING,  
                1  
            )  
        }  
    }  

    val collection =  
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  
            if (isPhoto) {  
                MediaStore.Images.Media.getContentUri(  
                    MediaStore.VOLUME_EXTERNAL_PRIMARY  
                )  
            } else {  
                MediaStore.Video.Media.getContentUri(  
                    MediaStore.VOLUME_EXTERNAL_PRIMARY  
                )  
            }  
        } else {  
            if (isPhoto) {  
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI  
            } else {  
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI  
            }  
        }  

    val destination = resolver.insert(collection, values)  
        ?: return "Could not create the download file."  

    resolver.openInputStream(sourceUri)?.use { input ->  
        resolver.openOutputStream(destination)?.use { output ->  
            input.copyTo(output)  
        } ?: throw Exception("Could not write the download.")  
    } ?: throw Exception(  
        "This media file is not available on this device."  
    )  

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  
        val doneValues = ContentValues().apply {  
            put(MediaStore.MediaColumns.IS_PENDING, 0)  
        }  
        resolver.update(destination, doneValues, null, null)  
    }  

    if (isPhoto) {  
        "Photo downloaded to your Pictures/Peejee folder."  
    } else {  
        "Video downloaded to your Movies/Peejee folder."  
    }  
} catch (exception: Exception) {  
    "Download failed: ${exception.message ?: "media is unavailable"}"  
}

}
