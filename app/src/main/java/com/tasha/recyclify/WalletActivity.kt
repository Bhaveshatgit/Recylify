package com.tasha.recyclify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("My Wallet") },
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
                    WalletScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

// -------------------- WALLET SCREEN --------------------
@Composable
fun WalletScreen(modifier: Modifier = Modifier) {
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

    Column(modifier = modifier.fillMaxSize()) {
        // -------------------- TAB NAVIGATION --------------------
        val tabs = listOf("Green Wallet", "Cash Wallet", "My Vouchers")

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(3.dp),
                    color = Color.White
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
                            style = MaterialTheme.typography.labelLarge
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
                            "🎉 Purchased ${voucher.title}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Not enough coins",
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
                            "💰 Received ₹$cashEarned!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Min 5 coins required (5 = ₹1)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            2 -> PurchasedVouchersTab(purchasedVouchers)
        }
    }
}

// -------------------- GREEN WALLET TAB --------------------
@Composable
fun GreenWalletTab(coins: Int, vouchers: List<Voucher>, onBuyVoucher: (Voucher) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
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
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = coins.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Green Coins Balance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Use coins to buy vouchers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Available Vouchers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹$cash",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Cash Balance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Available coins: $coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Convert Coins to Cash",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Exchange Rate: 5 Coins = ₹1",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputCoins,
                    onValueChange = { inputCoins = it.filter { c -> c.isDigit() } },
                    label = { Text("Enter Coins") },
                    placeholder = { Text("Minimum 5 coins") },
                    leadingIcon = {
                        Icon(Icons.Default.Stars, null, tint = Color(0xFF4CAF50))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val coinsToExchange = inputCoins.toIntOrNull() ?: 0
                        onExchange(coinsToExchange)
                        inputCoins = ""
                    },
                    enabled = inputCoins.isNotEmpty() && (inputCoins.toIntOrNull() ?: 0) >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CurrencyRupee, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert to Cash")
                }
            }
        }
    }
}

// -------------------- PURCHASED VOUCHERS TAB --------------------
@Composable
fun PurchasedVouchersTab(purchasedVouchers: List<Voucher>) {
    if (purchasedVouchers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Vouchers Yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Purchase vouchers to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        voucher.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        voucher.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                   // </Column>

                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                null,
                                Modifier.size(14.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${voucher.cost}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onBuy,
                    enabled = coins >= voucher.cost,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (coins >= voucher.cost) Color(0xFF4CAF50) else Color.Gray,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        if (coins >= voucher.cost) Icons.Default.ShoppingCart else Icons.Default.Lock,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (coins >= voucher.cost) "Buy Now" else "Not Enough Coins",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

    // -------------------- PURCHASED VOUCHER CARD --------------------
    @Composable
    fun PurchasedVoucherCard(voucher: Voucher) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        voucher.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        voucher.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Store,
                            null,
                            Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            voucher.brand,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
