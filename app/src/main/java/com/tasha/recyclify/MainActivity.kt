package com.tasha.recyclify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.data.model.User
import com.tasha.recyclify.ui.sell.SellActivity
import com.tasha.recyclify.ui.theme.RecyclifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ------------------ DATA MODELS ------------------
data class MediaStackResponse(val data: List<MediaStackArticle>)
data class MediaStackArticle(
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String?
)

// ------------------ RETROFIT API ------------------
interface MediaStackApi {
    @GET("v1/news")
    suspend fun getNews(
        @Query("access_key") accessKey: String,
        @Query("keywords") keywords: String,
        @Query("languages") languages: String = "en",
        @Query("limit") limit: Int = 10
    ): MediaStackResponse
}

object RetrofitClient {
    val service: MediaStackApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://api.mediastack.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MediaStackApi::class.java)
    }
}

// ------------------ MAIN ACTIVITY ------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uidFromIntent = intent.getStringExtra("uid")
        setContent {
            RecyclifyTheme {
                HomeScreen(uidFromIntent)
            }
        }
    }
}

// ------------------ UI ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uid: String?) {
    val backgroundColor = Color(0xFFA9FC8A)
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Firebase
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch user details
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // Log raw data for debugging
                        Log.d("FirestoreDebug", "Raw data: ${snapshot.data}")

                        // ✅ FIX: Read "buyer" field (not "isBuyer")
                        val isBuyerFromBoolean = snapshot.getBoolean("buyer")
                            ?: snapshot.getBoolean("isBuyer") // fallback
//                        val isBuyerFromString = snapshot.getString("buyer")
//                        val isBuyerFromLong = snapshot.getLong("buyer")

                        Log.d("FirestoreDebug", "buyer as Boolean: $isBuyerFromBoolean")
//                        Log.d("FirestoreDebug", "buyer as String: $isBuyerFromString")
//                        Log.d("FirestoreDebug", "buyer as Long: $isBuyerFromLong")

                        // Map to User object
                        val mappedUser = snapshot.toObject(User::class.java)

                        Log.d("FirestoreDebug", "Mapped user: $mappedUser")
                        Log.d("FirestoreDebug", "Mapped isBuyer: ${mappedUser?.isBuyer}")

                        // ✅ Use manual mapping if automatic mapping gives wrong isBuyer
                        user = if (mappedUser != null && mappedUser.isBuyer == isBuyerFromBoolean) {
                            // Mapping worked correctly
                            mappedUser
                        } else {
                            // Manual mapping as fallback
                            Log.w("FirestoreDebug", "Using manual mapping - auto-mapping failed for isBuyer")
                            User(
                                uid = snapshot.getString("uid") ?: "",
                                email = snapshot.getString("email") ?: "",
                                mobile = snapshot.getString("mobile") ?: "",
                                isBuyer = isBuyerFromBoolean ?: false,  // ✅ Use the boolean we read directly
                                orgName = snapshot.getString("orgName"),
                                orgLocation = snapshot.getString("orgLocation"),
                                orgContact = snapshot.getString("orgContact"),
                                firstName = snapshot.getString("firstName"),
                                lastName = snapshot.getString("lastName")
                            )
                        }

                        Log.d("FirestoreDebug", "Final user: $user")
                    } else {
                        Log.e("FirestoreError", "Document does not exist")
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Failed to load user", e)
                    errorMessage = when {
                        e.message?.contains("PERMISSION_DENIED") == true ->
                            "Permission denied. Please check Firestore security rules."
                        else -> "Error: ${e.message}"
                    }
                    isLoading = false
                }
        } else {
            Log.e("FirestoreError", "UID is null")
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Profile header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            user?.isBuyer == true -> user?.orgName ?: "Organization"
                            user?.isBuyer == false -> "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim().ifEmpty { "User" }
                            else -> "Guest"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (user?.isBuyer == true) "Buyer" else "Seller",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Divider()

                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { /* TODO */ }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { /* TODO */ }
                )
                NavigationDrawerItem(
                    label = { Text("Statistics") },
                    selected = false,
                    onClick = { context.startActivity(Intent(context, StatisticsActivity::class.java)) }
                )
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        auth.signOut()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (user?.isBuyer == true) "Buyer Dashboard" else "Seller Dashboard")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )
            },
            containerColor = backgroundColor
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (user == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "Failed to load user data",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        }) {
                            Text("Return to Login")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    // Dashboard modules
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (user?.isBuyer == true) {
                            // Buyer Modules
                            item {
                                ModuleCard(
                                    title = "Pickup Requests",
                                    icon = Icons.Default.LocalShipping,
                                    onClick = { /* TODO: navigate to pickup requests */
                                        context.startActivity(Intent(context,
                                            PickupRequestsActivity::class.java))
                                    }
                                )
                            }
                            item {
                                ModuleCard(
                                    title = "Buy",
                                    icon = Icons.Default.ShoppingCart,
                                    onClick = {
                                    /* TODO: navigate to buy */
                                        context.startActivity(Intent(context,
                                            CompanyManagementActivity::class.java))
                                    }
                                )
                            }
                            item {
                                ModuleCard(
                                    title = "Donations",
                                    icon = Icons.Default.Favorite,
                                    onClick = { /* TODO: navigate to donations */ }
                                )
                            }
                        } else {
                            // Seller Modules
                            item {
                                ModuleCard(
                                    title = "Sell",
                                    icon = Icons.Default.Sell,
                                    onClick = {
                                        context.startActivity(Intent(context, SellActivity::class.java))
                                    }
                                )
                            }

                            item {
                                ModuleCard(
                                    title = "My Bookings",
                                    icon = Icons.Default.List,
                                    onClick = {
                                        context.startActivity(Intent(context, MyBookingsActivity::class.java))
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // News Feed
                    Text("Recycling News", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    NewsFeed()
                }
            }
        }
    }
}

// ------------------ MODULE CARD ------------------
@Composable
fun ModuleCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title)
        }
    }
}

// ------------------ NEWS FEED ------------------
@Composable
fun NewsFeed() {
    var articles by remember { mutableStateOf(listOf<MediaStackArticle>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.service.getNews(
                    accessKey = "ttgtgtgt6thh89",
                    keywords = "waste"
                )
                if (response.data.isNotEmpty()) {
                    articles = response.data
                } else {
                    error = "No news available."
                }
            } catch (e: Exception) {
                Log.e("NewsFeed", "Error fetching news", e)
                error = e.message
            }
        }
    }

    when {
        error != null -> Text(
            text = "Error loading news: $error",
            color = Color.Red,
            modifier = Modifier.padding(8.dp)
        )
        articles.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        else -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(articles.size) { index ->
                val news = articles[index]
                NewsCard(title = news.title, desc = news.description)
            }
        }
    }
}


// ------------------ NEWS CARD ------------------
@Composable
fun NewsCard(title: String?, desc: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Spacer(modifier = Modifier.height(4.dp))
            desc?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
