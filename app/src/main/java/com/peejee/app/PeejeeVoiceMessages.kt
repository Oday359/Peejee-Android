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

@Composable
fun PeejeeVoiceMessages(
    sending: Boolean,
    onSendVoice: (
        String,
        Long,
        (Boolean) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var recording by remember {
        mutableStateOf(false)
    }

    var recordingPath by remember {
        mutableStateOf("")
    }

    var previewPath by remember {
        mutableStateOf("")
    }

    var recordingStartedAt by remember {
        mutableStateOf(0L)
    }

    var durationMs by remember {
        mutableStateOf(0L)
    }

    var recorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var playing by remember {
        mutableStateOf(false)
    }

    var sendingVoice by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf("")
    }

    fun stopPreview() {
        val activePlayer = player

        player = null
        playing = false

        try {
            activePlayer?.setOnCompletionListener(null)
            activePlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            activePlayer?.release()
        } catch (_: Exception) {
        }
    }

    fun discardRecording() {
        stopPreview()

        try {
            if (previewPath.isNotBlank()) {
                File(previewPath).delete()
            }
        } catch (_: Exception) {
        }

        previewPath = ""
        durationMs = 0L
        error = ""
    }

    fun startRecording() {
        if (recording || sending || sendingVoice) return

        try {
            discardRecording()

            val outputFile = File(
                context.cacheDir,
                "peejee_voice_${System.currentTimeMillis()}.3gp"
            )

            val newRecorder = MediaRecorder()

            try {
                newRecorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )

                newRecorder.setOutputFormat(
                    MediaRecorder.OutputFormat.THREE_GPP
                )

                newRecorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AMR_NB
                )

                newRecorder.setOutputFile(
                    outputFile.absolutePath
                )

                newRecorder.prepare()
                newRecorder.start()

                recorder = newRecorder
                recordingPath = outputFile.absolutePath
                recordingStartedAt = System.currentTimeMillis()
                recording = true
                error = ""

            } catch (exception: Exception) {
                try {
                    newRecorder.release()
                } catch (_: Exception) {
                }

                try {
                    outputFile.delete()
                } catch (_: Exception) {
                }

                throw exception
            }

        } catch (exception: Exception) {
            error = exception.message
                ?: "Could not start recording."
        }
    }

    fun stopRecording() {
        if (!recording) return

        val activeRecorder = recorder
        val path = recordingPath

        recording = false
        recorder = null
        recordingPath = ""

        try {
            activeRecorder?.stop()
            activeRecorder?.release()

            val outputFile = File(path)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                outputFile.delete()
                error = "The recording is empty. Please record again."
                return
            }

            durationMs = (
                System.currentTimeMillis() - recordingStartedAt
            ).coerceAtLeast(0L)

            previewPath = path
            error = ""

        } catch (_: Exception) {
            try {
                activeRecorder?.release()
            } catch (_: Exception) {
            }

            try {
                File(path).delete()
            } catch (_: Exception) {
            }

            previewPath = ""
            error = "Could not finish recording. Please try again."
        }
    }

    fun playPreview() {
        if (previewPath.isBlank()) return

        stopPreview()

        try {
            val file = File(previewPath)

            if (!file.exists()) {
                error = "Recording not found. Please record again."
                previewPath = ""
                return
            }

            val newPlayer = MediaPlayer()

            player = newPlayer

            newPlayer.setOnCompletionListener { completedPlayer ->
                try {
                    completedPlayer.release()
                } catch (_: Exception) {
                }

                if (player === completedPlayer) {
                    player = null
                    playing = false
                }
            }

            newPlayer.setDataSource(file.absolutePath)
            newPlayer.prepare()
            newPlayer.start()

            playing = true
            error = ""

        } catch (exception: Exception) {
            stopPreview()

            error = exception.message
                ?: "Could not play the preview."
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startRecording()
            } else {
                error = "Please allow microphone permission to record."
            }
        }

    fun requestRecordingPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startRecording()
        } else {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }

            try {
                recorder?.release()
            } catch (_: Exception) {
            }

            try {
                player?.setOnCompletionListener(null)
                player?.stop()
            } catch (_: Exception) {
            }

            try {
                player?.release()
            } catch (_: Exception) {
            }

            try {
                if (recordingPath.isNotBlank()) {
                    File(recordingPath).delete()
                }
            } catch (_: Exception) {
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Peejee Voice Note")

            when {
                recording -> {
                    Text("🔴 Recording... Press Stop when ready.")

                    Button(
                        onClick = { stopRecording() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !sending
                    ) {
                        Text("⏹ Stop and Preview")
                    }
                }

                previewPath.isNotBlank() -> {
                    Text(
                        "Voice note ready — ${
                            durationMs / 1000
                        } seconds"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (playing) {
                                    stopPreview()
                                } else {
                                    playPreview()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !sending && !sendingVoice
                        ) {
                            Text(
                                if (playing) "⏹ Stop Preview"
                                else "▶ Preview"
                            )
                        }

                        Button(
                            onClick = { discardRecording() },
                            modifier = Modifier.weight(1f),
                            enabled = !sending && !sendingVoice
                        ) {
                            Text("🗑 Discard")
                        }
                    }

                    Button(
                        onClick = {
                            val path = previewPath
                            val duration = durationMs

                            if (
                                path.isBlank() ||
                                !File(path).exists()
                            ) {
                                error = "Recording not found. Please record again."
                                return@Button
                            }

                            stopPreview()
                            sendingVoice = true
                            error = ""

                            onSendVoice(
                                path,
                                duration
                            ) { success ->
                                sendingVoice = false

                                if (success) {
                                    try {
                                        File(path).delete()
                                    } catch (_: Exception) {
                                    }

                                    previewPath = ""
                                    durationMs = 0L
                                    error = ""
                                } else {
                                    // Keep the recording so the user
                                    // can retry or discard it.
                                    error = "Could not send. Your recording is saved; try again."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !sending && !sendingVoice
                    ) {
                        Text(
                            if (sendingVoice || sending) {
                                "Sending..."
                            } else {
                                "📤 Send Voice Note"
                            }
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            requestRecordingPermission()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !sending && !sendingVoice
                    ) {
                        Text("🎙 Record Voice Note")
                    }
                }
            }

            if (error.isNotBlank()) {
                Text(error)
            }
        }
    }
}
