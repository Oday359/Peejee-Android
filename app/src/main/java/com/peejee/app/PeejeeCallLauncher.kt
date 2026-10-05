package com.peejee.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PeejeeCallButtons(
    onStartCall: (PeejeeCallType) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
        horizontalArrangement =
            Arrangement.End
    ) {

        Button(
            onClick = {
                onStartCall(
                    PeejeeCallType.AUDIO
                )
            }
        ) {
            Text("📞")
        }

        Button(
            onClick = {
                onStartCall(
                    PeejeeCallType.VIDEO
                )
            },
            modifier =
                Modifier.padding(
                    start = 6.dp
                )
        ) {
            Text("📹")
        }
    }
}

@Composable
fun PeejeeCallWindow(
    callId: String,
    isCaller: Boolean,
    callType: PeejeeCallType,
    onClose: () -> Unit
) {

    AlertDialog(
        onDismissRequest = {
            onClose()
        },
        confirmButton = {},
        text = {

            PeejeeCallScreen(
                callId = callId,
                isCaller = isCaller,
                callType = callType,
                onEndCall = {
                    onClose()
                }
            )
        }
    )
}
