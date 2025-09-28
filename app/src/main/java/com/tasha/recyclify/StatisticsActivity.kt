package com.tasha.recyclify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                StatisticsScreen(onWalletClick = {
                    startActivity(Intent(this, WalletActivity::class.java))
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onWalletClick: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics") },
                actions = {
                    IconButton(onClick = { onWalletClick() }) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Wallet"
                        )
                    }
                }
            )
        }
    ) { padding ->
        StatisticsContent(Modifier.padding(padding))
    }
}

@Composable
fun StatisticsContent(modifier: Modifier = Modifier) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var monthlyCoins by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalCoins by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load Firestore data
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
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

            monthlyCoins = coinsPerMonth.toSortedMap(compareByDescending { it })
            totalCoins = sum
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = "Total green coins collected = $totalCoins",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { if (totalCoins > 0) 1f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color.LightGray, shape = MaterialTheme.shapes.small),
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(32.dp))

            monthlyCoins.forEach { (month, coins) ->
                val percentage = if (totalCoins > 0) coins.toFloat() / totalCoins else 0f
                CoinStatItem(
                    label = "$coins green coins collected in $month",
                    percentage = percentage
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CoinStatItem(label: String, percentage: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "coinAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Animated circular progress
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(150.dp),
                strokeWidth = 12.dp,
                color = Color(0xFF4CAF50)
            )

            // Your coin image in the center
            Image(
                painter = painterResource(id = R.drawable.green_coin), // <-- place your PNG here
                contentDescription = "Green Coin",
                modifier = Modifier.size(100.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
