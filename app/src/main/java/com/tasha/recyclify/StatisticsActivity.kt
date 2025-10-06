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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load data when the current user changes (or first composition)
    LaunchedEffect(key1 = auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // No logged-in user -> show message and stop loading
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

            // Sort months in calendar order (Jan → Dec)
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

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "Total green coins collected = $totalCoins",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = 1f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color.LightGray, shape = MaterialTheme.shapes.small),
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = msg, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (monthlyCoins.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No monthly statistics available", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Try collecting coins or check your wallet transactions.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        items(monthlyCoins.toList()) { (month, coins) ->
                            val percentage = if (totalCoins > 0) coins.toFloat() / totalCoins else 0f
                            CoinStatItem(
                                label = "$coins green coins collected in $month",
                                percentage = percentage
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safe painter loader:
 * - uses LocalContext to check if the drawable exists (non-composable call inside remember)
 * - then calls painterResource() (a composable) only if resource exists — no try/catch around composable.
 */
@Composable
private fun safePainterOrNull(resId: Int): Painter? {
    val context = LocalContext.current

    // Check resource existence in a remembered, non-composable block (safe to use try/catch)
    val exists = remember(resId) {
        try {
            // getDrawable will throw Resources.NotFoundException if missing
            context.resources.getDrawable(resId, context.theme)
            true
        } catch (e: Resources.NotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    return if (exists) {
        // painterResource is a composable — call it directly (not inside try/catch)
        painterResource(id = resId)
    } else {
        null
    }
}

@Composable
fun CoinStatItem(label: String, percentage: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "coinAnimation"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.size(120.dp),
                strokeWidth = 12.dp,
                color = Color(0xFF4CAF50)
            )

            val coinPainter = safePainterOrNull(R.drawable.green_coin)
            if (coinPainter != null) {
                Image(
                    painter = coinPainter,
                    contentDescription = "Green Coin",
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsPreview() {
    RecyclifyTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total green coins collected = 400", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = 1f, modifier = Modifier.fillMaxWidth().height(12.dp))
            Spacer(modifier = Modifier.height(24.dp))

            CoinStatItem(label = "120 green coins collected in January", percentage = 120f / 400f)
            Spacer(modifier = Modifier.height(12.dp))
            CoinStatItem(label = "80 green coins collected in February", percentage = 80f / 400f)
            Spacer(modifier = Modifier.height(12.dp))
            CoinStatItem(label = "200 green coins collected in March", percentage = 200f / 400f)
        }
    }
}
