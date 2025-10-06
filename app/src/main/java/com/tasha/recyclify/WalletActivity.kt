package com.tasha.recyclify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme

// -------------------- DATA CLASS --------------------
data class Voucher(
    val title: String = "",
    val description: String = "",
    val cost: Int = 0,
    val brand: String = ""
)

// -------------------- MAIN ACTIVITY --------------------
class WalletActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                WalletScreen()
            }
        }
    }
}

// -------------------- WALLET SCREEN --------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var coins by remember { mutableStateOf(0) }
    var cash by remember { mutableStateOf(0) }
    var purchasedVouchers by remember { mutableStateOf(listOf<Voucher>()) }

    var selectedTab by remember { mutableStateOf(0) }

    val vouchers = remember {
        listOf(
            Voucher("Meesho ₹500 Off", "₹500 off on men's clothing", 3, "Meesho"),
            Voucher("Amazon ₹200 Off", "₹200 off on household items", 2, "Amazon"),
            Voucher("Flipkart ₹300 Off", "₹300 off on electronics", 4, "Flipkart"),
            Voucher("Zomato ₹150 Off", "₹150 off on food orders", 1, "Zomato"),
            Voucher("Nykaa ₹400 Off", "₹400 off on beauty products", 3, "Nykaa")
        )
    }

    // 🔹 Fetch wallet info + purchased vouchers
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("wallet").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    coins = snapshot?.getLong("coins")?.toInt() ?: 0
                    cash = snapshot?.getLong("cashBalance")?.toInt() ?: 0
                }

            db.collection("wallet")
                .document(uid)
                .collection("purchased_vouchers")
                .addSnapshotListener { snapshot, _ ->
                    val list = snapshot?.documents?.mapNotNull { it.toObject(Voucher::class.java) } ?: emptyList()
                    purchasedVouchers = list
                }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Wallet") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFA9FC8A))
            )
        },
        containerColor = Color(0xFFF5FFF1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // -------------------- TAB NAVIGATION --------------------
            val tabs = listOf("Green Wallet", "Cash Wallet", "My Vouchers")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFA9FC8A),
                contentColor = Color.Black,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp),
                        color = Color(0xFF4CAF50)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.Black else Color.DarkGray
                            )
                        }
                    )
                }
            }

            // -------------------- TAB CONTENT --------------------
            when (selectedTab) {
                0 -> GreenWalletTab(
                    coins = coins,
                    vouchers = vouchers,
                    onBuyVoucher = { voucher ->
                        if (coins >= voucher.cost) {
                            val newCoins = coins - voucher.cost
                            coins = newCoins
                            currentUser?.let { user ->
                                val uid = user.uid
                                db.collection("wallet").document(uid).update("coins", newCoins)
                                db.collection("wallet").document(uid)
                                    .collection("purchased_vouchers")
                                    .document(voucher.title)
                                    .set(voucher)
                            }
                            Toast.makeText(
                                context,
                                "🎉 Purchased ${voucher.title}! Remaining: $newCoins coins",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Not enough coins to buy ${voucher.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                1 -> CashWalletTab(
                    coins = coins,
                    cash = cash,
                    onExchange = { coinsToExchange ->
                        if (coinsToExchange >= 5 && coinsToExchange <= coins) {
                            val cashEarned = coinsToExchange / 5
                            val newCoins = coins - coinsToExchange
                            val newCash = cash + cashEarned
                            coins = newCoins
                            cash = newCash
                            currentUser?.let { user ->
                                val uid = user.uid
                                db.collection("wallet").document(uid).update(
                                    mapOf("coins" to newCoins, "cashBalance" to newCash)
                                )
                            }
                            Toast.makeText(
                                context,
                                "💰 You received ₹$cashEarned for $coinsToExchange coins!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Exchange at least 5 coins (5 = ₹1)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                2 -> PurchasedVouchersTab(purchasedVouchers)
            }
        }
    }
}

// -------------------- GREEN WALLET TAB --------------------
@Composable
fun GreenWalletTab(coins: Int, vouchers: List<Voucher>, onBuyVoucher: (Voucher) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = coins.toString(), fontSize = 42.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Green Coins", style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            Text("🎟️ Available Vouchers", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        items(vouchers) { voucher ->
            VoucherCard(voucher, coins, onBuy = { onBuyVoucher(voucher) })
        }
    }
}

// -------------------- CASH WALLET TAB --------------------
@Composable
fun CashWalletTab(coins: Int, cash: Int, onExchange: (Int) -> Unit) {
    var inputCoins by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFFFD54F), shape = MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "₹$cash", fontSize = 42.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Total Cash Balance", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("💰 Cash Wallet", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Exchange Rate: 5 Coins = ₹1", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputCoins,
            onValueChange = { inputCoins = it.filter { c -> c.isDigit() } },
            label = { Text("Enter Coins to Exchange") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val coinsToExchange = inputCoins.toIntOrNull() ?: 0
                onExchange(coinsToExchange)
                inputCoins = ""
            },
            enabled = inputCoins.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Convert to Cash")
        }
    }
}

// -------------------- PURCHASED VOUCHERS TAB --------------------
@Composable
fun PurchasedVouchersTab(purchasedVouchers: List<Voucher>) {
    if (purchasedVouchers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You haven't purchased any vouchers yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(purchasedVouchers) { voucher ->
                PurchasedVoucherCard(voucher)
            }
        }
    }
}

// -------------------- VOUCHER CARD --------------------
@Composable
fun VoucherCard(voucher: Voucher, coins: Int, onBuy: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(voucher.title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(voucher.description, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Cost: ${voucher.cost} Coins", color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onBuy,
                enabled = coins >= voucher.cost,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (coins >= voucher.cost) Color(0xFF4CAF50) else Color.Gray
                )
            ) {
                Text(if (coins >= voucher.cost) "Buy Now" else "Not Enough Coins")
            }
        }
    }
}

// -------------------- PURCHASED VOUCHER CARD --------------------
@Composable
fun PurchasedVoucherCard(voucher: Voucher) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(voucher.title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(voucher.description, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Brand: ${voucher.brand}", color = Color(0xFF2E7D32))
        }
    }
}
