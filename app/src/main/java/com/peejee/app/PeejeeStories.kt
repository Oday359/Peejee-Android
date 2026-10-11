package com.peejee.app

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PeejeeStoryPurple = Color(0xFF7B2FF7)
private val PeejeeStoryPalePurple = Color(0xFFF1E8FF)
private val PeejeeLiveRed = Color(0xFFE53935)

/**

Data used by the Peejee Stories row.

hasStory controls the purple ring; isLive controls the LIVE label.
*/
data class PeejeeStoryItem(
val id: String,
val displayName: String,
val profilePhotoBase64: String = "",
val hasStory: Boolean = false,
val isLive: Boolean = false,
val isCurrentUser: Boolean = false
)


/**

Standalone Stories UI. The caller supplies story items and click actions.

This file does not read/write Firebase or start a live stream by itself.
*/
@Composable
fun PeejeeStories(
stories: List<PeejeeStoryItem>,
onAddStoryClick: () -> Unit,
onStoryClick: (PeejeeStoryItem) -> Unit,
modifier: Modifier = Modifier
) {
Column(modifier = modifier) {
Text(
text = "Stories",
color = MaterialTheme.colorScheme.onSurface,
fontSize = 18.sp,
fontWeight = FontWeight.Bold
)

Spacer(modifier = Modifier.size(10.dp))  

 Row(  
     horizontalArrangement = Arrangement.spacedBy(14.dp),  
     verticalAlignment = Alignment.Top  
 ) {  
     PeejeeStoryAvatar(  
         item = PeejeeStoryItem(  
             id = "current-user-story",  
             displayName = "Your story",  
             hasStory = false,  
             isCurrentUser = true  
         ),  
         onClick = onAddStoryClick,  
         showAddMark = true  
     )  

     stories  
         .filterNot { it.isCurrentUser || it.id == "current-user-story" }  
         .distinctBy { it.id }  
         .take(6)  
         .forEach { story ->  
             PeejeeStoryAvatar(  
                 item = story,  
                 onClick = { onStoryClick(story) },  
                 showAddMark = false  
             )  
         }  
 }

}
}


@Composable
private fun PeejeeStoryAvatar(
item: PeejeeStoryItem,
onClick: () -> Unit,
showAddMark: Boolean
) {
Column(
horizontalAlignment = Alignment.CenterHorizontally,
modifier = Modifier
.width(66.dp)
.clickable(onClick = onClick)
) {
Box(
modifier = Modifier.size(60.dp),
contentAlignment = Alignment.Center
) {
PeejeeStoryProfilePhoto(
photoBase64 = item.profilePhotoBase64,
modifier = Modifier.size(54.dp),
hasStory = item.hasStory || item.isLive
)

if (showAddMark) {  
            Box(  
                modifier = Modifier  
                    .align(Alignment.BottomEnd)  
                    .size(21.dp)  
                    .clip(CircleShape)  
                    .background(PeejeeStoryPurple),  
                contentAlignment = Alignment.Center  
            ) {  
                Text(  
                    text = "+",  
                    color = Color.White,  
                    fontSize = 17.sp,  
                    fontWeight = FontWeight.Bold,  
                    textAlign = TextAlign.Center  
                )  
            }  
        }  

        if (item.isLive) {  
            Box(  
                modifier = Modifier  
                    .align(Alignment.BottomCenter)  
                    .background(PeejeeLiveRed, RoundedCornerShape(5.dp))  
                    .padding(horizontal = 5.dp, vertical = 1.dp)  
            ) {  
                Text(  
                    text = "LIVE",  
                    color = Color.White,  
                    fontSize = 9.sp,  
                    fontWeight = FontWeight.Bold  
                )  
            }  
        }  
    }  

    Spacer(modifier = Modifier.size(5.dp))  

    Text(  
        text = if (item.isCurrentUser) "Your story" else item.displayName,  
        color = MaterialTheme.colorScheme.onSurface,  
        fontSize = 11.sp,  
        maxLines = 1,  
        textAlign = TextAlign.Center  
    )  
}

}

@Composable
private fun PeejeeStoryProfilePhoto(
photoBase64: String,
modifier: Modifier,
hasStory: Boolean
) {
val bitmap = remember(photoBase64) {
try {
if (photoBase64.isBlank()) {
null
} else {
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
    color = PeejeeStoryPalePurple,  
    border = BorderStroke(  
        width = if (hasStory) 3.dp else 1.dp,  
        color = if (hasStory) PeejeeStoryPurple  
        else MaterialTheme.colorScheme.outlineVariant  
    )  
) {  
    Box(  
        modifier = Modifier  
            .size(54.dp)  
            .clip(CircleShape),  
        contentAlignment = Alignment.Center  
    ) {  
        if (bitmap != null) {  
            Image(  
                bitmap = bitmap.asImageBitmap(),  
                contentDescription = "Story profile photo",  
                modifier = Modifier  
                    .size(54.dp)  
                    .clip(CircleShape)  
            )  
        } else {  
            Text(text = "👤", fontSize = 24.sp)  
        }  
    }  
}

}
