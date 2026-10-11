package com.peejee.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PeejeeActionsPurple = Color(0xFF7B2FF7)
private val PeejeeActionsPalePurple = Color(0xFFF1E8FF)

/**

Reusable post-action row.

The parent supplies callbacks so this component can be connected to the

app's existing like, comment, save and share logic without duplicating it.
*/
@Composable
fun PeejeePostActions(
likeCount: Int,
commentCount: Int,
shareCount: Int,
isLiked: Boolean,
isSaved: Boolean,
onLikeClick: () -> Unit,
onCommentClick: () -> Unit,
onSaveClick: () -> Unit,
onShareClick: () -> Unit,
modifier: Modifier = Modifier
) {
Row(
modifier = modifier,
horizontalArrangement = Arrangement.SpaceEvenly
) {
PeejeePostActionButton(
icon = if (isLiked) "♥" else "♡",
label = if (isLiked) "Liked" else "Like",
count = likeCount,
selected = isLiked,
onClick = onLikeClick
)
PeejeePostActionButton(
icon = "💬",
label = "Comment",
count = commentCount,
selected = false,
onClick = onCommentClick
)
PeejeePostActionButton(
icon = if (isSaved) "🔖" else "♧",
label = if (isSaved) "Saved" else "Save",
count = null,
selected = isSaved,
onClick = onSaveClick
)
PeejeePostActionButton(
icon = "↗",
label = "Share",
count = shareCount,
selected = false,
onClick = onShareClick
)
}
}


@Composable
private fun PeejeePostActionButton(
icon: String,
label: String,
count: Int?,
selected: Boolean,
onClick: () -> Unit
) {
Surface(
modifier = Modifier
.clip(RoundedCornerShape(10.dp))
.clickable(onClick = onClick)
.background(if (selected) PeejeeActionsPalePurple else Color.Transparent),
color = if (selected) PeejeeActionsPalePurple else Color.Transparent,
shape = RoundedCornerShape(10.dp)
) {
Row(
modifier = Modifier.padding(horizontal = 7.dp, vertical = 9.dp),
horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
Text(
text = icon,
color = if (selected) PeejeeActionsPurple else MaterialTheme.colorScheme.onSurfaceVariant,
fontSize = 14.sp
)
Text(
text = label + (count?.let { " $it" } ?: ""),
color = if (selected) PeejeeActionsPurple else MaterialTheme.colorScheme.onSurfaceVariant,
fontSize = 11.sp,
fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
textAlign = TextAlign.Center
)
}
}
}
