package com.peejee.app

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PeejeeFeedPurple = Color(0xFF7B2FF7)
private val PeejeeFeedPalePurple = Color(0xFFF1E8FF)

/**

UI model for one item in the home feed.

Convert your existing Firebase post objects to this model when integrating.
*/
data class PeejeeHomeFeedItem(
val id: String,
val authorName: String,
val authorProfilePhotoBase64: String = "",
val text: String = "",
val mediaUri: String = "",
val mediaType: String = "",
val timeLabel: String = "",
val likeCount: Int = 0,
val commentCount: Int = 0,
val shareCount: Int = 0,
val isLikedByCurrentUser: Boolean = false,
val isSavedByCurrentUser: Boolean = false
)


/**

Scrollable presentation of home-feed posts.

This is a separate UI component. It does not fetch Firebase data or perform

like/comment/share/save actions itself; the parent supplies data and callbacks.
*/
@Composable
fun PeejeeHomeFeed(
posts: List<PeejeeHomeFeedItem>,
onAuthorClick: (PeejeeHomeFeedItem) -> Unit,
onLikeClick: (PeejeeHomeFeedItem) -> Unit,
onCommentClick: (PeejeeHomeFeedItem) -> Unit,
onShareClick: (PeejeeHomeFeedItem) -> Unit,
onSaveClick: (PeejeeHomeFeedItem) -> Unit,
modifier: Modifier = Modifier
) {
if (posts.isEmpty()) {
Surface(
modifier = modifier.fillMaxWidth(),
shape = RoundedCornerShape(14.dp),
color = MaterialTheme.colorScheme.surfaceVariant
) {
Text(
text = "No posts to show yet. Follow people or create your first post!",
color = MaterialTheme.colorScheme.onSurfaceVariant,
fontSize = 14.sp,
modifier = Modifier.padding(16.dp)
)
}
} else {
LazyColumn(
modifier = modifier.fillMaxWidth(),
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
items(posts, key = { it.id }) { post ->
PeejeeFeedPostCard(
post = post,
onAuthorClick = { onAuthorClick(post) },
onLikeClick = { onLikeClick(post) },
onCommentClick = { onCommentClick(post) },
onShareClick = { onShareClick(post) },
onSaveClick = { onSaveClick(post) }
)
}
}
}
}


@Composable
private fun PeejeeFeedPostCard(
post: PeejeeHomeFeedItem,
onAuthorClick: () -> Unit,
onLikeClick: () -> Unit,
onCommentClick: () -> Unit,
onShareClick: () -> Unit,
onSaveClick: () -> Unit
) {
Card(
modifier = Modifier.fillMaxWidth(),
shape = RoundedCornerShape(16.dp),
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
) {
Column {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(14.dp),
verticalAlignment = Alignment.CenterVertically
) {
PeejeeFeedProfilePhoto(
photoBase64 = post.authorProfilePhotoBase64,
modifier = Modifier.size(42.dp)
)

Column(  
                modifier = Modifier  
                    .weight(1f)  
                    .padding(start = 10.dp)  
            ) {  
                Text(  
                    text = post.authorName.ifBlank { "Peejee User" },  
                    color = MaterialTheme.colorScheme.onSurface,  
                    fontSize = 14.sp,  
                    fontWeight = FontWeight.Bold  
                )  
                if (post.timeLabel.isNotBlank()) {  
                    Text(  
                        text = post.timeLabel,  
                        color = MaterialTheme.colorScheme.onSurfaceVariant,  
                        fontSize = 11.sp  
                    )  
                }  
            }  
        }  

        if (post.text.isNotBlank()) {  
            Text(  
                text = post.text,  
                color = MaterialTheme.colorScheme.onSurface,  
                fontSize = 14.sp,  
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)  
            )  
        }  

        if (post.mediaUri.isNotBlank()) {  
            PeejeeFeedMedia(  
                mediaUri = post.mediaUri,  
                modifier = Modifier.fillMaxWidth()  
            )  
        }  

        Row(  
            modifier = Modifier  
                .fillMaxWidth()  
                .padding(horizontal = 8.dp, vertical = 5.dp),  
            horizontalArrangement = Arrangement.SpaceEvenly,  
            verticalAlignment = Alignment.CenterVertically  
        ) {  
            FeedAction(  
                label = if (post.isLikedByCurrentUser) "Liked ${post.likeCount}" else "Like ${post.likeCount}",  
                onClick = onLikeClick,  
                highlighted = post.isLikedByCurrentUser  
            )  
            FeedAction(  
                label = "Comment ${post.commentCount}",  
                onClick = onCommentClick  
            )  
            FeedAction(  
                label = if (post.isSavedByCurrentUser) "Saved" else "Save",  
                onClick = onSaveClick,  
                highlighted = post.isSavedByCurrentUser  
            )  
            FeedAction(  
                label = "Share ${post.shareCount}",  
                onClick = onShareClick  
            )  
        }  
    }  
}

}

@Composable
private fun FeedAction(
label: String,
onClick: () -> Unit,
highlighted: Boolean = false
) {
Surface(
modifier = Modifier
.clip(RoundedCornerShape(10.dp))
.background(if (highlighted) PeejeeFeedPalePurple else Color.Transparent),
onClick = onClick,
color = if (highlighted) PeejeeFeedPalePurple else Color.Transparent,
shape = RoundedCornerShape(10.dp)
) {
Text(
text = label,
color = if (highlighted) PeejeeFeedPurple else MaterialTheme.colorScheme.onSurfaceVariant,
fontSize = 11.sp,
fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
modifier = Modifier.padding(horizontal = 7.dp, vertical = 9.dp)
)
}
}

@Composable
private fun PeejeeFeedMedia(
mediaUri: String,
modifier: Modifier = Modifier
) {
val bitmap = remember(mediaUri) {
try {
val decoded = Uri.parse(mediaUri)
if (decoded.scheme == "data") {
val base64Part = mediaUri.substringAfter("base64,", "")
if (base64Part.isBlank()) null else {
val bytes = Base64.decode(base64Part, Base64.DEFAULT)
BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
} else {
null
}
} catch (_: Exception) {
null
}
}

Box(  
    modifier = modifier  
        .fillMaxWidth()  
        .background(MaterialTheme.colorScheme.surfaceVariant),  
    contentAlignment = Alignment.Center  
) {  
    if (bitmap != null) {  
        Image(  
            bitmap = bitmap.asImageBitmap(),  
            contentDescription = "Post media",  
            modifier = Modifier.fillMaxWidth(),  
            contentScale = ContentScale.Crop  
        )  
    } else {  
        Text(  
            text = "Media preview will appear when connected to Peejee's media storage.",  
            color = MaterialTheme.colorScheme.onSurfaceVariant,  
            fontSize = 12.sp,  
            modifier = Modifier.padding(16.dp)  
        )  
    }  
}

}

@Composable
private fun PeejeeFeedProfilePhoto(
photoBase64: String,
modifier: Modifier
) {
val bitmap = remember(photoBase64) {
try {
if (photoBase64.isBlank()) null
else {
val cleaned = photoBase64.substringAfter("base64,", photoBase64)
val bytes = Base64.decode(cleaned, Base64.DEFAULT)
BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
} catch (_: Exception) {
null
}
}

Surface(  
    modifier = modifier,  
    shape = CircleShape,  
    color = PeejeeFeedPalePurple  
) {  
    Box(  
        modifier = Modifier  
            .size(42.dp)  
            .clip(CircleShape),  
        contentAlignment = Alignment.Center  
    ) {  
        if (bitmap != null) {  
            Image(  
                bitmap = bitmap.asImageBitmap(),  
                contentDescription = "Post author's profile photo",  
                modifier = Modifier  
                    .size(42.dp)  
                    .clip(CircleShape),  
                contentScale = ContentScale.Crop  
            )  
        } else {  
            Text(text = "👤", fontSize = 21.sp)  
        }  
    }  
}

}
