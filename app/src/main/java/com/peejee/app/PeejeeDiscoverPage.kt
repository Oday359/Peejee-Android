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
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val currentUserId =
        remember {
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: ""
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

    /*
     * The currently opened Discover post.
     */
    var selectedDiscoverPost by remember {
        mutableStateOf<PeejeePost?>(null)
    }

    /*
     * Comments state.
     */
    var showCommentsDialog by remember {
        mutableStateOf(false)
    }

    var comments by remember {
        mutableStateOf<List<PeejeeComment>>(emptyList())
    }

    var newComment by remember {
        mutableStateOf("")
    }

    var commentsLoading by remember {
        mutableStateOf(false)
    }

    var sendingComment by remember {
        mutableStateOf(false)
    }

    var commentError by remember {
        mutableStateOf("")
    }

    /*
     * Post options dialog.
     */
    var showPostOptions by remember {
        mutableStateOf(false)
    }

    fun replacePost(updatedPost: PeejeePost) {

        posts =
            posts.map { post ->
                if (post.id == updatedPost.id) {
                    updatedPost
                } else {
                    post
                }
            }

        selectedDiscoverPost = updatedPost
    }

    fun loadDiscoverContent(
        refreshing: Boolean = false
    ) {

        if (refreshing) {
            isRefreshing = true
        } else {
            isLoading = true
        }

        errorMessage = ""

        firestore
            .collection("users")
            .limit(50)
            .get()
            .addOnSuccessListener { userSnapshot ->

                users =
                    userSnapshot.documents
                        .mapNotNull { document ->

                            val id = document.id

                            if (id == currentUserId) {
                                return@mapNotNull null
                            }

                            PeejeeDiscoverUser(
                                id = id,
                                name =
                                    document
                                        .getString("name")
                                        ?: "Peejee User",
                                bio =
                                    document
                                        .getString("bio")
                                        ?: "",
                                profilePhoto =
                                    document
                                        .getString("profilePhoto")
                                        ?: "",
                                followersCount =
                                    document
                                        .getLong("followersCount")
                                        ?: 0L,
                                updatedAt =
                                    document
                                        .getLong("updatedAt")
                                        ?: 0L
                            )
                        }
                        .sortedByDescending {
                            it.updatedAt
                        }

                firestore
                    .collection("posts")
                    .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                    )
                    .limit(50)
                    .get()
                    .addOnSuccessListener { postSnapshot ->

                        posts =
                            postSnapshot.documents
                                .mapNotNull { document ->

                                    try {

                                        PeejeePost(
                                            id = document.id,
                                            userId =
                                                document
                                                    .getString("userId")
                                                    ?: "",
                                            userName =
                                                document
                                                    .getString("userName")
                                                    ?: "Peejee User",
                                            text =
                                                document
                                                    .getString("text")
                                                    ?: "",
                                            timestamp =
                                                document
                                                    .getLong("timestamp")
                                                    ?: 0L,
                                            mediaUrl =
                                                document
                                                    .getString("mediaUrl")
                                                    ?: "",
                                            mediaType =
                                                document
                                                    .getString("mediaType")
                                                    ?: "",
                                            likeCount =
                                                (
                                                    document
                                                        .getLong("likes")
                                                        ?: 0L
                                                ).toInt(),
                                            likedBy =
                                                getLikedBy(document),
                                            commentCount =
                                                (
                                                    document
                                                        .getLong("commentCount")
                                                        ?: 0L
                                                ).toInt(),
                                            shareCount =
                                                (
                                                    document
                                                        .getLong("shareCount")
                                                        ?: 0L
                                                ).toInt(),
                                            sharedFromPostId =
                                                document
                                                    .getString(
                                                        "sharedFromPostId"
                                                    )
                                                    ?: "",
                                            sharedFromUserName =
                                                document
                                                    .getString(
                                                        "sharedFromUserName"
                                                    )
                                                    ?: "",
                                            sharedFromText =
                                                document
                                                    .getString(
                                                        "sharedFromText"
                                                    )
                                                    ?: ""
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

                                        val tag =
                                            word
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
                                                (
                                                    tagCounter[tag]
                                                        ?: 0
                                                ) + 1
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

                        /*
                         * If the opened post still exists,
                         * refresh the opened copy too.
                         */
                        selectedDiscoverPost?.let { opened ->

                            posts
                                .firstOrNull {
                                    it.id == opened.id
                                }
                                ?.let { freshPost ->
                                    selectedDiscoverPost =
                                        freshPost
                                }
                        }
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

    /*
     * LIKE POST
     */
    fun toggleLike(post: PeejeePost) {

        val userId = currentUserId

        if (userId.isBlank()) {
            return
        }

        val postReference =
            firestore
                .collection("posts")
                .document(post.id)

        firestore.runTransaction { transaction ->

            val snapshot =
                transaction.get(postReference)

            val currentLikes =
                (
                    snapshot.getLong("likes")
                        ?: 0L
                ).toInt()

            val rawLikedBy =
                snapshot.get("likedBy")

            val likedBy =
                mutableMapOf<String, Boolean>()

            if (rawLikedBy is Map<*, *>) {

                rawLikedBy.forEach { entry ->

                    val key =
                        entry.key as? String

                    val value =
                        entry.value as? Boolean

                    if (
                        key != null &&
                        value != null
                    ) {
                        likedBy[key] = value
                    }
                }
            }

            val alreadyLiked =
                likedBy[userId] == true

            if (alreadyLiked) {

                likedBy.remove(userId)

                transaction.update(
                    postReference,
                    "likes",
                    maxOf(0, currentLikes - 1)
                )

            } else {

                likedBy[userId] = true

                transaction.update(
                    postReference,
                    "likes",
                    currentLikes + 1
                )
            }

            transaction.update(
                postReference,
                "likedBy",
                likedBy
            )

            Pair(
                !alreadyLiked,
                if (alreadyLiked) {
                    maxOf(0, currentLikes - 1)
                } else {
                    currentLikes + 1
                }
            )

        }.addOnSuccessListener { result ->

            val wasLiked = result.first
            val newLikeCount = result.second

            val updatedLikedBy =
                post.likedBy.toMutableMap()

            if (wasLiked) {
                updatedLikedBy[userId] = true
            } else {
                updatedLikedBy.remove(userId)
            }

            val updatedPost =
                post.copy(
                    likeCount = newLikeCount,
                    likedBy = updatedLikedBy
                )

            replacePost(updatedPost)
        }
    }

    /*
     * LOAD COMMENTS
     */
    fun loadComments(postId: String) {

        commentsLoading = true
        commentError = ""

        firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy(
                "timestamp",
                Query.Direction.DESCENDING
            )
            .get()
            .addOnSuccessListener { snapshot ->

                comments =
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

                commentsLoading = false
            }
            .addOnFailureListener { exception ->

                commentsLoading = false

                commentError =
                    exception.message
                        ?: "Unable to load comments."
            }
    }

    /*
     * OPEN COMMENTS
     */
    fun openComments(post: PeejeePost) {

        showCommentsDialog = true
        comments = emptyList()
        newComment = ""
        commentError = ""

        loadComments(post.id)
    }

    /*
     * SEND COMMENT
     */
    fun sendComment(post: PeejeePost) {

        val user =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?: return

        val text =
            newComment.trim()

        if (text.isBlank() || sendingComment) {
            return
        }

        sendingComment = true
        commentError = ""

        firestore
            .collection("users")
            .document(user.uid)
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

                val commentData =
                    hashMapOf<String, Any>(
                        "commentId" to commentReference.id,
                        "userId" to user.uid,
                        "userName" to userName,
                        "text" to text,
                        "timestamp" to
                            System.currentTimeMillis()
                    )

                val postReference =
                    firestore
                        .collection("posts")
                        .document(post.id)

                firestore.runTransaction { transaction ->

                    val postSnapshot =
                        transaction.get(postReference)

                    val currentCommentCount =
                        (
                            postSnapshot
                                .getLong("commentCount")
                                ?: 0L
                        ).toInt()

                    transaction.set(
                        commentReference,
                        commentData
                    )

                    transaction.update(
                        postReference,
                        "commentCount",
                        currentCommentCount + 1
                    )

                    currentCommentCount + 1

                }.addOnSuccessListener { newCount ->

                    val newCommentObject =
                        PeejeeComment(
                            id = commentReference.id,
                            userId = user.uid,
                            userName = userName,
                            text = text,
                            timestamp =
                                System.currentTimeMillis()
                        )

                    comments =
                        listOf(
                            newCommentObject
                        ) + comments

                    newComment = ""
                    sendingComment = false

                    replacePost(
                        post.copy(
                            commentCount = newCount
                        )
                    )

                }.addOnFailureListener { exception ->

                    sendingComment = false

                    commentError =
                        exception.message
                            ?: "Unable to send comment."
                }
            }
            .addOnFailureListener { exception ->

                sendingComment = false

                commentError =
                    exception.message
                        ?: "Unable to send comment."
            }
    }

    /*
     * SHARE POST
     */
    fun sharePost(post: PeejeePost) {

        val user =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?: return

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { userDocument ->

                val currentName =
                    userDocument
                        .getString("name")
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Peejee User"

                val originalPostReference =
                    firestore
                        .collection("posts")
                        .document(post.id)

                val newPostReference =
                    firestore
                        .collection("posts")
                        .document()

                firestore.runTransaction { transaction ->

                    val postSnapshot =
                        transaction.get(
                            originalPostReference
                        )

                    val currentShares =
                        (
                            postSnapshot
                                .getLong("shareCount")
                                ?: 0L
                        ).toInt()

                    val sharedPostData =
                        hashMapOf<String, Any>(
                            "postId" to
                                newPostReference.id,
                            "userId" to
                                user.uid,
                            "userName" to
                                currentName,
                            "text" to
                                post.text,
                            "likes" to
                                0,
                            "likedBy" to
                                emptyMap<String, Boolean>(),
                            "commentCount" to
                                0,
                            "shareCount" to
                                0,
                            "timestamp" to
                                System.currentTimeMillis(),
                            "sharedFromPostId" to
                                post.id,
                            "sharedFromUserName" to
                                post.userName,
                            "sharedFromText" to
                                post.text
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
                        originalPostReference,
                        "shareCount",
                        currentShares + 1
                    )

                    currentShares + 1

                }.addOnSuccessListener { newShareCount ->

                    replacePost(
                        post.copy(
                            shareCount =
                                newShareCount
                        )
                    )
                }
            }
    }

    LaunchedEffect(Unit) {
        loadDiscoverContent()
    }

    val refreshConnection = remember {

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

                        loadDiscoverContent(true)
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

    /*
     * Android back button for the opened post.
     */
    if (selectedDiscoverPost != null) {

        BackHandler {

            selectedDiscoverPost = null
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(
                    refreshConnection
                )
    ) {

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
                    openComments(
                        selectedDiscoverPost!!
                    )
                },
                onShare = {
                    sharePost(
                        selectedDiscoverPost!!
                    )
                },
                onMore = {
                    showPostOptions = true
                }
            )

        } else if (isLoading) {

            Box(
                Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        } else {

            LazyColumn(
                state = listState,
                modifier =
                    Modifier.fillMaxSize(),
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
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                "Find people, posts and trends on Peejee",
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            "↻",
                            fontSize = 30.sp,
                            fontWeight =
                                FontWeight.Bold,
                            modifier =
                                Modifier.clickable {
                                    loadDiscoverContent(
                                        true
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
                            isRefreshing =
                                isRefreshing
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {

                    item {

                        Card(
                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .errorContainer
                                    )
                        ) {

                            Text(
                                errorMessage,
                                modifier =
                                    Modifier.padding(
                                        16.dp
                                    )
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
                                Arrangement.spacedBy(
                                    12.dp
                                ),
                            contentPadding =
                                PaddingValues(
                                    end = 4.dp
                                )
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

                        DiscoverUserCard(user) {

                            onUserClick(
                                user.id
                            )
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

                        DiscoverUserCard(user) {

                            onUserClick(
                                user.id
                            )
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

                        DiscoverUserCard(user) {

                            onUserClick(
                                user.id
                            )
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
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {

                            trendingTags.forEach { trend ->

                                Surface(
                                    Modifier.fillMaxWidth(),
                                    RoundedCornerShape(
                                        14.dp
                                    ),
                                    tonalElevation = 2.dp
                                ) {

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                14.dp
                                            ),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Text(
                                            trend.tag,
                                            fontWeight =
                                                FontWeight.Bold,
                                            modifier =
                                                Modifier.weight(
                                                    1f
                                                )
                                        )

                                        Text(
                                            "${trend.count} post${
                                                if (
                                                    trend.count == 1
                                                ) {
                                                    ""
                                                } else {
                                                    "s"
                                                }
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

                        DiscoverPostCard(post) {

                            selectedDiscoverPost =
                                post
                        }
                    }
                }
            }
        }
    }

    /*
     * COMMENTS DIALOG
     */
    if (
        showCommentsDialog &&
        selectedDiscoverPost != null
    ) {

        AlertDialog(
            onDismissRequest = {
                showCommentsDialog = false
            },
            title = {
                Text(
                    "Comments",
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {

                Column(
                    Modifier.fillMaxWidth()
                ) {

                    if (commentsLoading) {

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            CircularProgressIndicator()
                        }

                    } else if (
                        comments.isEmpty()
                    ) {

                        Text(
                            "No comments yet. Be the first to comment.",
                            modifier =
                                Modifier.padding(
                                    vertical = 12.dp
                                )
                        )

                    } else {

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {

                            items(
                                comments,
                                key = {
                                    it.id
                                }
                            ) { comment ->

                                Column(
                                    Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        comment.userName,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        comment.text,
                                        fontSize =
                                            14.sp
                                    )
                                }
                            }
                        }
                    }

                    if (
                        commentError.isNotBlank()
                    ) {

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            commentError,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    OutlinedTextField(
                        value = newComment,
                        onValueChange = {
                            newComment = it
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                "Write a comment..."
                            )
                        },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {

                Button(
                    onClick = {

                        sendComment(
                            selectedDiscoverPost!!
                        )
                    },
                    enabled =
                        newComment
                            .trim()
                            .isNotBlank() &&
                            !sendingComment
                ) {

                    Text(
                        if (sendingComment) {
                            "Sending..."
                        } else {
                            "Send"
                        }
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showCommentsDialog = false
                    }
                ) {

                    Text("Close")
                }
            }
        )
    }

    /*
     * POST OPTIONS
     */
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
                    "More post options will be added here."
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
    onMore: () -> Unit
) {

    val isLiked =
        post.likedBy[currentUserId] == true

    Box(
        Modifier
            .fillMaxSize()
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            )
    ) {

        Column(
            Modifier.fillMaxSize()
        ) {

            /*
             * TOP BAR
             */
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 10.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                onBack()
                            },
                    shape = CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            "←",
                            fontSize = 27.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    Modifier.width(12.dp)
                )

                Column(
                    Modifier.weight(1f)
                ) {

                    Text(
                        post.userName,
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "Peejee Post",
                        fontSize = 12.sp
                    )
                }

                Surface(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                onMore()
                            },
                    shape = CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            "⋮",
                            fontSize = 28.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            /*
             * POST CONTENT
             */
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = 8.dp
                        )
                ) {

                    if (
                        post.mediaUrl.isNotBlank()
                    ) {

                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription =
                                "Peejee post media",
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            contentScale =
                                ContentScale.Fit
                        )

                    } else {

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    20.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
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
                                    Modifier.padding(
                                        20.dp
                                    ),
                                fontSize = 22.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    if (
                        post.text.isNotBlank()
                    ) {

                        Text(
                            post.text,
                            modifier =
                                Modifier.padding(
                                    start = 18.dp,
                                    end = 18.dp,
                                    top = 12.dp,
                                    bottom = 12.dp
                                ),
                            fontSize = 16.sp
                        )
                    }
                }

                /*
                 * RIGHT-SIDE ACTION BUTTONS
                 */
                Column(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .padding(
                                end = 12.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(
                            14.dp
                        )
                ) {

                    DiscoverActionButton(
                        icon =
                            if (isLiked) {
                                "♥"
                            } else {
                                "♡"
                            },
                        count =
                            post.likeCount,
                        active =
                            isLiked,
                        onClick = onLike
                    )

                    DiscoverActionButton(
                        icon = "💬",
                        count =
                            post.commentCount,
                        onClick =
                            onComments
                    )

                    DiscoverActionButton(
                        icon = "↗",
                        count =
                            post.shareCount,
                        onClick =
                            onShare
                    )

                    DiscoverActionButton(
                        icon = "⋮",
                        count = 0,
                        onClick = onMore
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverActionButton(
    icon: String,
    count: Int,
    active: Boolean = false,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Surface(
            modifier =
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable {
                        onClick()
                    },
            shape = CircleShape,
            color =
                if (active) {
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                } else {
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                }
        ) {

            Box(
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    icon,
                    fontSize = 25.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        if (count > 0) {

            Spacer(
                Modifier.height(3.dp)
            )

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
private fun PeejeeRefreshIndicator(
    isRefreshing: Boolean
) {

    val transition =
        rememberInfiniteTransition(
            label = "peejee_refresh"
        )

    val rotation by transition.animateFloat(
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
        fontWeight =
            FontWeight.Bold,
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

            if (
                user.profilePhoto.isNotBlank()
            ) {

                AsyncImage(
                    user.profilePhoto,
                    user.name,
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

                if (
                    user.bio.isNotBlank()
                ) {

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

            if (
                post.mediaUrl.isNotBlank()
            ) {

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
                            Modifier.padding(
                                12.dp
                            ),
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

                if (
                    post.text.isNotBlank()
                ) {

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
                    "♥ ${post.likeCount}   💬 ${post.commentCount}   ↗ ${post.shareCount}",
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

            if (
                post.text.isNotBlank()
            ) {

                Text(
                    post.text,
                    Modifier.padding(14.dp),
                    fontSize = 15.sp
                )
            }

            if (
                post.mediaUrl.isNotBlank()
            ) {

                AsyncImage(
                    post.mediaUrl,
                    "Post media",
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
