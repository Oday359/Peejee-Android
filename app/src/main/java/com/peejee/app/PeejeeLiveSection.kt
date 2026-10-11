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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private val PeejeeLivePurple = Color(0xFF7B2FF7)
private val PeejeeLiveRed = Color(0xFFE53935)
private val PeejeeLivePalePurple = Color(0xFFF1E8FF)

data class PeejeeLiveItem(
val userId: String,
val displayName: String,
val profilePhotoBase64: String = "",
val liveTitle: String = "Live now"
)

/**

Live-section UI only. The parent supplies the live users and callbacks.

This component does not start a broadcast or query Firebase by itself.
*/
@Composable
fun PeejeeLiveSection(
liveItems: List<PeejeeLiveItem>,
onGoLiveClick: () -> Unit,
onLiveUserClick: (PeejeeLiveItem) -> Unit,
modifier: Modifier = Modifier
) {
Column(
modifier = modifier
.fillMaxWidth()
.padding(vertical = 10.dp)
) {
Row(
modifier = Modifier.fillMaxWidth(),
verticalAlignment = Alignment.CenterVertically
) {
Column(modifier = Modifier.weight(1f)) {
Text(
text = "Live",
color = MaterialTheme.colorScheme.onSurface,
fontSize = 18.sp,
fontWeight = FontWeight.Bold
)
Text(
text = "Watch creators who are live now",
color = MaterialTheme.colorScheme.onSurfaceVariant,
fontSize = 12.sp
)
}

Button(  
         onClick = onGoLiveClick,  
         shape = RoundedCornerShape(24.dp),  
         colors = ButtonDefaults.buttonColors(  
             containerColor = PeejeeLivePurple,  
             contentColor = Color.White  
         )  
     ) {  
         Text("● Go Live", fontWeight = FontWeight.SemiBold)  
     }  
 }  

 Spacer(modifier = Modifier.size(12.dp))  

 val uniqueLiveItems = liveItems.distinctBy { it.userId }.take(8)  

 if (uniqueLiveItems.isEmpty()) {  
     Surface(  
         modifier = Modifier.fillMaxWidth(),  
         shape = RoundedCornerShape(14.dp),  
         color = MaterialTheme.colorScheme.surfaceVariant  
     ) {  
         Text(  
             text = "No one is live right now. Start a live session when you're ready.",  
             color = MaterialTheme.colorScheme.onSurfaceVariant,  
             fontSize = 13.sp,  
             modifier = Modifier.padding(14.dp)  
         )  
     }  
 } else {  
     Row(  
         horizontalArrangement = Arrangement.spacedBy(12.dp),  
         verticalAlignment = Alignment.Top  
     ) {  
         uniqueLiveItems.forEach { liveItem ->  
             Column(  
                 horizontalAlignment = Alignment.CenterHorizontally,  
                 modifier = Modifier  
                     .width(82.dp)  
                     .clickable { onLiveUserClick(liveItem) }  
             ) {  
                 Box(  
                     modifier = Modifier.size(66.dp),  
                     contentAlignment = Alignment.Center  
                 ) {  
                     PeejeeLiveProfilePhoto(  
                         photoBase64 = liveItem.profilePhotoBase64,  
                         modifier = Modifier.size(58.dp)  
                     )  

                     Box(  
                         modifier = Modifier  
                             .align(Alignment.BottomCenter)  
                             .background(PeejeeLiveRed, RoundedCornerShape(5.dp))  
                             .padding(horizontal = 6.dp, vertical = 2.dp)  
                     ) {  
                         Text(  
                             text = "LIVE",  
                             color = Color.White,  
                             fontSize = 9.sp,  
                             fontWeight = FontWeight.Bold  
                         )  
                     }  
                 }  

                 Spacer(modifier = Modifier.size(5.dp))  

                 Text(  
                     text = liveItem.displayName,  
                     color = MaterialTheme.colorScheme.onSurface,  
                     fontSize = 11.sp,  
                     fontWeight = FontWeight.SemiBold,  
                     maxLines = 1,  
                     textAlign = TextAlign.Center  
                 )  

                 Text(  
                     text = liveItem.liveTitle,  
                     color = MaterialTheme.colorScheme.onSurfaceVariant,  
                     fontSize = 10.sp,  
                     maxLines = 1,  
                     textAlign = TextAlign.Center  
                 )  
             }  
         }  
     }  
 }

}
}


@Composable
private fun PeejeeLiveProfilePhoto(
photoBase64: String,
modifier: Modifier
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
    color = PeejeeLivePalePurple,  
    border = BorderStroke(3.dp, PeejeeLivePurple)  
) {  
    Box(  
        modifier = Modifier  
            .size(58.dp)  
            .clip(CircleShape),  
        contentAlignment = Alignment.Center  
    ) {  
        if (bitmap != null) {  
            Image(  
                bitmap = bitmap.asImageBitmap(),  
                contentDescription = "Live creator profile photo",  
                modifier = Modifier  
                    .size(58.dp)  
                    .clip(CircleShape)  
            )  
        } else {  
            Text(text = "👤", fontSize = 25.sp)  
        }  
    }  
}

}
