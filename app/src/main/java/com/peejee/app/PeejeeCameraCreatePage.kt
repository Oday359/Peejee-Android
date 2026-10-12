package com.peejee.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PeejeePurple = Color(0xFF7138D8)
private val PeejeeDark = Color(0xFF08070C)

@Composable
fun PeejeeCameraCreatePage(
onPostCreated: () -> Unit,
paddingValues: PaddingValues
) {
val context = LocalContext.current
val lifecycleOwner = LocalLifecycleOwner.current
val scope = rememberCoroutineScope()
val storage = remember { FirebaseStorage.getInstance() }
val firestore = remember { FirebaseFirestore.getInstance() }
val auth = remember { FirebaseAuth.getInstance() }

var page by remember { mutableStateOf("CAMERA") }  
var captureMode by remember { mutableStateOf("15s") }  
var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }  
var flashOn by remember { mutableStateOf(false) }  
var timerSeconds by remember { mutableIntStateOf(0) }  
var selectedEffect by remember { mutableStateOf("Original") }  
var selectedFilter by remember { mutableStateOf("Natural") }  
var selectedMedia by remember { mutableStateOf<Uri?>(null) }  
var selectedMediaType by remember { mutableStateOf("") }  
var selectedSound by remember { mutableStateOf<Uri?>(null) }  
var caption by remember { mutableStateOf("") }  
var textPost by remember { mutableStateOf("") }  
var showTools by remember { mutableStateOf(true) }  
var publishing by remember { mutableStateOf(false) }  
var recording by remember { mutableStateOf(false) }  
var recordingSeconds by remember { mutableIntStateOf(0) }  
var error by remember { mutableStateOf("") }  
var cameraPermission by remember {  
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)  
}  
var audioPermission by remember {  
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)  
}  
var previewView by remember { mutableStateOf<PreviewView?>(null) }  
var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }  
var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }  
var boundCamera by remember { mutableStateOf<Camera?>(null) }  
var activeRecording by remember { mutableStateOf<Recording?>(null) }  
var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }  
var showGalleryChoice by remember { mutableStateOf(false) }  

val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->  
    cameraPermission = grants[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED  
    audioPermission = grants[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED  
}  
val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->  
    if (uri != null) { selectedMedia = uri; selectedMediaType = "photo"; error = "" }  
}  
val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->  
    if (uri != null) { selectedMedia = uri; selectedMediaType = "video"; page = "PREVIEW"; error = "" }  
}  
val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->  
    if (uri != null) { selectedSound = uri; error = "Sound selected. Audio mixing into videos will be added in a later update." }  
}  

fun bindCamera() {  
    if (!cameraPermission) return  
    val providerFuture = ProcessCameraProvider.getInstance(context)  
    providerFuture.addListener({  
        try {  
            val provider = providerFuture.get()  
            cameraProvider = provider  
            val pv = previewView ?: return@addListener  
            val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }  
            val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()  
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()  
            val video = VideoCapture.withOutput(recorder)  
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()  
            provider.unbindAll()  
            boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture, video)  
            imageCapture = capture  
            videoCapture = video  
            if (flashOn && boundCamera?.cameraInfo?.hasFlashUnit() == true) boundCamera?.cameraControl?.enableTorch(true)  
        } catch (e: Exception) {  
            error = "Camera could not start: ${e.localizedMessage ?: "Please try again."}"  
        }  
    }, ContextCompat.getMainExecutor(context))  
}  

LaunchedEffect(cameraPermission, lensFacing, previewView) {  
    if (cameraPermission && previewView != null && page == "CAMERA") bindCamera()  
}  
LaunchedEffect(flashOn, boundCamera) {  
    val camera = boundCamera  
    if (camera?.cameraInfo?.hasFlashUnit() == true) camera.cameraControl.enableTorch(flashOn)  
}  
LaunchedEffect(recording) {  
    if (recording) {  
        recordingSeconds = 0  
        val limit = when (captureMode) { "15s" -> 15; "60s" -> 60; "10m" -> 600; else -> 600 }  
        while (recording && recordingSeconds < limit) { delay(1000); recordingSeconds++ }  
        if (recording && recordingSeconds >= limit) activeRecording?.stop()  
    }  
}  

fun startRecording() {  
    val capture = videoCapture ?: run { error = "Camera is still starting. Please try again."; return }  
    if (!audioPermission) {  
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))  
        error = "Allow microphone access to record a video with sound, then tap record again."  
        return  
    }  
    val name = "peejee_${System.currentTimeMillis()}.mp4"  
    val values = ContentValues().apply {  
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)  
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")  
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Peejee")  
    }  
    val output = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()  
    var pending: PendingRecording = capture.output.prepareRecording(context, output).withAudioEnabled()  
    activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->  
        when (event) {  
            is VideoRecordEvent.Start -> { recording = true; error = "" }  
            is VideoRecordEvent.Finalize -> {  
                recording = false  
                activeRecording = null  
                if (!event.hasError() && event.outputResults.outputUri != Uri.EMPTY) {  
                    selectedMedia = event.outputResults.outputUri  
                    selectedMediaType = "video"  
                    page = "PREVIEW"  
                } else error = "Video recording failed. Please try again."  
            }  
        }  
    }  
}  

fun takePhoto() {  
    val capture = imageCapture ?: run { error = "Camera is still starting. Please try again."; return }  
    val values = ContentValues().apply {  
        put(MediaStore.MediaColumns.DISPLAY_NAME, "peejee_${System.currentTimeMillis()}.jpg")  
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")  
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Peejee")  
    }  
    val output = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()  
    capture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {  
        override fun onImageSaved(result: ImageCapture.OutputFileResults) {  
            selectedMedia = result.savedUri  
            selectedMediaType = "photo"  
            page = "PREVIEW"  
            error = ""  
        }  
        override fun onError(exception: ImageCaptureException) { error = "Photo capture failed: ${exception.localizedMessage ?: "Please try again."}" }  
    })  
}  

fun publishPost() {  
    val user = auth.currentUser  
    if (user == null) { error = "Please log in again."; return }  
    if (selectedMedia == null && caption.isBlank() && textPost.isBlank()) { error = "Take a photo, record a video, or write a text post first."; return }  
    publishing = true  
    error = ""  
    firestore.collection("users").document(user.uid).get().addOnSuccessListener { userDoc ->  
        val name = userDoc.getString("name") ?: userDoc.getString("username") ?: "Peejee User"  
        fun savePost(mediaUrl: String, mediaType: String) {  
            val postRef = firestore.collection("posts").document()  
            val post = hashMapOf<String, Any>(  
                "postId" to postRef.id,  
                "userId" to user.uid,  
                "userName" to name,  
                "text" to (if (textPost.isNotBlank()) textPost.trim() else caption.trim()),  
                "likes" to 0,  
                "likedBy" to emptyMap<String, Boolean>(),  
                "commentCount" to 0,  
                "shareCount" to 0,  
                "timestamp" to System.currentTimeMillis()  
            )  
            if (mediaUrl.isNotBlank()) { post["mediaType"] = mediaType; post["mediaUrl"] = mediaUrl }  
            postRef.set(post).addOnSuccessListener {  
                publishing = false  
                onPostCreated()  
            }.addOnFailureListener { e -> publishing = false; error = e.localizedMessage ?: "Could not publish post." }  
        }  
        val uri = selectedMedia  
        if (uri == null) savePost("", "") else {  
            val path = "posts/${user.uid}/${System.currentTimeMillis()}_${if (selectedMediaType == "video") "video.mp4" else "photo.jpg"}"  
            storage.reference.child(path).putFile(uri).continueWithTask { task ->  
                if (!task.isSuccessful) throw (task.exception ?: Exception("Upload failed"))  
                storage.reference.child(path).downloadUrl  
            }.addOnSuccessListener { downloadUri -> savePost(downloadUri.toString(), selectedMediaType) }  
                .addOnFailureListener { e -> publishing = false; error = "Media upload failed: ${e.localizedMessage ?: "Please try again."}" }  
        }  
    }.addOnFailureListener { e -> publishing = false; error = e.localizedMessage ?: "Could not load your profile." }  
}  

if (showGalleryChoice) AlertDialog(  
    onDismissRequest = { showGalleryChoice = false },  
    title = { Text("Choose media") },  
    text = { Text("Choose a photo or a video from your phone.") },  
    confirmButton = { TextButton(onClick = { showGalleryChoice = false; imagePicker.launch("image/*") }) { Text("Photo") } },  
    dismissButton = { TextButton(onClick = { showGalleryChoice = false; videoPicker.launch("video/*") }) { Text("Video") } }  
)  

Column(Modifier.fillMaxSize().background(PeejeeDark).padding(paddingValues)) {  
    if (page == "PREVIEW") {  
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {  
            Text("✕", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable { selectedMedia = null; selectedMediaType = ""; page = "CAMERA"; error = "" })  
            Spacer(Modifier.width(18.dp))  
            Text("Preview your post", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)  
        }  
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF17131F)), contentAlignment = Alignment.Center) {  
            if (selectedMediaType == "photo") {  
                AsyncImage(model = selectedMedia, contentDescription = "Selected photo", modifier = Modifier.fillMaxSize())  
            } else if (selectedMediaType == "video") {  
                AndroidView(factory = { ctx -> VideoView(ctx).apply { setVideoURI(selectedMedia); setOnPreparedListener { it.isLooping = true; start() } } }, modifier = Modifier.fillMaxSize())  
            }  
        }  
        OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Add a caption", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth().padding(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PeejeePurple, unfocusedBorderColor = Color.Gray))  
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF8080), modifier = Modifier.padding(horizontal = 16.dp))  
        Button(onClick = { publishPost() }, enabled = !publishing, modifier = Modifier.fillMaxWidth().padding(14.dp), colors = ButtonDefaults.buttonColors(containerColor = PeejeePurple)) { Text(if (publishing) "Uploading and publishing…" else "Publish to Peejee") }  
    } else if (page == "TEXT") {  
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {  
            Text("‹", color = Color.White, fontSize = 32.sp, modifier = Modifier.clickable { page = "CAMERA" })  
            Spacer(Modifier.width(14.dp)); Text("Create a text post", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)  
        }  
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF201A2B)).padding(18.dp)) {  
            OutlinedTextField(value = textPost, onValueChange = { textPost = it }, placeholder = { Text("Write your text…", color = Color.Gray) }, modifier = Modifier.fillMaxSize(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PeejeePurple, unfocusedBorderColor = Color.DarkGray))  
        }  
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF8080), modifier = Modifier.padding(16.dp))  
        Button(onClick = { publishPost() }, enabled = !publishing && textPost.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PeejeePurple)) { Text(if (publishing) "Publishing…" else "Publish text post") }  
    } else if (page == "CREATE") {  
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {  
            Text("‹", color = Color.White, fontSize = 32.sp, modifier = Modifier.clickable { page = "CAMERA" })  
            Spacer(Modifier.width(14.dp)); Text("Create", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)  
        }  
        Column(Modifier.weight(1f).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {  
            Text("Create for Peejee", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)  
            Text("Choose a photo or video to start creating a post.", color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))  
            Button(onClick = { showGalleryChoice = true }, colors = ButtonDefaults.buttonColors(containerColor = PeejeePurple)) { Text("Choose photo or video") }  
            OutlinedButton(onClick = { page = "TEXT" }) { Text("Create text post", color = Color.White) }  
        }  
    } else {  
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {  
            Text("✕", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable { onPostCreated() })  
            Spacer(Modifier.weight(1f))  
            TextButton(onClick = { soundPicker.launch("audio/*") }) { Text("♫ ${if (selectedSound == null) "Add sound" else "Sound selected"}", color = Color.White, fontWeight = FontWeight.Bold) }  
        }  
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF17131F))) {  
            if (cameraPermission) {  
                AndroidView(factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); previewView = this }, modifier = Modifier.fillMaxSize())  
            } else {  
                Column(Modifier.align(Alignment.Center).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {  
                    Text("Peejee needs camera access", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)  
                    Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }, colors = ButtonDefaults.buttonColors(containerColor = PeejeePurple)) { Text("Allow camera") }  
                }  
            }  
            if (showTools) {  
                Column(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {  
                    CameraTool("↻", "Flip") { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT }  
                    CameraTool(if (flashOn) "⚡" else "ϟ", "Flash") { flashOn = !flashOn }  
                    CameraTool("◷", "Timer") { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }; error = if (timerSeconds == 0) "Timer off" else "Timer set to ${timerSeconds}s" }  
                    CameraTool("▣", "Layout") { error = "Layout tools will be expanded in a later update." }  
                    CameraTool("✦", "Effects") { selectedEffect = if (selectedEffect == "Original") "Glow" else "Original"; error = "Effect selected: $selectedEffect" }  
                    CameraTool("◉", "Filters") { selectedFilter = if (selectedFilter == "Natural") "Warm" else "Natural"; error = "Filter selected: $selectedFilter" }  
                }  
            }  
            if (recording) Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(CircleShape).background(Color.Red).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("● REC ${recordingSeconds}s", color = Color.White, fontWeight = FontWeight.Bold) }  
        }  
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {  
            listOf("10m", "60s", "15s", "PHOTO", "TEXT").forEach { mode ->  
                val chosen = captureMode == mode  
                Text(mode, color = if (chosen) PeejeeDark else Color.White, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 4.dp).clip(CircleShape).background(if (chosen) Color.White else Color.Transparent).clickable { captureMode = mode; if (mode == "TEXT") page = "TEXT" }.padding(horizontal = 9.dp, vertical = 7.dp), fontSize = 13.sp)  
            }  
        }  
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {  
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2434)).clickable { showGalleryChoice = true }, contentAlignment = Alignment.Center) {  
                if (selectedMedia != null && selectedMediaType == "photo") AsyncImage(model = selectedMedia, contentDescription = "Gallery thumbnail", modifier = Modifier.fillMaxSize()) else Text("▧", color = Color.White, fontSize = 30.sp)  
            }  
            Spacer(Modifier.weight(1f))  
            Box(Modifier.size(82.dp).clip(CircleShape).background(Color.White).padding(5.dp).clip(CircleShape).background(if (recording) Color(0xFF8B1538) else Color(0xFFFF2857)).clickable {  
                if (timerSeconds > 0 && captureMode == "PHOTO") scope.launch { delay(timerSeconds * 1000L); takePhoto() }  
                else if (captureMode == "PHOTO") takePhoto()  
                else if (recording) activeRecording?.stop() else startRecording()  
            }, contentAlignment = Alignment.Center) {  
                if (recording) Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color.White))  
            }  
            Spacer(Modifier.weight(1f))  
            Column(horizontalAlignment = Alignment.CenterHorizontally) {  
                Text("●", color = PeejeePurple, fontSize = 24.sp)  
                Text(selectedEffect, color = Color.White, fontSize = 10.sp)  
            }  
        }  
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {  
            items(listOf("Original", "Glow", "Warm", "Cool", "Soft")) { effect ->  
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedEffect = effect }) {  
                    Box(Modifier.size(48.dp).clip(CircleShape).background(if (selectedEffect == effect) PeejeePurple else Color(0xFF39313F)), contentAlignment = Alignment.Center) { Text(if (effect == "Original") "○" else "✦", color = Color.White, fontSize = 20.sp) }  
                    Text(effect, color = Color.White, fontSize = 10.sp)  
                }  
            }  
        }  
        Row(Modifier.fillMaxWidth().background(Color.Black).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {  
            Text("CAMERA", color = if (page == "CAMERA") Color.White else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { page = "CAMERA" })  
            Text("CREATE", color = if (page == "CREATE") Color.White else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { page = "CREATE" })  
        }  
        if (error.isNotBlank()) Text(error, color = Color(0xFFFFA6A6), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().background(Color.Black).padding(horizontal = 10.dp, vertical = 4.dp), textAlign = TextAlign.Center)  
    }  
}

}

@Composable
private fun CameraTool(icon: String, label: String, onClick: () -> Unit) {
Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp).clickable(onClick = onClick).padding(vertical = 2.dp)) {
Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x77000000)), contentAlignment = Alignment.Center) { Text(icon, color = Color.White, fontSize = 22.sp) }
Text(label, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center)
}
}
