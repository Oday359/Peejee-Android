package com.peejee.app

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**

Small, separate building block for the Peejee home-screen header.

This file is intentionally not connected to MainActivity or the existing HomeScreen yet.

Existing app screens and files are therefore left unchanged.
*/
data class PeejeeStoryPreview(
val id: String,
val name: String,
val profilePhotoBase64: String = "",
val hasStory: Boolean = false,
val isLive: Boolean = false
)


private val PeejeePurple = Color(0xFF7B2FF7)
private val PeejeeLightPurple = Color(0xFFF1E8FF)

@Composable
fun PeejeeHomeTopSection(
userName: String,
currentUserProfilePhotoBase64: String,
stories: List<PeejeeStoryPreview>,
onGoLiveClick: () -> Unit,
onProfileClick: () -> Unit,
onStoryClick: (PeejeeStoryPreview) -> Unit,
modifier: Modifier = Modifier
) {
Column(
modifier = modifier
.fillMaxWidth()
.background(MaterialTheme.colorScheme.surface)
.padding(horizontal = 16.dp, vertical = 12.dp)
) {
Row(
modifier = Modifier.fillMaxWidth(),
verticalAlignment = Alignment.CenterVertically
) {
ProfilePhotoCircle(
photoBase64 = currentUserProfilePhotoBase64,
size = 54,
hasStory = false,
onClick = onProfileClick
)

Spacer(modifier = Modifier.width(12.dp))  

        Column(modifier = Modifier.weight(1f)) {  
            Text(  
                text = "Welcome,",  
                color = MaterialTheme.colorScheme.onSurfaceVariant,  
                fontSize = 13.sp  
            )  
            Text(  
                text = userName.ifBlank { "Peejee User" },  
                color = MaterialTheme.colorScheme.onSurface,  
                fontSize = 18.sp,  
                fontWeight = FontWeight.Bold  
            )  
        }  

        Button(  
            onClick = onGoLiveClick,  
            shape = RoundedCornerShape(24.dp),  
            colors = ButtonDefaults.buttonColors(  
                containerColor = PeejeePurple,  
                contentColor = Color.White  
            )  
        ) {  
            Text("● Go Live", fontWeight = FontWeight.SemiBold)  
        }  
    }  

    Spacer(modifier = Modifier.size(18.dp))  

    Text(  
        text = "Stories",  
        fontSize = 17.sp,  
        fontWeight = FontWeight.Bold,  
        color = MaterialTheme.colorScheme.onSurface  
    )  

    Spacer(modifier = Modifier.size(10.dp))  

    if (stories.isEmpty()) {  
        Text(  
            text = "Stories from people you follow will appear here.",  
            color = MaterialTheme.colorScheme.onSurfaceVariant,  
            fontSize = 13.sp,  
            modifier = Modifier.padding(vertical = 8.dp)  
        )  
    } else {  
        Row(  
            modifier = Modifier.fillMaxWidth(),  
            horizontalArrangement = Arrangement.spacedBy(14.dp),  
            verticalAlignment = Alignment.Top  
        ) {  
            stories.distinctBy { it.id }.take(6).forEach { story ->  
                Column(  
                    horizontalAlignment = Alignment.CenterHorizontally,  
                    modifier = Modifier.width(62.dp)  
                ) {  
                    Box {  
                        ProfilePhotoCircle(  
                            photoBase64 = story.profilePhotoBase64,  
                            size = 54,  
                            hasStory = story.hasStory,  
                            onClick = { onStoryClick(story) }  
                        )  

                        if (story.isLive) {  
                            Box(  
                                modifier = Modifier  
                                    .align(Alignment.BottomCenter)  
                                    .background(Color(0xFFE53935), RoundedCornerShape(8.dp))  
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
                        text = story.name,  
                        fontSize = 11.sp,  
                        color = MaterialTheme.colorScheme.onSurface,  
                        maxLines = 1  
                    )  
                }  
            }  
        }  
    }  
}

}

@Composable
private fun ProfilePhotoCircle(
photoBase64: String,
size: Int,
hasStory: Boolean,
onClick: () -> Unit
) {
val bitmap = remember(photoBase64) {
try {
if (photoBase64.isBlank()) {
null
} else {
val cleanValue = photoBase64.substringAfter("base64,", photoBase64)
val bytes = Base64.decode(cleanValue, Base64.DEFAULT)
BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
} catch (_: Exception) {
null
}
}

OutlinedButton(  
    onClick = onClick,  
    modifier = Modifier.size((size + if (hasStory) 6 else 2).dp),  
    shape = CircleShape,  
    border = BorderStroke(  
        width = if (hasStory) 3.dp else 1.dp,  
        color = if (hasStory) PeejeePurple else MaterialTheme.colorScheme.outlineVariant  
    ),  
    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),  
    colors = ButtonDefaults.outlinedButtonColors(  
        containerColor = if (bitmap == null) PeejeeLightPurple else MaterialTheme.colorScheme.surface  
    )  
) {  
    if (bitmap != null) {  
        Image(  
            bitmap = bitmap.asImageBitmap(),  
            contentDescription = "Profile photo",  
            modifier = Modifier  
                .size(size.dp)  
                .clip(CircleShape)  
        )  
    } else {  
        Box(  
            modifier = Modifier  
                .size(size.dp)  
                .clip(CircleShape)  
                .background(PeejeeLightPurple),  
            contentAlignment = Alignment.Center  
        ) {  
            Text(  
                text = "👤",  
                fontSize = 23.sp  
            )  
        }  
    }  
}

}
