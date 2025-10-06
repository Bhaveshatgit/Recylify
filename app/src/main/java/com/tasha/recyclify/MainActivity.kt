package com.tasha.recyclify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        Log.d("UID received", "onCreate: $uidFromIntent")
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
    var isBuyerFlag by remember { mutableStateOf<Boolean?>(null) } // explicit flag from Firestore


    // Fetch user details
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { snapshot ->
                    // 1) Raw map & types for debugging
                    val raw = snapshot.data ?: emptyMap<String, Any>()
                    Log.d("FirestoreRawData", raw.toString())

                    // 2) raw field value + its Java type
                    val rawField = snapshot.get("isBuyer")
                    Log.d("FirestoreRawField", "isBuyer raw: $rawField (${rawField?.let { it::class.java.name } ?: "null"})")

                    // 3) Firestore helper to read boolean safely
                    val boolVal: Boolean? = snapshot.getBoolean("isBuyer")
                    Log.d("FirestoreGetBoolean", "getBoolean -> $boolVal")

                    // 4) Try mapping to User (still useful)
                    val mapped = snapshot.toObject(User::class.java)
                    Log.d("MappedUser", "mapped -> $mapped")

                    // 5) Decide final flag
                    // Prefer explicit boolean read; fallback to mapped value if needed; final fallback = false
                    isBuyerFlag = boolVal ?: mapped?.isBuyer ?: false

                    // 6) Store mapped user as well (for other fields)
                    user = mapped

                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Failed to load user", e)
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
                            user?.isBuyer == true -> (user?.orgName ?: "").trim()
                            user?.isBuyer == false -> "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                            else -> "User"
                        },
                        style = MaterialTheme.typography.titleMedium
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
                    title = { Text("Dashboard") },
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
                            item {
                                ModuleCard(
                                    title = "Sell",
                                    icon = Icons.Default.Favorite,
                                    onClick = {
                                        context.startActivity(Intent(context, SellActivity::class.java))
                                    }
                                )
                            }
                            item {
                                ModuleCard(
                                    title = "My bookings",
                                    icon = Icons.Default.Refresh,
                                    onClick = {
                                        context.startActivity(Intent(context, MyBookingsActivity::class.java))
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
                            // Buyer Modules
                            item {
                                ModuleCard(
                                    title = "Buy",
                                    icon = Icons.Default.Refresh,
                                    onClick = { /* TODO: navigate to pickup requests */ }
                                )
                            }
                            item {
                                ModuleCard(
                                    title = "Pickup Requests",
                                    icon = Icons.Default.Refresh,
                                    onClick = { /* TODO: navigate to pickup requests */ }
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

@Composable
fun ModuleCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title)
        }
    }
}

// ------------------ NEWS FEED ------------------
@Composable
fun NewsFeed() {
    var articles by remember { mutableStateOf(listOf<MediaStackArticle>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.service.getNews(
                    accessKey = "a96c76eb4b8fc1535851278dec7a54a0",
                    keywords = "waste"
                )
                articles = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(articles) { news ->
            NewsCard(title = news.title, desc = news.description)
        }
    }
}

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

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    RecyclifyTheme {
        //HomeScreen(uid )
    }
}
