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

    // ✅ Cached profile image
    val sharedPrefs = context.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
    val currentUid = auth.currentUser?.uid ?: uid
    var cachedUri by remember { mutableStateOf<Uri?>(null) }

    // Load cached image
    LaunchedEffect(Unit) {
        val uriString = sharedPrefs.getString("cached_profile_uri_$currentUid", null)
        cachedUri = uriString?.toUri()
    }

    // Launcher for ProfileActivity — updates cached image when returning
    val profileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val updatedUri = sharedPrefs.getString("cached_profile_uri_$currentUid", null)
        cachedUri = updatedUri?.toUri()
    }

    // Load user data
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) user = snapshot.toObject(User::class.java)
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = e.message
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable {
                                val intent = Intent(context, ProfileActivity::class.java)
                                profileLauncher.launch(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cachedUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(cachedUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "Default Profile",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            user?.isBuyer == true -> user?.orgName ?: "Organization"
                            user?.isBuyer == false -> "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                                .ifEmpty { "User" }
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
                    onClick = {
                        val intent = Intent(context, ProfileActivity::class.java)
                        profileLauncher.launch(intent)
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Statistics") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, StatisticsActivity::class.java)
                        profileLauncher.launch(intent)
                    }
                )
                NavigationDrawerItem(label = { Text("Wallet") }, selected = false, onClick = {val intent = Intent(context,
                    WalletActivity::class.java)
                    profileLauncher.launch(intent) })
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
                    title = { Text(if (user?.isBuyer == true) "Buyer Dashboard" else "Seller Dashboard") },
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (user?.isBuyer == true) {
                            item {
                                ModuleCard("Buy", Icons.Default.ShoppingCart) {}
                            }
                            item {
                                ModuleCard("Pickup Requests", Icons.Default.LocalShipping) {}
                            }
                        } else {
                            item {
                                ModuleCard("Sell", Icons.Default.Sell) {
                                    context.startActivity(Intent(context, SellActivity::class.java))
                                }
                            }
                            item {
                                ModuleCard("My Bookings", Icons.Default.List) {
                                    context.startActivity(Intent(context, MyBookingsActivity::class.java))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
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
                    accessKey = "f60590a7c97dee646420851fd4054001",
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
