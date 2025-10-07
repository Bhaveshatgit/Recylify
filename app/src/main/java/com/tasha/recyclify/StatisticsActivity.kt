package com.tasha.recyclify

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Statistics") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF4CAF50),
                                titleContentColor = Color.White
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                                }
                            }
                        )
                    },
                    containerColor = Color(0xFFF5F5F5)
                ) { padding ->
                    StatisticsContent(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun StatisticsContent(modifier: Modifier = Modifier) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var monthlyCoins by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalCoins by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.d("Statistics", "No logged-in user")
            isLoading = false
            errorMessage = "Not signed in. Please sign in to view statistics."
            monthlyCoins = emptyMap()
            totalCoins = 0
            return@LaunchedEffect
        }

        try {
            isLoading = true
            errorMessage = null

            val snapshot = firestore.collection("wallet")
                .document(uid)
                .collection("transactions")
                .get()
                .await()

            val coinsPerMonth = mutableMapOf<String, Int>()
            var sum = 0

            for (doc in snapshot.documents) {
                val coins = doc.getLong("coins")?.toInt() ?: 0
                val timestamp = doc.getDate("timestamp") ?: Date()
                val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(timestamp)

                coinsPerMonth[month] = coinsPerMonth.getOrDefault(month, 0) + coins
                sum += coins
            }

            val monthOrder = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            monthlyCoins = coinsPerMonth.toSortedMap(compareBy { monthOrder.indexOf(it) })
            totalCoins = sum

            if (monthlyCoins.isEmpty()) {
                errorMessage = "No transactions found."
            }
        } catch (e: Exception) {
            Log.e("Statistics", "Error loading statistics", e)
            errorMessage = "Failed to load statistics: ${e.localizedMessage ?: "Unknown error"}"
            monthlyCoins = emptyMap()
            totalCoins = 0
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading statistics...", color = Color.Gray)
                    }
                }
            }

            else -> {
                // Header Card with Total
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = totalCoins.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Total Green Coins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Collected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                if (monthlyCoins.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No monthly statistics available",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Complete pickups to earn coins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Monthly Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        items(monthlyCoins.toList()) { (month, coins) ->
                            val percentage = if (totalCoins > 0) coins.toFloat() / totalCoins else 0f
                            MonthlyStatCard(month = month, coins = coins, percentage = percentage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyStatCard(month: String, coins: Int, percentage: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "monthProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress
            Box(
                modifier = Modifier.size(70.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(70.dp),
                    strokeWidth = 6.dp,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$coins coins collected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}