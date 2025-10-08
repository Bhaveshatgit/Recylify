package com.tasha.recyclify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.data.model.User
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
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Firebase
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var greenCoins by remember { mutableStateOf(0) }

    // Fetch user details
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val isBuyerFromBoolean = snapshot.getBoolean("buyer")
                            ?: snapshot.getBoolean("isBuyer")

                        val mappedUser = snapshot.toObject(User::class.java)

                        user = if (mappedUser != null && mappedUser.isBuyer == isBuyerFromBoolean) {
                            mappedUser
                        } else {
                            User(
                                uid = snapshot.getString("uid") ?: "",
                                email = snapshot.getString("email") ?: "",
                                mobile = snapshot.getString("mobile") ?: "",
                                isBuyer = isBuyerFromBoolean ?: false,
                                orgName = snapshot.getString("orgName"),
                                orgLocation = snapshot.getString("orgLocation"),
                                orgContact = snapshot.getString("orgContact"),
                                firstName = snapshot.getString("firstName"),
                                lastName = snapshot.getString("lastName")
                            )
                        }

                        // Fetch green coins for sellers
                        if (user?.isBuyer == false) {
                            db.collection("wallet").document(uid)
                                .addSnapshotListener { walletSnapshot, _ ->
                                    greenCoins = walletSnapshot?.getLong("coins")?.toInt() ?: 0
                                }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = when {
                        e.message?.contains("PERMISSION_DENIED") == true ->
                            "Permission denied. Please check Firestore security rules."
                        else -> "Error: ${e.message}"
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // Modern Profile Header with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF66BB6A)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Profile Avatar with Border
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (user?.isBuyer == true)
                                        Icons.Default.Business
                                    else
                                        Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when {
                                user?.isBuyer == true -> user?.orgName ?: "Organization"
                                user?.isBuyer == false -> "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim().ifEmpty { "User" }
                                else -> "Guest"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = if (user?.isBuyer == true) "Buyer Account" else "Seller Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                DrawerNavigationItem(
                    icon = Icons.Outlined.Person,
                    label = "Profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to profile
                    }
                )

                DrawerNavigationItem(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    label = "My Wallet",
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, WalletActivity::class.java))
                    }
                )

                // Statistics only for sellers
                if (user?.isBuyer == false) {
                    DrawerNavigationItem(
                        icon = Icons.Outlined.BarChart,
                        label = "Statistics",
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, StatisticsActivity::class.java))
                        }
                    )
                }

                DrawerNavigationItem(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to settings
                    }
                )

                DrawerNavigationItem(
                    icon = Icons.Outlined.Help,
                    label = "Help & Support",
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to help
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                DrawerNavigationItem(
                    icon = Icons.Outlined.Logout,
                    label = "Logout",
                    isDestructive = true,
                    onClick = {
                        auth.signOut()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Dashboard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Welcome back!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (user?.isBuyer == false) {
                            // Coins Badge for Sellers
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = greenCoins.toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF4CAF50),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading dashboard...", color = Color.Gray)
                    }
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
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Failed to load user data",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, LoginActivity::class.java))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Return to Login")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Welcome Card
                    item {
                        WelcomeCard(user = user)
                    }

                    // Quick Actions Section
                    item {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    // Dashboard Modules
                    item {
                        if (user?.isBuyer == true) {
                            // Buyer Modules
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernModuleCard(
                                    title = "Pickup Requests",
                                    subtitle = "Manage incoming requests",
                                    icon = Icons.Default.LocalShipping,
                                    gradient = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A)),
                                    onClick = {
                                        context.startActivity(Intent(context, PickupRequestsActivity::class.java))
                                    }
                                )

                                ModernModuleCard(
                                    title = "Company Management",
                                    subtitle = "Manage your companies",
                                    icon = Icons.Default.Business,
                                    gradient = listOf(Color(0xFF2196F3), Color(0xFF42A5F5)),
                                    onClick = {
                                        context.startActivity(Intent(context, CompanyManagementActivity::class.java))
                                    }
                                )
                            }
                        } else {
                            // Seller Modules
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernModuleCard(
                                    title = "Schedule Pickup",
                                    subtitle = "Book a waste pickup",
                                    icon = Icons.Default.Recycling,
                                    gradient = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A)),
                                    onClick = {
                                        context.startActivity(Intent(context, CompanyListingActivity::class.java))
                                    }
                                )

                                ModernModuleCard(
                                    title = "My Bookings",
                                    subtitle = "View your pickup history",
                                    icon = Icons.Default.ListAlt,
                                    gradient = listOf(Color(0xFF2196F3), Color(0xFF42A5F5)),
                                    onClick = {
                                        context.startActivity(Intent(context, MyBookingsActivity::class.java))
                                    }
                                )

                                ModernModuleCard(
                                    title = "My Wallet",
                                    subtitle = "Manage green coins",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    gradient = listOf(Color(0xFFFF9800), Color(0xFFFFB74D)),
                                    onClick = {
                                        context.startActivity(Intent(context, WalletActivity::class.java))
                                    }
                                )
                            }
                        }
                    }

                    // News Feed Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "News Feed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Icon(
                                Icons.Default.Newspaper,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    item {
                        NewsFeed()
                    }
                }
            }
        }
    }
}

// ------------------ DRAWER NAVIGATION ITEM ------------------
@Composable
fun DrawerNavigationItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDestructive) Color(0xFFF44336) else Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) Color(0xFFF44336) else Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ------------------ WELCOME CARD ------------------
@Composable
fun WelcomeCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50),
                            Color(0xFF66BB6A)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hello,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = when {
                            user?.isBuyer == true -> user.orgName ?: "Organization"
                            else -> user?.firstName ?: "User"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Let's make a difference today!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ------------------ MODERN MODULE CARD ------------------
@Composable
fun ModernModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
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
        error != null -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Unable to load news",
                    color = Color(0xFFE65100)
                )
            }
        }
        articles.isEmpty() -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            articles.take(5).forEach { news ->
                ModernNewsCard(title = news.title, desc = news.description)
            }
        }
    }
}

// ------------------ MODERN NEWS CARD ------------------
@Composable
fun ModernNewsCard(title: String?, desc: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    desc?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}