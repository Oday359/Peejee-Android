package com.peejee.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun SettingsPage(
    email: String,
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUserId = auth.currentUser?.uid

    var privateAccount by remember {
        mutableStateOf(false)
    }

    var messagePermission by remember {
        mutableStateOf("everyone")
    }

    var followPermission by remember {
        mutableStateOf("everyone")
    }

    var commentPermission by remember {
        mutableStateOf("everyone")
    }

    var loadingSettings by remember {
        mutableStateOf(true)
    }

    var savingSettings by remember {
        mutableStateOf(false)
    }

    var settingsMessage by remember {
        mutableStateOf("")
    }

    var blockedUsers by remember {
        mutableStateOf<List<Pair<String, String>>>(emptyList())
    }

    var loadingBlockedUsers by remember {
        mutableStateOf(true)
    }

    var blockedUsersError by remember {
        mutableStateOf("")
    }

    var openSelection by remember {
        mutableStateOf("")
    }

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            loadingSettings = false
            loadingBlockedUsers = false
            settingsMessage = "Please log in again."
            blockedUsersError = "Please log in again."
            onDispose { }
        } else {
            val userRef = firestore
                .collection("users")
                .document(currentUserId)

            val userRegistration = userRef
                .addSnapshotListener { document, error ->

                    if (error != null) {
                        loadingSettings = false
                        settingsMessage =
                            error.message
                                ?: "Could not load privacy settings."
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        privateAccount =
                            document.getBoolean("privateAccount")
                                ?: false

                        messagePermission =
                            document.getString("messagePermission")
                                ?: "everyone"

                        followPermission =
                            document.getString("followPermission")
                                ?: "everyone"

                        commentPermission =
                            document.getString("commentPermission")
                                ?: "everyone"
                    }

                    loadingSettings = false
                }

            val blockedRegistration = userRef
                .collection("blockedUsers")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        loadingBlockedUsers = false
                        blockedUsersError =
                            error.message
                                ?: "Could not load blocked users."
                        return@addSnapshotListener
                    }

                    blockedUsers = snapshot
                        ?.documents
                        ?.map { document ->
                            val name =
                                document.getString("name")
                                    ?.trim()
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "Peejee User"

                            Pair(document.id, name)
                        }
                        ?.sortedBy { it.second.lowercase() }
                        ?: emptyList()

                    loadingBlockedUsers = false
                    blockedUsersError = ""
                }

            onDispose {
                userRegistration.remove()
                blockedRegistration.remove()
            }
        }
    }

    fun savePrivacySettings() {
        if (currentUserId == null) {
            settingsMessage = "Please log in again."
            return
        }

        savingSettings = true
        settingsMessage = ""

        val settings = hashMapOf<String, Any>(
            "privateAccount" to privateAccount,
            "messagePermission" to messagePermission,
            "followPermission" to followPermission,
            "commentPermission" to commentPermission
        )

        firestore
            .collection("users")
            .document(currentUserId)
            .set(settings, SetOptions.merge())
            .addOnSuccessListener {
                savingSettings = false
                settingsMessage = "Privacy settings saved."
            }
            .addOnFailureListener { exception ->
                savingSettings = false
                settingsMessage =
                    exception.message
                        ?: "Could not save privacy settings."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {

        TextButton(
            onClick = onBack
        ) {
            Text("← Back")
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Account Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(Modifier.height(20.dp))

        Text(
            "Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Email",
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            email,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(Modifier.height(20.dp))

        Text(
            "🔒 Privacy & Account Controls",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (loadingSettings) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

        } else {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "🔒 Private account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            if (privateAccount) {
                                "Your account is private."
                            } else {
                                "Your account is public."
                            },
                            fontSize = 13.sp
                        )
                    }

                    TextButton(
                        onClick = {
                            privateAccount = !privateAccount
                            savePrivacySettings()
                        },
                        enabled = !savingSettings
                    ) {
                        Text(
                            if (privateAccount) {
                                "ON"
                            } else {
                                "OFF"
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            PrivacySettingCard(
                title = "💬 Who can message me",
                value = messagePermissionLabel(messagePermission),
                onClick = {
                    openSelection = "message"
                }
            )

            Spacer(Modifier.height(10.dp))

            PrivacySettingCard(
                title = "👥 Who can follow me",
                value = followPermissionLabel(followPermission),
                onClick = {
                    openSelection = "follow"
                }
            )

            Spacer(Modifier.height(10.dp))

            PrivacySettingCard(
                title = "💬 Who can comment",
                value = commentPermissionLabel(commentPermission),
                onClick = {
                    openSelection = "comment"
                }
            )

            if (settingsMessage.isNotBlank()) {

                Spacer(Modifier.height(12.dp))

                Text(
                    settingsMessage,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    savePrivacySettings()
                },
                enabled = !savingSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (savingSettings) {
                        "Saving..."
                    } else {
                        "Save Privacy Settings"
                    }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        HorizontalDivider()

        Spacer(Modifier.height(20.dp))

        Text(
            "🚫 Blocked Users",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Manage people you have blocked on Peejee."
        )

        Spacer(Modifier.height(14.dp))

        if (loadingBlockedUsers) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (blockedUsersError.isNotBlank()) {

            Text(blockedUsersError)

        } else if (blockedUsers.isEmpty()) {

            Text("You have not blocked anyone.")

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                items(
                    items = blockedUsers,
                    key = { it.first }
                ) { blockedUser ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    blockedUser.second,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Blocked",
                                    fontSize = 13.sp
                                )
                            }

                            TextButton(
                                onClick = {

                                    if (currentUserId != null) {
                                        firestore
                                            .collection("users")
                                            .document(currentUserId)
                                            .collection("blockedUsers")
                                            .document(blockedUser.first)
                                            .delete()
                                    }
                                }
                            ) {
                                Text("🔓 Unblock")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }

    if (openSelection.isNotBlank()) {

        when (openSelection) {

            "message" -> {
                PrivacyChoiceDialog(
                    title = "Who can message me?",
                    selected = messagePermission,
                    options = listOf(
                        "everyone" to "Everyone",
                        "followers" to "Followers",
                        "nobody" to "Nobody"
                    ),
                    onSelected = {
                        messagePermission = it
                        openSelection = ""
                        savePrivacySettings()
                    },
                    onDismiss = {
                        openSelection = ""
                    }
                )
            }

            "follow" -> {
                PrivacyChoiceDialog(
                    title = "Who can follow me?",
                    selected = followPermission,
                    options = listOf(
                        "everyone" to "Everyone",
                        "approval" to "Approval required"
                    ),
                    onSelected = {
                        followPermission = it
                        openSelection = ""
                        savePrivacySettings()
                    },
                    onDismiss = {
                        openSelection = ""
                    }
                )
            }

            "comment" -> {
                PrivacyChoiceDialog(
                    title = "Who can comment?",
                    selected = commentPermission,
                    options = listOf(
                        "everyone" to "Everyone",
                        "followers" to "Followers",
                        "nobody" to "Nobody"
                    ),
                    onSelected = {
                        commentPermission = it
                        openSelection = ""
                        savePrivacySettings()
                    },
                    onDismiss = {
                        openSelection = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun PrivacySettingCard(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    value,
                    fontSize = 13.sp
                )
            }

            TextButton(
                onClick = onClick
            ) {
                Text("Change")
            }
        }
    }
}

@Composable
private fun PrivacyChoiceDialog(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column {
                options.forEach { option ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = selected == option.first,
                            onClick = {
                                onSelected(option.first)
                            }
                        )

                        Spacer(Modifier.height(1.dp))

                        Text(
                            option.second,
                            fontSize = 16.sp
                        )
                    }
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

private fun messagePermissionLabel(
    value: String
): String {
    return when (value) {
        "followers" -> "Followers"
        "nobody" -> "Nobody"
        else -> "Everyone"
    }
}

private fun followPermissionLabel(
    value: String
): String {
    return when (value) {
        "approval" -> "Approval required"
        else -> "Everyone"
    }
}

private fun commentPermissionLabel(
    value: String
): String {
    return when (value) {
        "followers" -> "Followers"
        "nobody" -> "Nobody"
        else -> "Everyone"
    }
}
