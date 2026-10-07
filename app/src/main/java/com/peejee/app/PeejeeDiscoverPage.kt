package com.peejee.app

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    val followingCount: Long = 0L,
    val updatedAt: Long = 0L
)

private data class PeejeeTrendingTag(
    val tag: String,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
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

                users = userSnapshot.documents
                    .mapNotNull { document ->

                        val id = document.id

                        if (id == currentUserId) {
                            return@mapNotNull null
                        }

                        PeejeeDiscoverUser(
                            id = id,
                            name = document.getString("name") ?: "Peejee User",
                            bio = document.getString("bio") ?: "",
                            profilePhoto = document.getString("profilePhoto") ?: "",
                            followersCount =
                                document.getLong("followersCount") ?: 0L,
                            followingCount =
                                document.getLong("followingCount") ?: 0L,
                            updatedAt =
                                document.getLong("updatedAt") ?: 0L
                        )
                    }
                    .sortedByDescending {
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

                        posts = postSnapshot.documents
                            .mapNotNull { document ->

                                try {

                                    PeejeePost(
                                        id = document.id,
                                        userId =
                                            document.getString("userId") ?: "",
                                        userName =
                                            document.getString("userName")
                                                ?: "Peejee User",
                                        text =
                                            document.getString("text") ?: "",
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
                                            document.getLong("likeCount")
                                                ?: 0L,
                                        likedBy =
                                            (document.get("likedBy")
                                                as? List<*>)
                                                ?.mapNotNull {
                                                    it?.toString()
                                                }
                                                ?: emptyList(),
                                        commentCount =
                                            document.getLong("commentCount")
                                                ?: 0L,
                                        shareCount =
                                            document.getLong("shareCount")
                                                ?: 0L,
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

                        val tagCounter = mutableMapOf<String, Int>()

                        posts.forEach { post ->

                            val words =
                                post.text
                                    .split(
                                        Regex("\\s+")
                                    )

                            words.forEach { word ->

                                if (word.startsWith("#") &&
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

                                    if (cleanTag.length > 1) {
                                        tagCounter[
                                            cleanTag
                                        ] =
                                            (tagCounter[cleanTag] ?: 0) + 1
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
                    .addOnFailureListener { error ->

                        errorMessage =
                            error.message
                                ?: "Unable to load posts."

                        isLoading = false
                        isRefreshing = false
                    }
            }
            .addOnFailureListener { error ->

                errorMessage =
                    error.message
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
                it.likeCount + it.commentCount + it.shareCount
            }
            .take(10)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            loadDiscoverContent(true)
        },
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
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Discover",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Find people, posts and trends on Peejee",
                                fontSize = 14.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                loadDiscoverContent(true)
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {

                    item {

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {

                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                item {

                    DiscoverSectionTitle(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null
                            )
                        },
                        title = "Trending Now"
                    )
                }

                if (popularPosts.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            text = "Trending posts will appear here."
                        )
                    }

                } else {

                    items(
                        items = popularPosts,
                        key = { it.id }
                    ) { post ->

                        DiscoverPostCard(
                            post = post,
                            onClick = {
                                onPostClick(post.id)
                            }
                        )
                    }
                }

                item {

                    DiscoverSectionTitle(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null
                            )
                        },
                        title = "People You May Know"
                    )
                }

                if (suggestedPeople.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            text = "People will appear here as more users join Peejee."
                        )
                    }

                } else {

                    items(
                        items = suggestedPeople,
                        key = { "suggested_${it.id}" }
                    ) { user ->

                        DiscoverUserCard(
                            user = user,
                            onClick = {
                                onUserClick(user.id)
                            }
                        )
                    }
                }

                item {

                    DiscoverSectionTitle(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null
                            )
                        },
                        title = "New Peejee Users"
                    )
                }

                if (newUsers.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            text = "New users will appear here."
                        )
                    }

                } else {

                    items(
                        items = newUsers,
                        key = { "new_${it.id}" }
                    ) { user ->

                        DiscoverUserCard(
                            user = user,
                            onClick = {
                                onUserClick(user.id)
                            }
                        )
                    }
                }

                item {

                    DiscoverSectionTitle(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        title = "Trending Hashtags"
                    )
                }

                if (trendingTags.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            text = "Hashtags from popular posts will appear here."
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { },
                                    shape = RoundedCornerShape(14.dp),
                                    tonalElevation = 2.dp
                                ) {

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = trend.tag,
                                            fontWeight =
                                                FontWeight.Bold,
                                            modifier =
                                                Modifier.weight(1f)
                                        )

                                        Text(
                                            text =
                                                "${trend.count} post" +
                                                    if (trend.count == 1)
                                                        ""
                                                    else
                                                        "s"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {

                    DiscoverSectionTitle(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null
                            )
                        },
                        title = "Photo & Video Posts"
                    )
                }

                val mediaPosts =
                    posts
                        .filter {
                            it.mediaUrl.isNotBlank()
                        }
                        .take(12)

                if (mediaPosts.isEmpty()) {

                    item {
                        EmptyDiscoverCard(
                            text = "Photo and video posts will appear here."
                        )
                    }

                } else {

                    items(
                        items = mediaPosts,
                        key = { "media_${it.id}" }
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

@androidx.compose.runtime.Composable
private fun DiscoverSectionTitle(
    icon: @androidx.compose.runtime.Composable () -> Unit,
    title: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        icon()

        Spacer(
            modifier = Modifier.size(8.dp)
        )

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@androidx.compose.runtime.Composable
private fun DiscoverUserCard(
    user: PeejeeDiscoverUser,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (user.profilePhoto.isNotBlank()) {

                AsyncImage(
                    model = user.profilePhoto,
                    contentDescription = user.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text =
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
                modifier = Modifier.size(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                if (user.bio.isNotBlank()) {

                    Text(
                        text = user.bio,
                        maxLines = 2,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
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

@androidx.compose.runtime.Composable
private fun DiscoverPostCard(
    post: PeejeePost,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text =
                            post.userName
                                .firstOrNull()
                                ?.uppercase()
                                ?: "P",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Text(
                    text = post.userName,
                    fontWeight = FontWeight.Bold
                )
            }

            if (post.text.isNotBlank()) {

                Text(
                    text = post.text,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 15.sp
                )
            }

            if (post.mediaUrl.isNotBlank()) {

                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Post media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {

                Text(
                    text = "♥ ${post.likeCount}"
                )

                Text(
                    text = "💬 ${post.commentCount}"
                )

                Text(
                    text = "↗ ${post.shareCount}"
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptyDiscoverCard(
    text: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            fontSize = 14.sp
        )
    }
}
