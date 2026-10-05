package com.peejee.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PeejeeFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "peejee_messages"
        private const val CHANNEL_NAME = "Peejee Messages"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val title =
            remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: "Peejee"

        val body =
            remoteMessage.data["body"]
                ?: remoteMessage.notification?.body
                ?: "You have a new message."

        val senderId =
            remoteMessage.data["senderId"] ?: ""

        val messageId =
            remoteMessage.data["messageId"] ?: ""

        showPeejeeNotification(
            title = title,
            body = body,
            senderId = senderId,
            messageId = messageId
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val currentUserId =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid

        if (!currentUserId.isNullOrBlank()) {

            FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("fcmTokens")
                .document(token)
                .set(
                    hashMapOf(
                        "token" to token,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
        }
    }

    private fun showPeejeeNotification(
        title: String,
        body: String,
        senderId: String,
        messageId: String
    ) {

        val notificationManager =
            getSystemService(
                NotificationManager::class.java
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Notifications for Peejee private messages"

                    enableVibration(true)

                    setShowBadge(true)
                }

            notificationManager
                .createNotificationChannel(channel)
        }

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra(
                    "openChatUserId",
                    senderId
                )

                putExtra(
                    "messageId",
                    messageId
                )
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                messageId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.peejee_app_icon_512
                )
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .setVibrate(
                    longArrayOf(
                        0,
                        200,
                        120,
                        200
                    )
                )
                .build()

        notificationManager.notify(
            if (messageId.isNotBlank()) {
                messageId.hashCode()
            } else {
                System.currentTimeMillis()
                    .toInt()
            },
            notification
        )
    }
}
