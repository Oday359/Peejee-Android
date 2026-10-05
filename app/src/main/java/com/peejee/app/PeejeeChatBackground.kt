package com.peejee.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.max

const val PEEJEE_BG_PEEJEE = "peejee_style"
const val PEEJEE_BG_LOVE = "love_style"
const val PEEJEE_BG_GALAXY = "galaxy"
const val PEEJEE_BG_CITY = "city_night"
const val PEEJEE_BG_OCEAN = "ocean"
const val PEEJEE_BG_MOUNTAINS = "mountains"
const val PEEJEE_BG_FLOWERS = "flowers"
const val PEEJEE_BG_LEAVES = "leaves"
const val PEEJEE_BG_CUTE = "cute_doodle"
const val PEEJEE_BG_DARK = "dark"
const val PEEJEE_BG_PURPLE = "purple"
const val PEEJEE_BG_BLUE = "blue"
const val PEEJEE_BG_LIGHT = "light"
const val PEEJEE_BG_PHOTO = "my_photo"

private const val PEEJEE_BG_PREFS = "peejee_chat_backgrounds"

fun peejeeChatBackgroundKey(firstUserId: String, secondUserId: String): String {
return listOf(firstUserId, secondUserId).sorted().joinToString("__")
}

fun loadPeejeeChatBackground(context: Context, chatKey: String): String {
return context.getSharedPreferences(PEEJEE_BG_PREFS, Context.MODE_PRIVATE)
.getString("bg_$chatKey", PEEJEE_BG_PEEJEE) ?: PEEJEE_BG_PEEJEE
}

fun savePeejeeChatBackground(context: Context, chatKey: String, backgroundId: String) {
context.getSharedPreferences(PEEJEE_BG_PREFS, Context.MODE_PRIVATE)
.edit()
.putString("bg_$chatKey", backgroundId)
.apply()
}

fun loadPeejeeChatPhotoPath(context: Context, chatKey: String): String {
return context.getSharedPreferences(PEEJEE_BG_PREFS, Context.MODE_PRIVATE)
.getString("photo_$chatKey", "") ?: ""
}

fun savePeejeeChatPhotoPath(context: Context, chatKey: String, path: String) {
context.getSharedPreferences(PEEJEE_BG_PREFS, Context.MODE_PRIVATE)
.edit()
.putString("photo_$chatKey", path)
.apply()
}

fun copyPeejeeChatPhoto(context: Context, uri: Uri, chatKey: String): String? {
return try {
val directory = File(context.filesDir, "peejee_chat_backgrounds")
if (!directory.exists()) directory.mkdirs()

val target = File(directory, "chat_${chatKey.hashCode()}.jpg")  
    context.contentResolver.openInputStream(uri)?.use { input ->  
        target.outputStream().use { output -> input.copyTo(output) }  
    } ?: return null  

    target.absolutePath  
} catch (_: Exception) {  
    null  
}

}

private fun decodePeejeePhoto(path: String): Bitmap? {
if (path.isBlank()) return null

return try {  
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }  
    BitmapFactory.decodeFile(path, bounds)  

    var sample = 1  
    while (max(bounds.outWidth, bounds.outHeight) / sample > 1600) {  
        sample *= 2  
    }  

    val options = BitmapFactory.Options().apply { inSampleSize = sample }  
    BitmapFactory.decodeFile(path, options)  
} catch (_: Exception) {  
    null  
}

}

private fun heartPath(centerX: Float, centerY: Float, size: Float): Path {
val path = Path()
path.moveTo(centerX, centerY + size * 0.85f)
path.cubicTo(
centerX - size * 1.45f, centerY - size * 0.10f,
centerX - size * 0.90f, centerY - size * 1.15f,
centerX, centerY - size * 0.40f
)
path.cubicTo(
centerX + size * 0.90f, centerY - size * 1.15f,
centerX + size * 1.45f, centerY - size * 0.10f,
centerX, centerY + size * 0.85f
)
path.close()
return path
}

private fun drawPeejeePattern(
drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
accent: Color = Color(0xFF8A2BE2)
) {
with(drawScope) {
val w = size.width
val h = size.height
val stepX = 105f
val stepY = 115f

var row = 0  
    var y = 70f  
    while (y < h + 100f) {  
        var col = 0  
        var x = 35f + if (row % 2 == 0) 0f else 45f  
        while (x < w + 100f) {  
            drawCircle(  
                color = accent.copy(alpha = 0.10f),  
                radius = 28f,  
                center = Offset(x, y),  
                style = Stroke(width = 2.5f)  
            )  
            drawPath(  
                path = heartPath(x, y + 3f, 9f),  
                color = Color(0xFFB000FF).copy(alpha = 0.62f),  
                style = Stroke(width = 2.2f)  
            )  
            drawLine(  
                color = Color(0xFF7B00FF).copy(alpha = 0.60f),  
                start = Offset(x - 28f, y + 34f),  
                end = Offset(x + 3f, y + 2f),  
                strokeWidth = 2.2f  
            )  
            drawCircle(  
                color = Color(0xFFB000FF).copy(alpha = 0.65f),  
                radius = 4f,  
                center = Offset(x + 31f, y - 28f)  
            )  
            row += 0  
            col++  
            x += stepX  
        }  
        row++  
        y += stepY  
    }  
}

}

@Composable
fun PeejeeChatBackgroundLayer(
backgroundId: String,
photoPath: String,
modifier: Modifier = Modifier
) {
val photo = remember(photoPath) { decodePeejeePhoto(photoPath) }

Box(modifier = modifier) {  
    when (backgroundId) {  
        PEEJEE_BG_PHOTO -> {  
            if (photo != null) {  
                Image(  
                    bitmap = photo.asImageBitmap(),  
                    contentDescription = null,  
                    modifier = Modifier.fillMaxSize(),  
                    contentScale = ContentScale.Crop  
                )  
                Canvas(Modifier.fillMaxSize()) {  
                    drawRect(Color.Black.copy(alpha = 0.13f))  
                }  
            } else {  
                PeejeeChatBackgroundLayer(  
                    backgroundId = PEEJEE_BG_PEEJEE,  
                    photoPath = "",  
                    modifier = Modifier.fillMaxSize()  
                )  
            }  
        }  

        PEEJEE_BG_PEEJEE -> {  
            Canvas(  
                Modifier  
                    .fillMaxSize()  
                    .background(  
                        Brush.verticalGradient(  
                            listOf(  
                                Color(0xFF05000D),  
                                Color(0xFF10001F),  
                                Color(0xFF020006)  
                            )  
                        )  
                    )  
            ) {  
                drawPeejeePattern(this)  
            }  
        }  

        PEEJEE_BG_LOVE -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.verticalGradient(  
                        listOf(Color(0xFF13000E), Color(0xFF260019))  
                    )  
                )  
            ) {  
                var y = 70f  
                while (y < size.height + 100f) {  
                    var x = 45f  
                    while (x < size.width + 80f) {  
                        drawPath(  
                            heartPath(x, y, 16f),  
                            Color(0xFFFF3BA7).copy(alpha = 0.65f),  
                            style = Stroke(width = 3f)  
                        )  
                        x += 105f  
                    }  
                    y += 105f  
                }  
            }  
        }  

        PEEJEE_BG_GALAXY -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.radialGradient(  
                        colors = listOf(  
                            Color(0xFF5B16C9),  
                            Color(0xFF16002F),  
                            Color(0xFF020006)  
                        ),  
                        radius = 900f  
                    )  
                )  
            ) {  
                repeat(80) { index ->  
                    val x = ((index * 137) % 1000) / 1000f * size.width  
                    val y = ((index * 83) % 1000) / 1000f * size.height  
                    drawCircle(  
                        Color.White.copy(alpha = 0.30f + (index % 4) * 0.10f),  
                        radius = 1.5f + (index % 3),  
                        center = Offset(x, y)  
                    )  
                }  
            }  
        }  

        PEEJEE_BG_CITY -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.verticalGradient(  
                        listOf(Color(0xFF12002B), Color(0xFF020005))  
                    )  
                )  
            ) {  
                var x = 0f  
                var i = 0  
                while (x < size.width) {  
                    val buildingHeight = 130f + (i % 5) * 65f  
                    drawRect(  
                        Color(0xFF130A2B),  
                        topLeft = Offset(x, size.height - buildingHeight),  
                        size = Size(72f, buildingHeight)  
                    )  
                    var wy = size.height - buildingHeight + 20f  
                    while (wy < size.height - 20f) {  
                        drawRect(  
                            Color(0xFF9A4DFF).copy(alpha = 0.55f),  
                            topLeft = Offset(x + 12f, wy),  
                            size = Size(7f, 7f)  
                        )  
                        wy += 27f  
                    }  
                    x += 78f  
                    i++  
                }  
            }  
        }  

        PEEJEE_BG_OCEAN -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.verticalGradient(  
                        listOf(Color(0xFF003B63), Color(0xFF001421))  
                    )  
                )  
            ) {  
                repeat(9) { i ->  
                    val y = 80f + i * 105f  
                    drawLine(  
                        Color(0xFF5DD8FF).copy(alpha = 0.35f),  
                        Offset(0f, y),  
                        Offset(size.width, y + 18f),  
                        strokeWidth = 4f  
                    )  
                }  
            }  
        }  

        PEEJEE_BG_MOUNTAINS -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.verticalGradient(  
                        listOf(Color(0xFF243A65), Color(0xFF080D18))  
                    )  
                )  
            ) {  
                val path = Path().apply {  
                    moveTo(0f, size.height)  
                    lineTo(0f, size.height * 0.62f)  
                    lineTo(size.width * 0.28f, size.height * 0.34f)  
                    lineTo(size.width * 0.47f, size.height * 0.60f)  
                    lineTo(size.width * 0.68f, size.height * 0.28f)  
                    lineTo(size.width, size.height * 0.60f)  
                    lineTo(size.width, size.height)  
                    close()  
                }  
                drawPath(path, Color(0xFF111827))  
            }  
        }  

        PEEJEE_BG_FLOWERS -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.verticalGradient(  
                        listOf(Color(0xFF35154A), Color(0xFF110718))  
                    )  
                )  
            ) {  
                repeat(35) { i ->  
                    val x = ((i * 149) % 1000) / 1000f * size.width  
                    val y = ((i * 97) % 1000) / 1000f * size.height  
                    drawCircle(Color(0xFFFF6BB5).copy(alpha = 0.55f), 12f, Offset(x, y))  
                    drawCircle(Color(0xFFFFC6E5).copy(alpha = 0.7f), 5f, Offset(x, y))  
                }  
            }  
        }  

        PEEJEE_BG_LEAVES -> {  
            Canvas(  
                Modifier.fillMaxSize().background(  
                    Brush.verticalGradient(  
                        listOf(Color(0xFF09291C), Color(0xFF03120B))  
                    )  
                )  
            ) {  
                repeat(28) { i ->  
                    val x = ((i * 181) % 1000) / 1000f * size.width  
                    val y = ((i * 113) % 1000) / 1000f * size.height  
                    rotate((i % 4) * 18f, Offset(x, y)) {  
                        drawOval(  
                            Color(0xFF49D17D).copy(alpha = 0.45f),  
                            topLeft = Offset(x - 18f, y - 7f),  
                            size = Size(36f, 14f)  
                        )  
                    }  
                }  
            }  
        }  

        PEEJEE_BG_CUTE -> {  
            Canvas(  
                Modifier.fillMaxSize().background(Color(0xFFFFF4FA))  
            ) {  
                drawPeejeePattern(this, Color(0xFFFF72C5))  
            }  
        }  

        PEEJEE_BG_DARK -> Box(Modifier.fillMaxSize().background(Color(0xFF101116)))  
        PEEJEE_BG_PURPLE -> Box(Modifier.fillMaxSize().background(Color(0xFF2B124C)))  
        PEEJEE_BG_BLUE -> Box(Modifier.fillMaxSize().background(Color(0xFF0D2942)))  
        PEEJEE_BG_LIGHT -> Box(Modifier.fillMaxSize().background(Color(0xFFF1EFF7)))  
        else -> Box(Modifier.fillMaxSize().background(Color(0xFF10001F)))  
    }  
}

}

@Composable
fun PeejeeChatBackgroundPicker(
selectedBackground: String,
onBackgroundSelected: (String) -> Unit,
onChoosePhoto: () -> Unit,
onDismiss: () -> Unit
) {
ModalBottomSheet(onDismissRequest = onDismiss) {
var tab = remember { androidx.compose.runtime.mutableStateOf("Peejee Styles") }

Column(  
        modifier = Modifier  
            .fillMaxWidth()  
            .padding(bottom = 24.dp)  
    ) {  
        Text(  
            "Chat Background",  
            style = MaterialTheme.typography.titleLarge,  
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)  
        )  

        Row(  
            modifier = Modifier  
                .fillMaxWidth()  
                .horizontalScroll(rememberScrollState())  
                .padding(horizontal = 12.dp),  
            horizontalArrangement = Arrangement.spacedBy(8.dp)  
        ) {  
            listOf("Peejee Styles", "Colors", "Nature", "Minimal", "My Photos").forEach { name ->  
                FilterChip(  
                    selected = tab.value == name,  
                    onClick = { tab.value = name },  
                    label = { Text(name) }  
                )  
            }  
        }  

        Spacer(Modifier.height(12.dp))  

        when (tab.value) {  
            "Peejee Styles" -> BackgroundChoiceRow(  
                choices = listOf(  
                    PEEJEE_BG_PEEJEE to "Peejee Style",  
                    PEEJEE_BG_LOVE to "Love Style",  
                    PEEJEE_BG_GALAXY to "Galaxy",  
                    PEEJEE_BG_CITY to "City Night"  
                ),  
                selected = selectedBackground,  
                onSelected = onBackgroundSelected  
            )  

            "Colors" -> BackgroundChoiceRow(  
                choices = listOf(  
                    PEEJEE_BG_PURPLE to "Purple",  
                    PEEJEE_BG_BLUE to "Blue",  
                    PEEJEE_BG_DARK to "Dark",  
                    PEEJEE_BG_LIGHT to "Light"  
                ),  
                selected = selectedBackground,  
                onSelected = onBackgroundSelected  
            )  

            "Nature" -> BackgroundChoiceRow(  
                choices = listOf(  
                    PEEJEE_BG_OCEAN to "Ocean",  
                    PEEJEE_BG_MOUNTAINS to "Mountains",  
                    PEEJEE_BG_FLOWERS to "Flowers",  
                    PEEJEE_BG_LEAVES to "Leaves"  
                ),  
                selected = selectedBackground,  
                onSelected = onBackgroundSelected  
            )  

            "Minimal" -> BackgroundChoiceRow(  
                choices = listOf(  
                    PEEJEE_BG_DARK to "Dark",  
                    PEEJEE_BG_LIGHT to "Light",  
                    PEEJEE_BG_PURPLE to "Soft Purple"  
                ),  
                selected = selectedBackground,  
                onSelected = onBackgroundSelected  
            )  

            "My Photos" -> {  
                Button(  
                    onClick = onChoosePhoto,  
                    modifier = Modifier  
                        .fillMaxWidth()  
                        .padding(horizontal = 20.dp)  
                ) {  
                    Text("📷 Choose My Photo")  
                }  

                Text(  
                    "Your photo is used only as the chat wallpaper. Peejee chat bubble colors stay unchanged.",  
                    fontSize = 12.sp,  
                    modifier = Modifier.padding(20.dp)  
                )  
            }  
        }  
    }  
}

}

@Composable
private fun BackgroundChoiceRow(
choices: List<Pair<String, String>>,
selected: String,
onSelected: (String) -> Unit
) {
Row(
modifier = Modifier
.fillMaxWidth()
.horizontalScroll(rememberScrollState())
.padding(horizontal = 16.dp),
horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
choices.forEach { (id, title) ->
Card(
modifier = Modifier.size(width = 125.dp, height = 105.dp)
) {
Column(
modifier = Modifier
.fillMaxSize()
.padding(5.dp),
horizontalAlignment = Alignment.CenterHorizontally
) {
PeejeeChatBackgroundLayer(
backgroundId = id,
photoPath = "",
modifier = Modifier
.fillMaxWidth()
.height(65.dp)
)
TextButton(onClick = { onSelected(id) }) {
Text(if (selected == id) "✓ $title" else title, fontSize = 11.sp)
}
}
}
}
}
}
