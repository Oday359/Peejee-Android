package com.peejee.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun PeejeeCallScreen(
    callId: String,
    isCaller: Boolean,
    callType: PeejeeCallType,
    onEndCall: () -> Unit,
    onCallConnected: () -> Unit = {}
) {

    val context = LocalContext.current

    val audioManager =
        remember {
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager
        }

    var microphoneEnabled by remember {
        mutableStateOf(true)
    }

    var speakerEnabled by remember {
        mutableStateOf(true)
    }

    var cameraEnabled by remember {
        mutableStateOf(
            callType ==
                PeejeeCallType.VIDEO
        )
    }

    var callState by remember {
        mutableStateOf(
            PeejeeCallState.CONNECTING
        )
    }

    var localVideoTrack by remember {
        mutableStateOf<VideoTrack?>(null)
    }

    var remoteVideoTrack by remember {
        mutableStateOf<VideoTrack?>(null)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var hasPermissions by remember {

        mutableStateOf(

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

                &&

                (
                    callType !=
                        PeejeeCallType.VIDEO

                        ||

                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) ==
                            PackageManager.PERMISSION_GRANTED
                )
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {

            hasPermissions =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) ==
                    PackageManager.PERMISSION_GRANTED

                    &&

                    (
                        callType !=
                            PeejeeCallType.VIDEO

                            ||

                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) ==
                                PackageManager.PERMISSION_GRANTED
                    )
        }

    val signaling =
        remember {
            PeejeeCallSignaling()
        }

    val controller =
        remember(
            callId,
            isCaller,
            callType
        ) {

            PeejeeWebRtcCallController(

                context = context,

                callId = callId,

                isCaller = isCaller,

                callType = callType,

                signaling = signaling,

                onStateChanged = { state ->

                    callState =
                        state

                    if (
                        state ==
                            PeejeeCallState.CONNECTED
                    ) {

                        onCallConnected()
                    }
                },

                onError = { error ->

                    errorMessage =
                        error
                },

                onRemoteVideoTrack = { track ->

                    remoteVideoTrack =
                        track
                },

                onLocalVideoTrack = { track ->

                    localVideoTrack =
                        track
                }
            )
        }

    LaunchedEffect(hasPermissions) {

        if (hasPermissions) {

            /*
             * Start with the loudspeaker enabled.
             */
            try {

                audioManager.isSpeakerphoneOn =
                    true

            } catch (_: Exception) {
            }

            controller.start()
        }
    }

    DisposableEffect(controller) {

        onDispose {

            try {

                audioManager.isSpeakerphoneOn =
                    false

            } catch (_: Exception) {
            }

            controller.release()
        }
    }

    if (!hasPermissions) {

        LaunchedEffect(Unit) {

            val permissions =

                if (
                    callType ==
                        PeejeeCallType.VIDEO
                ) {

                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    )

                } else {

                    arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    )
                }

            permissionLauncher.launch(
                permissions
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    "Peejee needs permission to use your " +
                        if (
                            callType ==
                                PeejeeCallType.VIDEO
                        ) {
                            "microphone and camera"
                        } else {
                            "microphone"
                        },
                    color = Color.White
                )

                Button(
                    onClick = {

                        val permissions =

                            if (
                                callType ==
                                    PeejeeCallType.VIDEO
                            ) {

                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA
                                )

                            } else {

                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }

                        permissionLauncher.launch(
                            permissions
                        )
                    },

                    modifier =
                        Modifier.padding(
                            top = 16.dp
                        )
                ) {

                    Text("Allow")
                }
            }
        }

        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black
                )
    ) {

        if (
            callType ==
                PeejeeCallType.VIDEO
        ) {

            if (
                remoteVideoTrack != null
            ) {

                PeejeeRemoteVideoView(
                    videoTrack =
                        remoteVideoTrack!!,

                    modifier =
                        Modifier.fillMaxSize()
                )

            } else {

                Column(
                    modifier =
                        Modifier.fillMaxSize(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        "Waiting for video…",
                        color = Color.White
                    )

                    Text(
                        when (
                            callState
                        ) {

                            PeejeeCallState.CONNECTING ->
                                "Connecting…"

                            PeejeeCallState.CONNECTED ->
                                "Connected"

                            else ->
                                callState.name
                        },

                        color = Color.White,

                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            )
                    )
                }
            }

            if (
                localVideoTrack != null
            ) {

                PeejeeLocalVideoView(
                    videoTrack =
                        localVideoTrack!!,

                    modifier =
                        Modifier
                            .size(
                                width = 120.dp,
                                height = 180.dp
                            )
                            .align(
                                Alignment.TopEnd
                            )
                            .padding(12.dp)
                )
            }

        } else {

            Column(
                modifier =
                    Modifier.fillMaxSize(),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    "📞",

                    color = Color.White,

                    style =
                        MaterialTheme
                            .typography
                            .displayLarge
                )

                Text(
                    when (
                        callState
                    ) {

                        PeejeeCallState.CONNECTING ->
                            "Calling…"

                        PeejeeCallState.CONNECTED ->
                            "Connected"

                        else ->
                            callState.name
                    },

                    color = Color.White
                )
            }
        }

        if (
            errorMessage.isNotBlank()
        ) {

            Text(
                errorMessage,

                color = Color.White,

                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(16.dp)
            )
        }

        /*
         * CALL CONTROLS
         *
         * navigationBarsPadding()
         * keeps the buttons above the
         * Android navigation buttons.
         */
        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(
                        Alignment.BottomCenter
                    )
                    .navigationBarsPadding()
                    .padding(
                        bottom = 14.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceEvenly,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * MICROPHONE
             */
            IconButton(
                onClick = {

                    microphoneEnabled =
                        !microphoneEnabled

                    controller
                        .setMicrophoneEnabled(
                            microphoneEnabled
                        )
                }
            ) {

                Text(
                    if (
                        microphoneEnabled
                    ) {
                        "🎤"
                    } else {
                        "🔇"
                    },

                    color =
                        Color.White
                )
            }

            /*
             * LOUDSPEAKER
             */
            IconButton(
                onClick = {

                    speakerEnabled =
                        !speakerEnabled

                    try {

                        audioManager.isSpeakerphoneOn =
                            speakerEnabled

                    } catch (_: Exception) {
                    }
                }
            ) {

                Text(
                    if (
                        speakerEnabled
                    ) {
                        "🔊"
                    } else {
                        "🔈"
                    },

                    color =
                        Color.White
                )
            }

            /*
             * VIDEO CAMERA
             */
            if (
                callType ==
                    PeejeeCallType.VIDEO
            ) {

                IconButton(
                    onClick = {

                        cameraEnabled =
                            !cameraEnabled

                        controller
                            .setCameraEnabled(
                                cameraEnabled
                            )
                    }
                ) {

                    Text(
                        if (
                            cameraEnabled
                        ) {
                            "📷"
                        } else {
                            "🚫"
                        },

                        color =
                            Color.White
                    )
                }

                /*
                 * SWITCH CAMERA
                 */
                IconButton(
                    onClick = {

                        controller
                            .switchCamera()
                    }
                ) {

                    Text(
                        "🔄",

                        color =
                            Color.White
                    )
                }
            }

            /*
             * END CALL
             */
            Button(
                onClick = {

                    try {

                        audioManager.isSpeakerphoneOn =
                            false

                    } catch (_: Exception) {
                    }

                    controller.end()

                    onEndCall()
                }
            ) {

                Text("🔴 End")
            }
        }
    }
}

@Composable
private fun PeejeeRemoteVideoView(
    videoTrack: VideoTrack,
    modifier: Modifier
) {

    val context =
        LocalContext.current

    val renderer =
        remember {
            SurfaceViewRenderer(
                context
            )
        }

    AndroidView(

        factory = {

            renderer.apply {

                setEnableHardwareScaler(
                    true
                )

                setScalingType(
                    RendererCommon.ScalingType.SCALE_ASPECT_FILL
                )

                setMirror(
                    false
                )

                init(
                    null,
                    null
                )

                videoTrack.addSink(
                    this
                )
            }
        },

        modifier = modifier,

        update = {

            videoTrack.addSink(
                it
            )
        }
    )

    DisposableEffect(videoTrack) {

        onDispose {

            try {

                videoTrack.removeSink(
                    renderer
                )

            } catch (_: Exception) {
            }

            try {

                renderer.release()

            } catch (_: Exception) {
            }
        }
    }
}

@Composable
private fun PeejeeLocalVideoView(
    videoTrack: VideoTrack,
    modifier: Modifier
) {

    val context =
        LocalContext.current

    val renderer =
        remember {
            SurfaceViewRenderer(
                context
            )
        }

    AndroidView(

        factory = {

            renderer.apply {

                setEnableHardwareScaler(
                    true
                )

                setScalingType(
                    RendererCommon.ScalingType.SCALE_ASPECT_FILL
                )

                setMirror(
                    true
                )

                init(
                    null,
                    null
                )

                videoTrack.addSink(
                    this
                )
            }
        },

        modifier = modifier,

        update = {

            videoTrack.addSink(
                it
            )
        }
    )

    DisposableEffect(videoTrack) {

        onDispose {

            try {

                videoTrack.removeSink(
                    renderer
                )

            } catch (_: Exception) {
            }

            try {

                renderer.release()

            } catch (_: Exception) {
            }
        }
    }
}
