package com.peejee.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private data class PeejeeDiscoverUser(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val profilePhoto: String = "",
    val followersCount: Long = 0L,
    val updatedAt: Long = 0L
)

private data class PeejeeTrendingTag(
    val tag: String,
    val count: Int
)

@Composable
fun PeejeeDiscoverPage(
    onUserClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId = remember {
        auth.currentUser?.uid ?: ""
    }

    val listState = rememberLazyListState()

    var users by remember {
        mutableStateOf<List<PeejeeDiscoverUser>>(emptyList())
    }

    var posts by remember {
        mutableStateOf<List<PeejeePost>>(emptyList())
    }

    var trendingTags by remember {
        mutableStateOf<List<PeejeeTrendingTag>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var pullDistance by remember {
        mutableFloatStateOf(0f)
    }

    var selectedDiscoverPost by remember {
        mutableStateOf<PeejeePost?>(null)
    }

    var commentsPost by remember {
        mutableStateOf<PeejeePost?>(null)
    }

    var showPostOptions by remember {
        mutableStateOf(false)
    }

    fun replacePost(updatedPost: PeejeePost) {
        posts = posts.map {
            if (it.id == updatedPost.id) updatedPost else it
        }

        if (selectedDiscoverPost?.id == updatedPost.id) {
            selectedDiscoverPost = updatedPost
        }

        if (commentsPost?.id == updatedPost.id) {
            commentsPost = updatedPost
        }
    }

    fun loadDiscoverContent(refreshing: Boolean = false) {
        if (refreshing) {
            isRefreshing = true
        } else {
            isLoading = true
        }

        errorMessage = ""

        firestore.collection("users")
            .limit(50)
            .get()
            .addOnSuccessListener { userSnapshot ->

                users = userSnapshot.documents.mapNotNull { document ->

                    val id = document.id

                    if (id == currentUserId) {
                        return@mapNotNull null
                    }

                    PeejeeDiscoverUser(
                        id = id,
                        name = document.getString("name")
                            ?: "Peejee User",
                        bio = document.getString("bio")
                            ?: "",
                        profilePhoto = document.getString("profilePhoto")
                            ?: "",
                        followersCount = document.getLong("followersCount")
                            ?: 0L,
                        updatedAt = document.getLong("updatedAt")
                            ?: 0L
                    )
                }.sortedByDescending {
                    it.updatedAt
                }

                firestore.collection("posts")
                    .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                    )
                    .limit(50)
                    .get()
                    .addOnSuccessListener { postSnapshot ->

                        posts = postSnapshot.documents.mapNotNull { document ->

                            try {
                                PeejeePost(
                                    id = document.id,
                                    userId = document.getString("userId")
                                        ?: "",
                                    userName = document.getString("userName")
                                        ?: "Peejee User",
                                    text = document.getString("text")
                                        ?: "",
                                    timestamp = document.getLong("timestamp")
                                        ?: 0L,
                                    mediaUrl = document.getString("mediaUrl")
                                        ?: "",
                                    mediaType = document.getString("mediaType")
                                        ?: "",
                                    likeCount = (
                                        document.getLong("likes")
                                            ?: 0L
                                        ).toInt(),
                                    likedBy = getLikedBy(document),
                                    commentCount = (
                                        document.getLong("commentCount")
                                            ?: 0L
                                        ).toInt(),
                                    shareCount = (
                                        document.getLong("shareCount")
                                            ?: 0L
                                        ).toInt(),
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
                            } catch (_: Exception) {
                                null
                            }
                        }

                        val tagCounter =
                            mutableMapOf<String, Int>()

                        posts.forEach { post ->

                            post.text
                                .split(Regex("\\s+"))
                                .forEach { word ->

                                    if (
                                        word.startsWith("#") &&
                                        word.length > 1
                                    ) {
                                        val tag = word
                                            .trim()
                                            .trimEnd(
                                                '.',
                                                ',',
                                                '!',
                                                '?',
                                                ':',
                                                ';'
                                            )
                                            .lowercase()

                                        if (tag.length > 1) {
                                            tagCounter[tag] =
                                                (tagCounter[tag] ?: 0) + 1
                                        }
                                    }
                                }
                        }

                        trendingTags =
                            tagCounter
                                .map {
                                    PeejeeTrendingTag(
                                        it.key,
                                        it.value
                                    )
                                }
                                .sortedByDescending {
                                    it.count
                                }
                                .take(10)

                        isLoading = false
                        isRefreshing = false
                        pullDistance = 0f
                    }
                    .addOnFailureListener { exception ->

                        errorMessage =
                            exception.message
                                ?: "Unable to load posts."

                        isLoading = false
                        isRefreshing = false
                        pullDistance = 0f
                    }
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.message
                        ?: "Unable to load Discover."

                isLoading = false
                isRefreshing = false
                pullDistance = 0f
            }
    }

    fun toggleLike(post: PeejeePost) {

        if (currentUserId.isBlank()) {
            return
        }

        val postReference =
            firestore
                .collection("posts")
                .document(post.id)

        firestore.runTransaction { transaction ->

            val snapshot =
                transaction.get(postReference)

            val existingLikedBy =
                getLikedBy(snapshot).toMutableMap()

            val currentlyLiked =
                existingLikedBy[currentUserId] == true

            val newLikedState =
                !currentlyLiked

            if (newLikedState) {
                existingLikedBy[currentUserId] = true
            } else {
                existingLikedBy.remove(currentUserId)
            }

            val currentLikes =
                snapshot.getLong("likes")
                    ?.toInt()
                    ?: 0

            val newLikeCount =
                if (newLikedState) {
                    currentLikes + 1
                } else {
                    maxOf(0, currentLikes - 1)
                }

            transaction.update(
                postReference,
                "likedBy",
                existingLikedBy
            )

            transaction.update(
                postReference,
                "likes",
                newLikeCount
            )

            Pair(
                newLikeCount,
                existingLikedBy
            )
        }.addOnSuccessListener { result ->

            val updatedPost =
                post.copy(
                    likeCount = result.first,
                    likedBy = result.second
                )

            replacePost(updatedPost)
        }
    }

    fun loadComments(postId: String) {

        firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy(
                "timestamp",
                Query.Direction.ASCENDING
            )
            .get()
            .addOnSuccessListener { snapshot ->

                val comments =
                    snapshot.documents.mapNotNull { document ->

                        PeejeeComment(
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
                                    ?: 0L
                        )
                    }

                commentsPost =
                    commentsPost?.let { currentPost ->

                        currentPost.copy(
                            commentCount = comments.size
                        )
                    }
            }
    }

    fun sharePost(post: PeejeePost) {

        val currentUser =
            auth.currentUser
                ?: return

        val postReference =
            firestore
                .collection("posts")
                .document(post.id)

        val newPostReference =
            firestore
                .collection("posts")
                .document()

        val userReference =
            firestore
                .collection("users")
                .document(currentUser.uid)

        firestore.runTransaction { transaction ->

            val postSnapshot =
                transaction.get(postReference)

            val userSnapshot =
                transaction.get(userReference)

            val currentShares =
                postSnapshot
                    .getLong("shareCount")
                    ?.toInt()
                    ?: 0

            val currentName =
                userSnapshot
                    .getString("name")
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Peejee User"

            val sharedPostData =
                hashMapOf<String, Any>(
                    "postId" to newPostReference.id,
                    "userId" to currentUser.uid,
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

                sharedPostData["mediaUrl"] =
                    post.mediaUrl

                sharedPostData["mediaType"] =
                    post.mediaType
            }

            transaction.set(
                newPostReference,
                sharedPostData
            )

            transaction.update(
                postReference,
                "shareCount",
                currentShares + 1
            )

            currentShares + 1
        }.addOnSuccessListener { newShareCount ->

            replacePost(
                post.copy(
                    shareCount = newShareCount
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        loadDiscoverContent()
    }

    val refreshConnection =
        remember {

            object : NestedScrollConnection {

                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {

                    if (
                        available.y > 0f &&
                        !isRefreshing &&
                        !isLoading &&
                        listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0
                    ) {

                        pullDistance += available.y

                        if (pullDistance >= 120f) {

                            pullDistance = 0f

                            loadDiscoverContent(
                                refreshing = true
                            )
                        }

                    } else if (available.y < 0f) {

                        pullDistance = 0f
                    }

                    return Offset.Zero
                }

                override suspend fun onPreFling(
                    available: Velocity
                ): Velocity {

                    pullDistance = 0f

                    return Velocity.Zero
                }
            }
        }

    val suggestedPeople =
        users
            .sortedByDescending {
                it.followersCount
            }
            .take(8)

    val newUsers =
        users
            .sortedByDescending {
                it.updatedAt
            }
            .take(8)

    val popularPosts =
        posts
            .sortedByDescending {
                it.likeCount +
                    it.commentCount +
                    it.shareCount
            }
            .take(10)

    val mediaPosts =
        posts
            .filter {
                it.mediaUrl.isNotBlank()
            }
            .take(12)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(refreshConnection)
    ) {

        if (isLoading) {

            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 40.dp,
                        bottom = 100.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                item {

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                "Discover",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Find people, posts and trends on Peejee",
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            "↻",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.clickable {
                                    loadDiscoverContent(
                                        refreshing = true
                                    )
                                }
                        )
                    }
                }

                if (
                    isRefreshing ||
                    pullDistance > 0f
                ) {

                    item {

                        PeejeeRefreshIndicator(
                            isRefreshing = isRefreshing
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {

                    item {

                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer
                                )
                        ) {

                            Text(
                                errorMessage,
                                modifier =
                                    Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                item {
                    DiscoverSectionTitle(
                        "🔥 Trending Posts"
                    )
                }

                if (popularPosts.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            "Trending posts will appear here."
                        )
                    }

                } else {

                    item {

                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp),
                            contentPadding =
                                PaddingValues(end = 4.dp)
                        ) {

                            items(
                                popularPosts,
                                key = {
                                    "popular_${it.id}"
                                }
                            ) { post ->

                                DiscoverTrendingPostCard(
                                    post = post
                                ) {

                                    selectedDiscoverPost =
                                        post
                                }
                            }
                        }
                    }
                }

                item {
                    DiscoverSectionTitle(
                        "👥 People You May Know"
                    )
                }

                if (suggestedPeople.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            "People will appear here as more users join Peejee."
                        )
                    }

                } else {

                    items(
                        suggestedPeople,
                        key = {
                            "suggested_${it.id}"
                        }
                    ) { user ->

                        DiscoverUserCard(
                            user = user
                        ) {
                            onUserClick(user.id)
                        }
                    }
                }

                item {
                    DiscoverSectionTitle(
                        "⭐ Suggested People to Follow"
                    )
                }

                if (suggestedPeople.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            "Suggested people will appear here."
                        )
                    }

                } else {

                    items(
                        suggestedPeople.take(5),
                        key = {
                            "follow_${it.id}"
                        }
                    ) { user ->

                        DiscoverUserCard(
                            user = user
                        ) {
                            onUserClick(user.id)
                        }
                    }
                }

                item {
                    DiscoverSectionTitle(
                        "🆕 New Peejee Users"
                    )
                }

                if (newUsers.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            "New users will appear here."
                        )
                    }

                } else {

                    items(
                        newUsers,
                        key = {
                            "new_${it.id}"
                        }
                    ) { user ->

                        DiscoverUserCard(
                            user = user
                        ) {
                            onUserClick(user.id)
                        }
                    }
                }

                item {
                    DiscoverSectionTitle(
                        "#️⃣ Trending Hashtags"
                    )
                }

                if (trendingTags.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            "Hashtags from posts will appear here."
                        )
                    }

                } else {

                    item {

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            trendingTags.forEach { trend ->

                                Surface(
                                    Modifier.fillMaxWidth(),
                                    RoundedCornerShape(14.dp),
                                    tonalElevation = 2.dp
                                ) {

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Text(
                                            trend.tag,
                                            fontWeight =
                                                FontWeight.Bold,
                                            modifier =
                                                Modifier.weight(1f)
                                        )

                                        Text(
                                            "${trend.count} post${
                                                if (trend.count == 1) ""
                                                else "s"
                                            }"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    DiscoverSectionTitle(
                        "📸🎥 Photo & Video Posts"
                    )
                }

                if (mediaPosts.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            "Photo and video posts will appear here."
                        )
                    }

                } else {

                    items(
                        mediaPosts,
                        key = {
                            "media_${it.id}"
                        }
                    ) { post ->

                        DiscoverPostCard(
                            post = post
                        ) {

                            selectedDiscoverPost =
                                post
                        }
                    }
                }
            }
        }

        if (selectedDiscoverPost != null) {

            DiscoverPostViewer(
                post = selectedDiscoverPost!!,
                currentUserId = currentUserId,
                onBack = {
                    selectedDiscoverPost = null
                },
                onLike = {
                    toggleLike(
                        selectedDiscoverPost!!
                    )
                },
                onComments = {

                    commentsPost =
                        selectedDiscoverPost

                    loadComments(
                        selectedDiscoverPost!!.id
                    )
                },
                onShare = {
                    sharePost(
                        selectedDiscoverPost!!
                    )
                },
                onOptions = {
                    showPostOptions = true
                }
            )
        }
    }

    if (commentsPost != null) {

        DiscoverCommentsDialog(
            post = commentsPost!!,
            firestore = firestore,
            auth = auth,
            onDismiss = {
                commentsPost = null
            },
            onPostUpdated = { updatedPost ->

                replacePost(updatedPost)

                commentsPost = updatedPost
            }
        )
    }

    if (showPostOptions) {

        AlertDialog(
            onDismissRequest = {
                showPostOptions = false
            },
            title = {
                Text("Post options")
            },
            text = {
                Text(
                    "This is a Peejee post."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPostOptions = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DiscoverPostViewer(
    post: PeejeePost,
    currentUserId: String,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onOptions: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val isLiked =
        post.likedBy[currentUserId] == true

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surface
                )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = 8.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {
                    Text(
                        "←",
                        fontSize = 28.sp
                    )
                }

                Column(
                    Modifier.weight(1f)
                ) {

                    Text(
                        post.userName,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Text(
                        "Peejee post",
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onOptions
                ) {

                    Text(
                        "⋮",
                        fontSize = 28.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {

                if (post.mediaUrl.isNotBlank()) {

                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription =
                            "Peejee post media",
                        modifier =
                            Modifier.fillMaxSize(),
                        contentScale =
                            ContentScale.Fit
                    )

                } else {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(30.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            post.text.ifBlank {
                                "Peejee Post"
                            },
                            fontSize = 24.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(
                                end = 12.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(14.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    DiscoverActionButton(
                        icon =
                            if (isLiked) {
                                "❤️"
                            } else {
                                "♡"
                            },
                        count =
                            post.likeCount,
                        onClick = onLike
                    )

                    DiscoverActionButton(
                        icon = "💬",
                        count =
                            post.commentCount,
                        onClick = onComments
                    )

                    DiscoverActionButton(
                        icon = "↗",
                        count =
                            post.shareCount,
                        onClick = onShare
                    )

                    DiscoverActionButton(
                        icon = "⋮",
                        count = null,
                        onClick = onOptions
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 20.dp
                        )
            ) {

                if (post.text.isNotBlank()) {

                    Text(
                        post.text,
                        fontSize = 16.sp,
                        modifier =
                            Modifier.padding(
                                bottom = 10.dp
                            )
                    )
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    TextButton(
                        onClick = onLike
                    ) {

                        Text(
                            if (isLiked) {
                                "❤️ Liked"
                            } else {
                                "♡ Like"
                            }
                        )
                    }

                    TextButton(
                        onClick = onComments
                    ) {

                        Text(
                            "💬 Comments"
                        )
                    }

                    TextButton(
                        onClick = onShare
                    ) {

                        Text(
                            "↗ Share"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverActionButton(
    icon: String,
    count: Int?,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Surface(
            modifier =
                Modifier
                    .size(52.dp)
                    .clickable {
                        onClick()
                    },
            shape = CircleShape,
            tonalElevation = 4.dp
        ) {

            Box(
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    icon,
                    fontSize = 25.sp
                )
            }
        }

        if (count != null) {

            Text(
                count.toString(),
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DiscoverCommentsDialog(
    post: PeejeePost,
    firestore: FirebaseFirestore,
    auth: FirebaseAuth,
    onDismiss: () -> Unit,
    onPostUpdated: (PeejeePost) -> Unit
) {

    var comments by remember(post.id) {
        mutableStateOf<List<PeejeeComment>>(
            emptyList()
        )
    }

    var commentText by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var sending by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    fun loadComments() {

        loading = true

        firestore
            .collection("posts")
            .document(post.id)
            .collection("comments")
            .orderBy(
                "timestamp",
                Query.Direction.ASCENDING
            )
            .get()
            .addOnSuccessListener { snapshot ->

                comments =
                    snapshot.documents.mapNotNull { document ->

                        PeejeeComment(
                            id = document.id,
                            userId =
                                document.getString(
                                    "userId"
                                ) ?: "",
                            userName =
                                document.getString(
                                    "userName"
                                ) ?: "Peejee User",
                            text =
                                document.getString(
                                    "text"
                                ) ?: "",
                            timestamp =
                                document.getLong(
                                    "timestamp"
                                ) ?: 0L
                        )
                    }

                loading = false
            }
            .addOnFailureListener { exception ->

                loading = false

                errorMessage =
                    exception.message
                        ?: "Unable to load comments."
            }
    }

    fun sendComment() {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            errorMessage =
                "Please log in again."

            return
        }

        if (commentText.isBlank()) {
            return
        }

        sending = true
        errorMessage = ""

        val userReference =
            firestore
                .collection("users")
                .document(currentUser.uid)

        userReference
            .get()
            .addOnSuccessListener { userDocument ->

                val userName =
                    userDocument
                        .getString("name")
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Peejee User"

                val commentReference =
                    firestore
                        .collection("posts")
                        .document(post.id)
                        .collection("comments")
                        .document()

                val postReference =
                    firestore
                        .collection("posts")
                        .document(post.id)

                firestore.runTransaction { transaction ->

                    val postSnapshot =
                        transaction.get(
                            postReference
                        )

                    val currentCount =
                        postSnapshot
                            .getLong(
                                "commentCount"
                            )
                            ?.toInt()
                            ?: 0

                    val commentData =
                        hashMapOf<String, Any>(
                            "userId" to
                                currentUser.uid,
                            "userName" to
                                userName,
                            "text" to
                                commentText.trim(),
                            "timestamp" to
                                System.currentTimeMillis()
                        )

                    transaction.set(
                        commentReference,
                        commentData
                    )

                    transaction.update(
                        postReference,
                        "commentCount",
                        currentCount + 1
                    )

                    currentCount + 1
                }
                    .addOnSuccessListener { newCount ->

                        val newComment =
                            PeejeeComment(
                                id =
                                    commentReference.id,
                                userId =
                                    currentUser.uid,
                                userName =
                                    userName,
                                text =
                                    commentText.trim(),
                                timestamp =
                                    System.currentTimeMillis()
                            )

                        comments =
                            comments + newComment

                        commentText = ""

                        sending = false

                        onPostUpdated(
                            post.copy(
                                commentCount =
                                    newCount
                            )
                        )
                    }
                    .addOnFailureListener { exception ->

                        sending = false

                        errorMessage =
                            exception.message
                                ?: "Unable to send comment."
                    }
            }
            .addOnFailureListener { exception ->

                sending = false

                errorMessage =
                    exception.message
                        ?: "Unable to send comment."
            }
    }

    LaunchedEffect(post.id) {
        loadComments()
    }

    AlertDialog(
        onDismissRequest = {
            if (!sending) {
                onDismiss()
            }
        },
        title = {
            Text("Comments")
        },
        text = {

            Column {

                if (loading) {

                    Box(
                        modifier =
                            Modifier.fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }

                } else if (comments.isEmpty()) {

                    Text(
                        "No comments yet. Be the first!"
                    )

                } else {

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        items(
                            comments,
                            key = {
                                it.id
                            }
                        ) { comment ->

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    )
                            ) {

                                Column(
                                    modifier =
                                        Modifier.padding(
                                            10.dp
                                        )
                                ) {

                                    Text(
                                        comment.userName,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        comment.text,
                                        modifier =
                                            Modifier.padding(
                                                top = 4.dp
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {

                    Text(
                        errorMessage,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontSize = 12.sp,
                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            )
                    )
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = {
                        commentText = it
                        errorMessage = ""
                    },
                    enabled = !sending,
                    label = {
                        Text("Write a comment")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss,
                enabled = !sending
            ) {

                Text("Close")
            }
        },
        confirmButton = {

            TextButton(
                onClick = {
                    sendComment()
                },
                enabled =
                    !sending &&
                        commentText.isNotBlank()
            ) {

                Text(
                    if (sending) {
                        "Sending..."
                    } else {
                        "Send"
                    }
                )
            }
        }
    )
}

@Composable
private fun PeejeeRefreshIndicator(
    isRefreshing: Boolean
) {

    val transition =
        rememberInfiniteTransition(
            label = "peejee_refresh"
        )

    val rotation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    tween(900),
                    RepeatMode.Restart
                ),
            label = "peejee_rotation"
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 2.dp,
                    bottom = 2.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier =
                Modifier.size(66.dp),
            contentAlignment =
                Alignment.Center
        ) {

            CircularProgressIndicator(
                modifier =
                    Modifier.fillMaxSize(),
                strokeWidth = 4.dp
            )

            Image(
                painter =
                    painterResource(
                        id =
                            R.drawable
                                .peejee_app_icon_512
                    ),
                contentDescription =
                    "Peejee refreshing",
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .rotate(
                            if (isRefreshing) {
                                rotation
                            } else {
                                0f
                            }
                        ),
                contentScale =
                    ContentScale.Crop
            )
        }

        Text(
            "Refreshing...",
            fontWeight =
                FontWeight.Bold,
            fontSize = 16.sp
        )

        Text(
            "Get the latest posts",
            fontSize = 13.sp
        )
    }
}

@Composable
private fun DiscoverSectionTitle(
    title: String
) {

    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier.padding(
                top = 6.dp
            )
    )
}

@Composable
private fun DiscoverUserCard(
    user: PeejeeDiscoverUser,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(18.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (user.profilePhoto.isNotBlank()) {

                AsyncImage(
                    model = user.profilePhoto,
                    contentDescription =
                        user.name,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        user.name
                            .firstOrNull()
                            ?.uppercase()
                            ?: "P",
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                Modifier.size(12.dp)
            )

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    user.name,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 17.sp
                )

                if (user.bio.isNotBlank()) {

                    Text(
                        user.bio,
                        maxLines = 2,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    Modifier.height(3.dp)
                )

                Text(
                    "${user.followersCount} followers",
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onClick
            ) {

                Text("View")
            }
        }
    }
}

@Composable
private fun DiscoverTrendingPostCard(
    post: PeejeePost,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .width(154.dp)
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(18.dp)
    ) {

        Column(
            Modifier.fillMaxWidth()
        ) {

            if (post.mediaUrl.isNotBlank()) {

                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription =
                        "Trending post",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .secondaryContainer
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        post.text.ifBlank {
                            "Peejee Post"
                        },
                        modifier =
                            Modifier.padding(12.dp),
                        maxLines = 5,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Column(
                Modifier.padding(10.dp)
            ) {

                Text(
                    post.userName,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1
                )

                if (post.text.isNotBlank()) {

                    Text(
                        post.text,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    "♥ ${post.likeCount}   " +
                        "💬 ${post.commentCount}   " +
                        "↗ ${post.shareCount}",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DiscoverPostCard(
    post: PeejeePost,
    onClick: () -> Unit
) {

    Card(
        Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        RoundedCornerShape(18.dp)
    ) {

        Column(
            Modifier.fillMaxWidth()
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 14.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        post.userName
                            .firstOrNull()
                            ?.uppercase()
                            ?: "P",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.size(10.dp)
                )

                Text(
                    post.userName,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (post.text.isNotBlank()) {

                Text(
                    post.text,
                    Modifier.padding(14.dp),
                    fontSize = 15.sp
                )
            }

            if (post.mediaUrl.isNotBlank()) {

                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription =
                        "Post media",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    contentScale =
                        ContentScale.Crop
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {

                Text(
                    "♥ ${post.likeCount}"
                )

                Text(
                    "💬 ${post.commentCount}"
                )

                Text(
                    "↗ ${post.shareCount}"
                )
            }
        }
    }
}

@Composable
private fun EmptyDiscoverCard(
    text: String
) {

    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp)
    ) {

        Text(
            text,
            Modifier.padding(18.dp),
            fontSize = 14.sp
        )
    }
}
