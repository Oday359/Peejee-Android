package com.peejee.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

private fun formatVoicePreviewDuration(durationMs: Long): String {
val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
return String.format("%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
fun PeejeeVoiceMessages(
sending: Boolean,
onSendVoice: (String, Long, (Boolean) -> Unit) -> Unit,
modifier: Modifier = Modifier
) {
val context = LocalContext.current

var isRecording by remember { mutableStateOf(false) }  
var recordingStartedAt by remember { mutableStateOf(0L) }  
var recordingPath by remember { mutableStateOf("") }  
var previewPath by remember { mutableStateOf("") }  
var previewDurationMs by remember { mutableStateOf(0L) }  
var recorder by remember { mutableStateOf<MediaRecorder?>(null) }  
var player by remember { mutableStateOf<MediaPlayer?>(null) }  
var isPlaying by remember { mutableStateOf(false) }  
var sendingVoice by remember { mutableStateOf(false) }  
var voiceError by remember { mutableStateOf("") }  

fun stopPreview() {  
    val oldPlayer = player  
    player = null  
    isPlaying = false  
    try { oldPlayer?.stop() } catch (_: Exception) {}  
    try { oldPlayer?.release() } catch (_: Exception) {}  
}  

fun startRecording() {  
    if (isRecording || sending || sendingVoice) return  
    try {  
        val file = File(  
            context.cacheDir,  
            "peejee_recording_${System.currentTimeMillis()}.3gp"  
        )  
        val newRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {  
            MediaRecorder(context)  
        } else {  
            @Suppress("DEPRECATION")  
            MediaRecorder()  
        }  
        newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)  
        newRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)  
        newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)  
        newRecorder.setOutputFile(file.absolutePath)  
        newRecorder.prepare()  
        newRecorder.start()  
        recorder = newRecorder  
        recordingPath = file.absolutePath  
        recordingStartedAt = System.currentTimeMillis()  
        isRecording = true  
        voiceError = ""  
    } catch (e: Exception) {  
        try { recorder?.release() } catch (_: Exception) {}  
        recorder = null  
        isRecording = false  
        voiceError = e.message ?: "Could not start voice recording."  
    }  
}  

val permissionLauncher = rememberLauncherForActivityResult(  
    contract = ActivityResultContracts.RequestPermission()  
) { granted ->  
    if (granted) startRecording()  
    else voiceError = "Microphone permission is required to record a voice note."  
}  

fun requestRecording() {  
    val granted = ContextCompat.checkSelfPermission(  
        context,  
        Manifest.permission.RECORD_AUDIO  
    ) == PackageManager.PERMISSION_GRANTED  
    if (granted) startRecording()  
    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)  
}  

fun stopRecordingToPreview() {  
    if (!isRecording) return  
    val activeRecorder = recorder  
    val path = recordingPath  
    val duration = (System.currentTimeMillis() - recordingStartedAt).coerceAtLeast(0L)  
    isRecording = false  
    recorder = null  
    recordingPath = ""  
    try {  
        activeRecorder?.stop()  
    } catch (_: Exception) {  
        try { if (path.isNotBlank()) File(path).delete() } catch (_: Exception) {}  
        try { activeRecorder?.release() } catch (_: Exception) {}  
        voiceError = "Could not save the voice note. Please record again."  
        return  
    }  
    try { activeRecorder?.release() } catch (_: Exception) {}  
    val file = File(path)  
    if (path.isBlank() || !file.exists() || file.length() == 0L) {  
        try { file.delete() } catch (_: Exception) {}  
        voiceError = "The recording is empty. Please record again."  
        return  
    }  
    stopPreview()  
    if (previewPath.isNotBlank() && previewPath != path) {  
        try { File(previewPath).delete() } catch (_: Exception) {}  
    }  
    previewPath = path  
    previewDurationMs = duration  
    voiceError = ""  
}  

fun playPreview() {  
    if (previewPath.isBlank()) return  
    if (isPlaying) {  
        stopPreview()  
        return  
    }  
    stopPreview()  
    try {  
        val previewPlayer = MediaPlayer()  
        player = previewPlayer  
        previewPlayer.setDataSource(previewPath)  
        previewPlayer.setOnCompletionListener { finished ->  
            try { finished.release() } catch (_: Exception) {}  
            if (player === finished) {  
                player = null  
                isPlaying = false  
            }  
        }  
        previewPlayer.setOnErrorListener { failed, _, _ ->  
            try { failed.release() } catch (_: Exception) {}  
            if (player === failed) {  
                player = null  
                isPlaying = false  
            }  
            voiceError = "Could not play the voice-note preview."  
            true  
        }  
        previewPlayer.prepare()  
        previewPlayer.start()  
        isPlaying = true  
    } catch (e: Exception) {  
        stopPreview()  
        voiceError = e.message ?: "Could not preview the voice note."  
    }  
}  

fun discardPreview() {  
    stopPreview()  
    if (previewPath.isNotBlank()) {  
        try { File(previewPath).delete() } catch (_: Exception) {}  
    }  
    previewPath = ""  
    previewDurationMs = 0L  
    voiceError = ""  
}  

DisposableEffect(Unit) {  
    onDispose {  
        try { recorder?.stop() } catch (_: Exception) {}  
        try { recorder?.release() } catch (_: Exception) {}  
        stopPreview()  
    }  
}  

Column(modifier = modifier) {  
    if (isRecording) {  
        Card(modifier = Modifier.fillMaxWidth()) {  
            Row(  
                modifier = Modifier.fillMaxWidth().padding(8.dp),  
                horizontalArrangement = Arrangement.SpaceBetween  
            ) {  
                Text("🔴 Recording voice note…")  
                TextButton(onClick = { stopRecordingToPreview() }) {  
                    Text("⏹ Stop")  
                }  
            }  
        }  
    } else if (previewPath.isNotBlank()) {  
        Card(modifier = Modifier.fillMaxWidth()) {  
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {  
                Text("Voice note preview • ${formatVoicePreviewDuration(previewDurationMs)}")  
                Row(  
                    modifier = Modifier.fillMaxWidth(),  
                    horizontalArrangement = Arrangement.spacedBy(6.dp)  
                ) {  
                    Button(  
                        onClick = { playPreview() },  
                        enabled = !sending && !sendingVoice,  
                        modifier = Modifier.weight(1f)  
                    ) {  
                        Text(if (isPlaying) "⏹ Stop" else "▶ Preview")  
                    }  
                    TextButton(  
                        onClick = { discardPreview() },  
                        enabled = !sending && !sendingVoice  
                    ) { Text("Discard") }  
                    Button(  
                        onClick = {  
                            if (!sendingVoice) {  
                                stopPreview()  
                                sendingVoice = true  
                                voiceError = ""  
                                onSendVoice(previewPath, previewDurationMs) { success ->  
                                    sendingVoice = false  
                                    if (success) {  
                                        try { File(previewPath).delete() } catch (_: Exception) {}  
                                        previewPath = ""  
                                        previewDurationMs = 0L  
                                        voiceError = ""  
                                    } else {  
                                        voiceError = "Could not send. Your recording is saved; try again."  
                                    }  
                                }  
                            }  
                        },  
                        enabled = !sending && !sendingVoice,  
                        modifier = Modifier.weight(1f)  
                    ) { Text(if (sendingVoice) "Sending…" else "📤 Send") }  
                }  
            }  
        }  
    } else {  
        TextButton(  
            onClick = { requestRecording() },  
            enabled = !sending && !sendingVoice  
        ) {  
            Text("🎙️ Record voice note")  
        }  
    }  

    if (voiceError.isNotBlank()) {  
        Text(  
            text = voiceError,  
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)  
        )  
    }  
}

}
