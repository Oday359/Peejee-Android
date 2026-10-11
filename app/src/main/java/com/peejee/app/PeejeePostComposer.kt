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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PeejeeComposerPurple = Color(0xFF7B2FF7)
private val PeejeeComposerPalePurple = Color(0xFFF1E8FF)

/**

Post-composer UI only. The parent screen supplies callbacks so Peejee can

connect these buttons to its existing post-creation flow.
*/
@Composable
fun PeejeePostComposer(
userName: String,
profilePhotoBase64: String,
onCreatePostClick: () -> Unit,
onPhotoClick: () -> Unit,
onVideoClick: () -> Unit,
onFeelingClick: () -> Unit,
modifier: Modifier = Modifier
) {
Surface(
modifier = modifier.fillMaxWidth(),
shape = RoundedCornerShape(16.dp),
color = MaterialTheme.colorScheme.surface,
border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
shadowElevation = 1.dp
) {
Column(modifier = Modifier.padding(14.dp)) {
Row(
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
PeejeeComposerProfilePhoto(
photoBase64 = profilePhotoBase64,
modifier = Modifier.size(44.dp)
)

Surface(  
             modifier = Modifier  
                 .weight(1f)  
                 .clip(RoundedCornerShape(24.dp))  
                 .clickable(onClick = onCreatePostClick),  
             shape = RoundedCornerShape(24.dp),  
             color = MaterialTheme.colorScheme.surfaceVariant  
         ) {  
             Text(  
                 text = "What's on your mind, ${userName.ifBlank { "friend" }}?",  
                 color = MaterialTheme.colorScheme.onSurfaceVariant,  
                 fontSize = 14.sp,  
                 modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),  
                 maxLines = 2  
             )  
         }  
     }  

     Spacer(modifier = Modifier.size(12.dp))  

     Row(  
         modifier = Modifier.fillMaxWidth(),  
         horizontalArrangement = Arrangement.SpaceBetween,  
         verticalAlignment = Alignment.CenterVertically  
     ) {  
         ComposerAction(icon = "📷", label = "Photo", onClick = onPhotoClick)  
         ComposerDivider()  
         ComposerAction(icon = "🎥", label = "Video", onClick = onVideoClick)  
         ComposerDivider()  
         ComposerAction(icon = "😊", label = "Feeling", onClick = onFeelingClick)  
     }  
 }

}
}


@Composable
private fun ComposerAction(
icon: String,
label: String,
onClick: () -> Unit
) {
Row(
modifier = Modifier
.clip(RoundedCornerShape(10.dp))
.clickable(onClick = onClick)
.padding(horizontal = 5.dp, vertical = 8.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(5.dp)
) {
Text(text = icon, fontSize = 17.sp)
Text(
text = label,
color = MaterialTheme.colorScheme.onSurface,
fontSize = 12.sp,
fontWeight = FontWeight.Medium
)
}
}

@Composable
private fun ComposerDivider() {
Box(
modifier = Modifier
.size(width = 1.dp, height = 24.dp)
.background(MaterialTheme.colorScheme.outlineVariant)
)
}

@Composable
private fun PeejeeComposerProfilePhoto(
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
    color = PeejeeComposerPalePurple,  
    border = BorderStroke(2.dp, PeejeeComposerPurple)  
) {  
    Box(  
        modifier = Modifier  
            .size(44.dp)  
            .clip(CircleShape),  
        contentAlignment = Alignment.Center  
    ) {  
        if (bitmap != null) {  
            Image(  
                bitmap = bitmap.asImageBitmap(),  
                contentDescription = "Your profile photo",  
                modifier = Modifier  
                    .size(44.dp)  
                    .clip(CircleShape)  
            )  
        } else {  
            Text(text = "👤", fontSize = 21.sp)  
        }  
    }  
}

}
