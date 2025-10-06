package com.tasha.recyclify

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tasha.recyclify.ui.theme.RecyclifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProfileScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance("gs://your-project-id.appspot.com") // ✅ Replace with your actual Firebase bucket

    val uid = auth.currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var orgName by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }

    val sharedPrefs = context.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)

    // Load cached image
    LaunchedEffect(Unit) {
        val cachedUrl = sharedPrefs.getString("profileImageUrl_$uid", null)
        if (cachedUrl != null) profileImageUrl = cachedUrl

        try {
            val doc = firestore.collection("users").document(uid).get().await()
            name = doc.getString("firstName") ?: ""
            email = doc.getString("email") ?: ""
            mobile = doc.getString("mobile") ?: ""
            orgName = doc.getString("orgName") ?: ""
            val url = doc.getString("profileImageUrl")
            if (url != null) {
                profileImageUrl = url
                sharedPrefs.edit().putString("profileImageUrl_$uid", url).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { imageUri = it } }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Profile") }) }
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
                        imageUri != null -> Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        profileImageUrl != null -> Image(
                            painter = rememberAsyncImagePainter(profileImageUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        else -> Text("Add Photo", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap to add photo", color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                // Input Fields
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("Organization") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            saveProfile(
                                uid, name, email, mobile, orgName, imageUri,
                                firestore, storage, sharedPrefs, context
                            ) { uploading -> isUploading = uploading }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    enabled = !isUploading
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Save Changes")
                }
            }
        }
    }
}

suspend fun saveProfile(
    uid: String,
    name: String,
    email: String,
    mobile: String,
    orgName: String,
    imageUri: Uri?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    sharedPrefs: android.content.SharedPreferences,
    context: Context,
    onUploadingChange: (Boolean) -> Unit
) {
    try {
        onUploadingChange(true)
        var uploadedUrl: String? = null

        if (imageUri != null) {
            val ref = storage.reference.child("profileImages/$uid-${UUID.randomUUID()}.jpg")
            ref.putFile(imageUri).await()
            uploadedUrl = ref.downloadUrl.await().toString()
        }

        val profileData = mutableMapOf<String, Any>(
            "firstName" to name,
            "email" to email,
            "mobile" to mobile,
            "orgName" to orgName
        )

        uploadedUrl?.let {
            profileData["profileImageUrl"] = it
            sharedPrefs.edit().putString("profileImageUrl_$uid", it).apply()
        }

        firestore.collection("users").document(uid).set(profileData, com.google.firebase.firestore.SetOptions.merge()).await()

        with(Dispatchers.Main) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        with(Dispatchers.Main) {
            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } finally {
        onUploadingChange(false)
    }
}
