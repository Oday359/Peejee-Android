package com.peejee.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val currentUserId = remember {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

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
                                    document.getString("name")
                                        ?: "Peejee User",
                                bio =
                                    document.getString("bio")
                                        ?: "",
                                profilePhoto =
                                    document.getString("profilePhoto")
                                        ?: "",
                                followersCount =
                                    document.getLong(
                                        "followersCount"
                                    ) ?: 0L,
                                updatedAt =
                                    document.getLong(
                                        "updatedAt"
                                    ) ?: 0L
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
                            postSnapshot.documents.mapNotNull { document ->

                                try {

                                    PeejeePost(
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
                                            (
                                                document.getLong(
                                                    "likes"
                                                ) ?: 0L
                                            ).toInt(),

                                        likedBy =
                                            getLikedBy(document),

                                        commentCount =
                                            (
                                                document.getLong(
                                                    "commentCount"
                                                ) ?: 0L
                                            ).toInt(),

                                        shareCount =
                                            (
                                                document.getLong(
                                                    "shareCount"
                                                ) ?: 0L
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

                                        val cleanTag =
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

                                        if (
                                            cleanTag.length > 1
                                        ) {

                                            tagCounter[
                                                cleanTag
                                            ] =
                                                (
                                                    tagCounter[
                                                        cleanTag
                                                    ] ?: 0
                                                ) + 1
                                        }
                                    }
                                }
                        }

                        trendingTags =
                            tagCounter
                                .map {
                                    PeejeeTrendingTag(
                                        tag = it.key,
                                        count = it.value
                                    )
                                }
                                .sortedByDescending {
                                    it.count
                                }
                                .take(10)

                        isLoading = false
                        isRefreshing = false
                    }
                    .addOnFailureListener { exception ->

                        errorMessage =
                            exception.message
                                ?: "Unable to load posts."

                        isLoading = false
                        isRefreshing = false
                    }
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.message
                        ?: "Unable to load Discover."

                isLoading = false
                isRefreshing = false
            }
    }

    LaunchedEffect(Unit) {
        loadDiscoverContent()
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
        modifier = Modifier.fillMaxSize()
    ) {

        if (isLoading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        } else {

            LazyColumn(
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
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
                            text = "↻",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.clickable {
                                    loadDiscoverContent(true)
                                }
                        )
                    }
                }

                if (isRefreshing) {

                    item {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(
                                Modifier.size(8.dp)
                            )

                            Text(
                                "Refreshing Discover..."
                            )
                        }
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

                /*
                 * TRENDING POSTS
                 *
                 * This is intentionally horizontal.
                 * Only the top 10 trending posts are shown.
                 */

                item {

                    DiscoverSectionTitle(
                        title = "🔥 Trending Posts"
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
                                PaddingValues(
                                    start = 2.dp,
                                    end = 2.dp
                                )
                        ) {

                            items(
                                items = popularPosts,
                                key = {
                                    "trending_${it.id}"
                                }
                            ) { post ->

                                DiscoverTrendingPostCard(
                                    post = post,
                                    onClick = {
                                        onPostClick(post.id)
                                    }
                                )
                            }
                        }
                    }
                }

                /*
                 * PEOPLE YOU MAY KNOW
                 */

                item {

                    DiscoverSectionTitle(
                        title = "👥 People You May Know"
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
                        items = suggestedPeople,
                        key = {
                            "suggested_${it.id}"
                        }
                    ) { user ->

                        DiscoverUserCard(
                            user = user,
                            onClick = {
                                onUserClick(user.id)
                            }
                        )
                    }
                }

                /*
                 * SUGGESTED PEOPLE TO FOLLOW
                 */

                item {

                    DiscoverSectionTitle(
                        title = "⭐ Suggested People to Follow"
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
                        items = suggestedPeople.take(5),
                        key = {
                            "follow_${it.id}"
                        }
                    ) { user ->

                        DiscoverUserCard(
                            user = user,
                            onClick = {
                                onUserClick(user.id)
                            }
                        )
                    }
                }

                /*
                 * NEW USERS
                 */

                item {

                    DiscoverSectionTitle(
                        title = "🆕 New Peejee Users"
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
                        items = newUsers,
                        key = {
                            "new_${it.id}"
                        }
                    ) { user ->

                        DiscoverUserCard(
                            user = user,
                            onClick = {
                                onUserClick(user.id)
                            }
                        )
                    }
                }

                /*
                 * TRENDING HASHTAGS
                 */

                item {

                    DiscoverSectionTitle(
                        title = "#️⃣ Trending Hashtags"
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
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    shape =
                                        RoundedCornerShape(14.dp),
                                    tonalElevation = 2.dp
                                ) {

                                    Row(
                                        modifier =
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
                                            "${trend.count} post" +
                                                if (
                                                    trend.count == 1
                                                ) {
                                                    ""
                                                } else {
                                                    "s"
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                /*
                 * PHOTO AND VIDEO POSTS
                 */

                item {

                    DiscoverSectionTitle(
                        title = "📸🎥 Photo & Video Posts"
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
                        items = mediaPosts,
                        key = {
                            "media_${it.id}"
                        }
                    ) { post ->

                        DiscoverPostCard(
                            post = post,
                            onClick = {
                                onPostClick(post.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

/*
 * SECTION TITLE
 */

@Composable
private fun DiscoverSectionTitle(
    title: String
) {

    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier.padding(top = 6.dp)
    )
}

/*
 * COMPACT HORIZONTAL TRENDING POST CARD
 */

@Composable
private fun DiscoverTrendingPostCard(
    post: PeejeePost,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .width(210.dp)
                .height(285.dp)
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(18.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * MEDIA
             */

            if (post.mediaUrl.isNotBlank()) {

                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Trending post media",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(155.dp),
                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            if (
                                post.text.isNotBlank()
                            ) {
                                post.text
                            } else {
                                "Peejee Post"
                            },
                        modifier =
                            Modifier.padding(14.dp),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 5,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }

            /*
             * USER + POST TEXT
             */

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
            ) {

                Text(
                    post.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                if (
                    post.text.isNotBlank() &&
                    post.mediaUrl.isNotBlank()
                ) {

                    Spacer(
                        Modifier.height(3.dp)
                    )

                    Text(
                        post.text,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    Modifier.height(7.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "♥ ${post.likeCount}",
                        fontSize = 12.sp
                    )

                    Text(
                        "💬 ${post.commentCount}",
                        fontSize = 12.sp
                    )

                    Text(
                        "↗ ${post.shareCount}",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/*
 * USER CARD
 */

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
            modifier =
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
                    model = user.profilePhoto,
                    contentDescription = user.name,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Box(
                    modifier =
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                Modifier.size(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                if (
                    user.bio.isNotBlank()
                ) {

                    Text(
                        user.bio,
                        maxLines = 2,
                        fontSize = 13.sp,
                        overflow =
                            TextOverflow.Ellipsis
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

/*
 * FULL PHOTO / VIDEO POST CARD
 */

@Composable
private fun DiscoverPostCard(
    post: PeejeePost,
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
            RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
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
                    modifier =
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
                    modifier =
                        Modifier.padding(14.dp),
                    fontSize = 15.sp
                )
            }

            if (
                post.mediaUrl.isNotBlank()
            ) {

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
                modifier =
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

/*
 * EMPTY CARD
 */

@Composable
private fun EmptyDiscoverCard(
    text: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Text(
            text,
            modifier =
                Modifier.padding(18.dp),
            fontSize = 14.sp
        )
    }
}
