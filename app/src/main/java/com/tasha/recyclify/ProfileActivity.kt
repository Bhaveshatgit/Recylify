package com.tasha.recyclify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tasha.recyclify.ui.theme.RecyclifyTheme
import kotlinx.coroutines.tasks.await
import java.util.*

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ProfileScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    val uid = auth.currentUser?.uid ?: return

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var orgName by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for picking image
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }

    // Load user data
    LaunchedEffect(Unit) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            name = doc.getString("firstName") ?: ""
            email = doc.getString("email") ?: ""
            mobile = doc.getString("mobile") ?: ""
            orgName = doc.getString("orgName") ?: ""
            profileImageUrl = doc.getString("profileImageUrl")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Profile") }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        imageUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        profileImageUrl != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUrl),
                                contentDescription = "Profile Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to add photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Editable fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    label = { Text("Organization") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        saveProfile(
                            uid,
                            name,
                            email,
                            mobile,
                            orgName,
                            imageUri,
                            firestore,
                            storage
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(text = "Save Changes")
                }
            }
        }
    }
}

fun saveProfile(
    uid: String,
    name: String,
    email: String,
    mobile: String,
    orgName: String,
    imageUri: Uri?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage
) {
    val userDoc = firestore.collection("users").document(uid)

    if (imageUri != null) {
        val ref = storage.reference.child("profileImages/$uid-${UUID.randomUUID()}.jpg")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val data = mapOf(
                        "firstName" to name,
                        "email" to email,
                        "mobile" to mobile,
                        "orgName" to orgName,
                        "profileImageUrl" to downloadUrl.toString()
                    )
                    userDoc.set(data)
                }
            }
    } else {
        val data = mapOf(
            "firstName" to name,
            "email" to email,
            "mobile" to mobile,
            "orgName" to orgName
        )
        userDoc.set(data)
    }
}



