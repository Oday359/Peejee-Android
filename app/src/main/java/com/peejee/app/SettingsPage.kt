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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
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
import com.google.firebase.firestore.FieldValue
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

    var supportPage by remember {
        mutableStateOf("")
    }

    var reportText by remember {
        mutableStateOf("")
    }

    var reportSending by remember {
        mutableStateOf(false)
    }

    var reportMessage by remember {
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

    fun submitReport() {
        if (currentUserId == null) {
            reportMessage = "Please log in again."
            return
        }

        if (reportText.trim().isBlank()) {
            reportMessage = "Please describe the problem first."
            return
        }

        reportSending = true
        reportMessage = ""

        val report = hashMapOf<String, Any>(
            "userId" to currentUserId,
            "email" to email,
            "description" to reportText.trim(),
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore
            .collection("reports")
            .add(report)
            .addOnSuccessListener {
                reportSending = false
                reportText = ""
                reportMessage = "Your report has been submitted."
            }
            .addOnFailureListener { exception ->
                reportSending = false
                reportMessage =
                    exception.message
                        ?: "Could not submit your report."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
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

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                blockedUsers.forEach { blockedUser ->

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

        HorizontalDivider()

        Spacer(Modifier.height(20.dp))

        Text(
            "🆘 Support & Information",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        SupportSettingCard(
            icon = "🆘",
            title = "Help Center",
            description = "Find answers to common questions.",
            onClick = {
                supportPage = "help"
            }
        )

        Spacer(Modifier.height(10.dp))

        SupportSettingCard(
            icon = "🚨",
            title = "Report a problem",
            description = "Tell us about a problem with Peejee.",
            onClick = {
                supportPage = "report"
                reportMessage = ""
            }
        )

        Spacer(Modifier.height(10.dp))

        SupportSettingCard(
            icon = "📖",
            title = "Community guidelines",
            description = "Learn about the rules for using Peejee.",
            onClick = {
                supportPage = "guidelines"
            }
        )

        Spacer(Modifier.height(10.dp))

        SupportSettingCard(
            icon = "📜",
            title = "Terms",
            description = "Read the terms that apply to Peejee.",
            onClick = {
                supportPage = "terms"
            }
        )

        Spacer(Modifier.height(10.dp))

        SupportSettingCard(
            icon = "🔐",
            title = "Privacy policy",
            description = "Learn how Peejee handles account information.",
            onClick = {
                supportPage = "privacy"
            }
        )

        Spacer(Modifier.height(10.dp))

        SupportSettingCard(
            icon = "ℹ️",
            title = "About Peejee",
            description = "Information about the Peejee app.",
            onClick = {
                supportPage = "about"
            }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }

        Spacer(Modifier.height(20.dp))
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

    if (supportPage.isNotBlank()) {

        when (supportPage) {

            "help" -> {
                HelpCenterDialog(
                    onDismiss = {
                        supportPage = ""
                    }
                )
            }

            "report" -> {
                ReportProblemDialog(
                    reportText = reportText,
                    onReportTextChange = {
                        reportText = it
                        reportMessage = ""
                    },
                    reportMessage = reportMessage,
                    sending = reportSending,
                    onSubmit = {
                        submitReport()
                    },
                    onDismiss = {
                        supportPage = ""
                    }
                )
            }

            "guidelines" -> {
                InformationDialog(
                    title = "Community Guidelines",
                    content = """
Peejee is designed to be a respectful community.

Please:

• Treat other users with respect.
• Do not use Peejee to threaten, harass, or intimidate others.
• Do not post illegal or harmful content.
• Do not impersonate another person.
• Do not use spam or deceptive activity to abuse the platform.
• Respect other people's privacy.
• Use the reporting tools when you see a serious problem.

These guidelines may be updated as Peejee develops.
                    """.trimIndent(),
                    onDismiss = {
                        supportPage = ""
                    }
                )
            }

            "terms" -> {
                InformationDialog(
                    title = "Peejee Terms",
                    content = """
By using Peejee, you agree to use the service responsibly and follow the rules of the platform.

You are responsible for the content and activity associated with your account.

You should not use Peejee for unlawful activity, abuse, harassment, fraud, spam, or activities that could harm other users or the service.

Peejee may introduce, change, or remove features as the application develops.

If you do not agree with these terms, you should stop using the service.

These terms may be updated as Peejee develops.
                    """.trimIndent(),
                    onDismiss = {
                        supportPage = ""
                    }
                )
            }

            "privacy" -> {
                InformationDialog(
                    title = "Peejee Privacy Policy",
                    content = """
Peejee uses account information needed to provide the application and its features.

Depending on the features you use, information associated with your account may include your email address, profile information, posts, messages, settings, blocked-user information, and reports submitted through the app.

Peejee should only use information for purposes connected with operating, securing, improving, and supporting the service.

Do not share passwords or other sensitive information with other users.

The privacy policy may be expanded and updated as Peejee develops additional features and services.
                    """.trimIndent(),
                    onDismiss = {
                        supportPage = ""
                    }
                )
            }

            "about" -> {
                InformationDialog(
                    title = "About Peejee",
                    content = """
Peejee

Peejee is a social and messaging application designed to help people connect, communicate, share posts, and manage their profiles.

Current features include:

• Profiles
• Posts
• Comments
• Likes
• Following
• Private accounts
• Messaging
• Notifications
• Privacy controls
• Blocked users
• Reporting tools

Peejee is continuously being developed and improved.
                    """.trimIndent(),
                    onDismiss = {
                        supportPage = ""
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
private fun SupportSettingCard(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    icon,
                    fontSize = 24.sp
                )

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        description,
                        fontSize = 13.sp
                    )
                }

                Text(
                    "›",
                    fontSize = 28.sp
                )
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

                        Spacer(Modifier.width(8.dp))

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

@Composable
private fun HelpCenterDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🆘 Help Center")
        },
        text = {
            Column {
                HelpQuestion(
                    question = "How do I change my privacy settings?",
                    answer = "Open Account Settings and use the Privacy & Account Controls section."
                )

                Spacer(Modifier.height(12.dp))

                HelpQuestion(
                    question = "How do I block someone?",
                    answer = "Use the Block User option where available on a user's profile or post."
                )

                Spacer(Modifier.height(12.dp))

                HelpQuestion(
                    question = "How do I unblock someone?",
                    answer = "Open Account Settings, go to Blocked Users, and select Unblock."
                )

                Spacer(Modifier.height(12.dp))

                HelpQuestion(
                    question = "How do I report a problem?",
                    answer = "Open Account Settings, select Report a problem, describe the issue, and submit it."
                )

                Spacer(Modifier.height(12.dp))

                HelpQuestion(
                    question = "What if I cannot access my account?",
                    answer = "Use the available account recovery options and make sure you are using the correct login information."
                )
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

@Composable
private fun HelpQuestion(
    question: String,
    answer: String
) {
    Column {
        Text(
            question,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(3.dp))

        Text(
            answer,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ReportProblemDialog(
    reportText: String,
    onReportTextChange: (String) -> Unit,
    reportMessage: String,
    sending: Boolean,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!sending) {
                onDismiss()
            }
        },
        title = {
            Text("🚨 Report a problem")
        },
        text = {
            Column {

                Text(
                    "Tell us what went wrong or describe a problem you found in Peejee.",
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = reportText,
                    onValueChange = onReportTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Describe the problem")
                    },
                    minLines = 5,
                    maxLines = 8,
                    enabled = !sending
                )

                if (reportMessage.isNotBlank()) {

                    Spacer(Modifier.height(10.dp))

                    Text(
                        reportMessage,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = !sending
            ) {
                Text(
                    if (sending) {
                        "Sending..."
                    } else {
                        "Submit"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !sending
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InformationDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    content,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
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
