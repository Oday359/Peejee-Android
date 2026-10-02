package com.peejee.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun PostMenu(
    post: PeejeePost,
    onDismiss: () -> Unit
) {

    var showReportDialog by remember {
        mutableStateOf(false)
    }

    var showBlockDialog by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId =
        auth.currentUser?.uid ?: ""

    val context =
        androidx.compose.ui.platform.LocalContext.current

    if (showReportDialog) {

        AlertDialog(

            onDismissRequest = {
                showReportDialog = false
            },

            title = {
                Text("Report Post")
            },

            text = {
                Text(
                    "Are you sure you want to report this post?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        if (currentUserId.isBlank()) {

                            message =
                                "Please log in again."

                            showReportDialog = false

                        } else {

                            val reportData =
                                hashMapOf<String, Any>(
                                    "postId" to post.id,
                                    "reportedUserId" to post.userId,
                                    "reportedUserName" to post.userName,
                                    "reportedBy" to currentUserId,
                                    "timestamp" to
                                        System.currentTimeMillis(),
                                    "reason" to "Post reported"
                                )

                            firestore
                                .collection("reports")
                                .add(reportData)
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        context,
                                        "Post reported.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showReportDialog = false
                                    onDismiss()
                                }
                                .addOnFailureListener {

                                    Toast.makeText(
                                        context,
                                        "Could not report post.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) {
                    Text("Report")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showReportDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )

        return
    }

    if (showBlockDialog) {

        AlertDialog(

            onDismissRequest = {
                showBlockDialog = false
            },

            title = {
                Text("Block User")
            },

            text = {
                Text(
                    "Are you sure you want to block ${post.userName}?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        if (currentUserId.isBlank()) {

                            Toast.makeText(
                                context,
                                "Please log in again.",
                                Toast.LENGTH_SHORT
                            ).show()

                            showBlockDialog = false

                        } else {

                            val blockedData =
                                hashMapOf<String, Any>(
                                    "uid" to post.userId,
                                    "name" to post.userName,
                                    "blockedAt" to
                                        System.currentTimeMillis()
                                )

                            firestore
                                .collection("users")
                                .document(currentUserId)
                                .collection("blockedUsers")
                                .document(post.userId)
                                .set(
                                    blockedData,
                                    SetOptions.merge()
                                )
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        context,
                                        "${post.userName} blocked.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showBlockDialog = false
                                    onDismiss()
                                }
                                .addOnFailureListener {

                                    Toast.makeText(
                                        context,
                                        "Could not block user.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) {
                    Text("Block")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showBlockDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )

        return
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Post Options")
        },

        text = {

            androidx.compose.foundation.layout.Column {

                Button(
                    onClick = {

                        if (currentUserId.isBlank()) {

                            Toast.makeText(
                                context,
                                "Please log in again.",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            val savedPost =
                                hashMapOf<String, Any>(
                                    "postId" to post.id,
                                    "userId" to post.userId,
                                    "userName" to post.userName,
                                    "text" to post.text,
                                    "savedAt" to
                                        System.currentTimeMillis()
                                )

                            firestore
                                .collection("users")
                                .document(currentUserId)
                                .collection("savedPosts")
                                .document(post.id)
                                .set(
                                    savedPost,
                                    SetOptions.merge()
                                )
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        context,
                                        "Post saved.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    onDismiss()
                                }
                                .addOnFailureListener {

                                    Toast.makeText(
                                        context,
                                        "Could not save post.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) {
                    Text("🔖 Save post")
                }

                Button(
                    onClick = {

                        val clipboard =
                            context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        val postLink =
                            "https://peejee.app/post/${post.id}"

                        val clip =
                            ClipData.newPlainText(
                                "Peejee post link",
                                postLink
                            )

                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(
                            context,
                            "Post link copied.",
                            Toast.LENGTH_SHORT
                        ).show()

                        onDismiss()
                    }
                ) {
                    Text("🔗 Copy link")
                }

                Button(
                    onClick = {

                        if (currentUserId.isNotBlank()) {

                            val data =
                                hashMapOf<String, Any>(
                                    "postId" to post.id,
                                    "userId" to post.userId,
                                    "markedAt" to
                                        System.currentTimeMillis()
                                )

                            firestore
                                .collection("users")
                                .document(currentUserId)
                                .collection("notInterested")
                                .document(post.id)
                                .set(data)

                            Toast.makeText(
                                context,
                                "You won't see this post again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        onDismiss()
                    }
                ) {
                    Text("🚫 Not interested")
                }

                OutlinedButton(
                    onClick = {
                        showReportDialog = true
                    }
                ) {
                    Text("⚠️ Report post")
                }

                OutlinedButton(
                    onClick = {
                        showBlockDialog = true
                    }
                ) {
                    Text("🚫 Block user")
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}
