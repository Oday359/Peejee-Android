package com.peejee.app

import android.widget.Toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun PeejeeSavePostAction(
    post: PeejeePost,
    currentUserId: String
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val context = LocalContext.current

    var isSaved by remember(post.id, currentUserId) {
        mutableStateOf(false)
    }

    var isSaving by remember(post.id, currentUserId) {
        mutableStateOf(false)
    }

    var saveStatusLoaded by remember(post.id, currentUserId) {
        mutableStateOf(false)
    }

    DisposableEffect(post.id, currentUserId) {

        if (currentUserId.isBlank() || post.id.isBlank()) {
            isSaved = false
            saveStatusLoaded = false

            onDispose { }
        } else {
            val savedPostReference = firestore
                .collection("users")
                .document(currentUserId)
                .collection("savedPosts")
                .document(post.id)

            val registration = savedPostReference
                .addSnapshotListener { snapshot, error ->

                    if (error == null && snapshot != null) {
                        isSaved = snapshot.exists()
                        saveStatusLoaded = true
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    ActionCircle(
        icon = if (isSaved) "🔖" else "🏷️",
        label = if (isSaved) "Saved" else "Save",
        onClick = {

            if (currentUserId.isBlank()) {
                Toast.makeText(
                    context,
                    "Please log in again.",
                    Toast.LENGTH_SHORT
                ).show()

                return@ActionCircle
            }

            if (post.id.isBlank()) {
                Toast.makeText(
                    context,
                    "This post cannot be saved.",
                    Toast.LENGTH_SHORT
                ).show()

                return@ActionCircle
            }

            if (!saveStatusLoaded || isSaving) {
                return@ActionCircle
            }

            isSaving = true

            val savedPostReference = firestore
                .collection("users")
                .document(currentUserId)
                .collection("savedPosts")
                .document(post.id)

            if (isSaved) {

                savedPostReference
                    .delete()
                    .addOnSuccessListener {

                        isSaving = false

                        Toast.makeText(
                            context,
                            "Post removed from saved posts.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {

                        isSaving = false

                        Toast.makeText(
                            context,
                            "Could not remove saved post.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            } else {

                val savedPost = hashMapOf<String, Any>(
                    "postId" to post.id,
                    "userId" to post.userId,
                    "userName" to post.userName,
                    "text" to post.text,
                    "mediaUrl" to post.mediaUrl,
                    "mediaType" to post.mediaType,
                    "savedAt" to System.currentTimeMillis()
                )

                savedPostReference
                    .set(
                        savedPost,
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {

                        isSaving = false

                        Toast.makeText(
                            context,
                            "Post saved.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {

                        isSaving = false

                        Toast.makeText(
                            context,
                            "Could not save post.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    )
}
