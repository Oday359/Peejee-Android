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

DisposableEffect(post.id, currentUserId) {  
    if (currentUserId.isBlank() || post.id.isBlank()) {  
        isSaved = false  
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
                }  
            }  

        onDispose {  
            registration.remove()  
        }  
    }  
}  

ActionCircle(  
    icon = if (isSaved) "🔖" else "🏷️",  
    label = when {  
        isSaving -> "Saving…"  
        isSaved -> "Saved"  
        else -> "Save"  
    },  
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
                "This post cannot be saved because its ID is missing.",  
                Toast.LENGTH_SHORT  
            ).show()  
            return@ActionCircle  
        }  

        if (isSaving) {  
            return@ActionCircle  
        }  

        isSaving = true  

        val savedPostReference = firestore  
            .collection("users")  
            .document(currentUserId)  
            .collection("savedPosts")  
            .document(post.id)  

        // Check Firestore directly when tapped. This avoids silently doing  
        // nothing if the background saved-status listener has not loaded.  
        savedPostReference  
            .get()  
            .addOnSuccessListener { snapshot ->  
                if (snapshot.exists()) {  
                    savedPostReference  
                        .delete()  
                        .addOnSuccessListener {  
                            isSaving = false  
                            isSaved = false  
                            Toast.makeText(  
                                context,  
                                "Post removed from saved posts.",  
                                Toast.LENGTH_SHORT  
                            ).show()  
                        }  
                        .addOnFailureListener { error ->  
                            isSaving = false  
                            Toast.makeText(  
                                context,  
                                "Could not remove post: ${error.localizedMessage ?: "Firebase request failed."}",  
                                Toast.LENGTH_LONG  
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
                        .set(savedPost, SetOptions.merge())  
                        .addOnSuccessListener {  
                            isSaving = false  
                            isSaved = true  
                            Toast.makeText(  
                                context,  
                                "Post saved.",  
                                Toast.LENGTH_SHORT  
                            ).show()  
                        }  
                        .addOnFailureListener { error ->  
                            isSaving = false  
                            Toast.makeText(  
                                context,  
                                "Could not save post: ${error.localizedMessage ?: "Firebase request failed."}",  
                                Toast.LENGTH_LONG  
                            ).show()  
                        }  
                }  
            }  
            .addOnFailureListener { error ->  
                isSaving = false  
                Toast.makeText(  
                    context,  
                    "Could not check saved post: ${error.localizedMessage ?: "Firebase request failed."}",  
                    Toast.LENGTH_LONG  
                ).show()  
            }  
    }  
)

}
