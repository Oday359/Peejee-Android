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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search

private val PeejeeHeaderPurple = Color(0xFF7B2FF7)

/**

Reusable header for the Peejee home screen.

This component is not connected to MainActivity yet. The parent screen must

pass the current user's name/photo and notification/search callbacks.
*/
@Composable
fun PeejeeHomeHeader(
userName: String,
profilePhotoBase64: String,
onProfileClick: () -> Unit,
onNotificationsClick: () -> Unit,
onSearchClick: () -> Unit,
modifier: Modifier = Modifier
) {
Column(modifier = modifier) {
Row(
verticalAlignment = Alignment.CenterVertically
) {
PeejeeHeaderProfilePhoto(
photoBase64 = profilePhotoBase64,
modifier = Modifier.size(46.dp),
onClick = onProfileClick
)

Spacer(modifier = Modifier.width(10.dp))  

     Column(modifier = Modifier.weight(1f)) {  
         Text(  
             text = "Peejee",  
             color = PeejeeHeaderPurple,  
             fontSize = 21.sp,  
             fontWeight = FontWeight.Bold  
         )  
         Text(  
             text = userName.ifBlank { "Peejee User" },  
             color = MaterialTheme.colorScheme.onSurfaceVariant,  
             fontSize = 13.sp,  
             maxLines = 1  
         )  
     }  

     IconButton(onClick = onSearchClick) {  
         Icon(  
             imageVector = Icons.Default.Search,  
             contentDescription = "Search",  
             tint = MaterialTheme.colorScheme.onSurface  
         )  
     }  

     IconButton(onClick = onNotificationsClick) {  
         Icon(  
             imageVector = Icons.Default.Notifications,  
             contentDescription = "Notifications",  
             tint = MaterialTheme.colorScheme.onSurface  
         )  
     }  
 }

}
}


@Composable
private fun PeejeeHeaderProfilePhoto(
photoBase64: String,
modifier: Modifier = Modifier,
onClick: () -> Unit
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
    modifier = modifier  
        .clip(CircleShape)  
        .clickable(onClick = onClick),  
    shape = CircleShape,  
    color = Color(0xFFF1E8FF),  
    border = BorderStroke(2.dp, PeejeeHeaderPurple)  
) {  
    Box(contentAlignment = Alignment.Center) {  
        if (bitmap != null) {  
            Image(  
                bitmap = bitmap.asImageBitmap(),  
                contentDescription = "Your profile photo",  
                modifier = Modifier  
                    .size(46.dp)  
                    .clip(CircleShape)  
            )  
        } else {  
            Text(text = "👤", fontSize = 22.sp)  
        }  
    }  
}

}
