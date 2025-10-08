package com.tasha.recyclify

import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme

// ------------------ DATA MODEL ------------------
data class PickupRequest(
    val id: String = "",
    val companyName: String = "",
    val companyId: String = "",
    val buyerId: String = "",
    val wasteType: String = "",
    val location: String = "",
    val mobileNumber: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "Pending",
    val userId: String = "",
    val timestamp: Long = 0L
)

// ------------------ MAIN ACTIVITY ------------------
class PickupRequestsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Pickup Requests") },
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
                    PickupRequestsScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

// ------------------ UI ------------------
@Composable
fun PickupRequestsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var requests by remember { mutableStateOf(listOf<PickupRequest>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var hasCompanies by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch all bookings directly by buyerId
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            // First check if buyer has any companies
            db.collection("companies")
                .whereEqualTo("buyerId", uid)
                .limit(1)
                .get()
                .addOnSuccessListener { companiesSnapshot ->
                    hasCompanies = !companiesSnapshot.isEmpty

                    if (hasCompanies) {
                        // Fetch all bookings where buyerId matches
                        db.collection("bookings")
                            .whereEqualTo("buyerId", uid)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    error.printStackTrace()
                                    isLoading = false
                                    errorMessage = error.message
                                    Toast.makeText(
                                        context,
                                        "Error: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@addSnapshotListener
                                }

                                val data = snapshot?.documents?.mapNotNull { doc ->
                                    try {
                                        doc.toObject(PickupRequest::class.java)?.copy(id = doc.id)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                } ?: emptyList()

                                // Sort in memory by timestamp (newest first)
                                requests = data.sortedByDescending { it.timestamp }
                                isLoading = false
                                errorMessage = null
                            }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    errorMessage = e.message
                    Toast.makeText(
                        context,
                        "Error checking companies: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            isLoading = false
            errorMessage = "User not logged in"
        }
    }

    // Filter requests
    val filteredRequests = remember(requests, selectedFilter) {
        when (selectedFilter) {
            "All" -> requests
            else -> requests.filter { it.status == selectedFilter }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Stats Card - Compact
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactStatItem(
                    icon = Icons.Default.Inbox,
                    label = "Total",
                    value = requests.size.toString(),
                    color = Color(0xFF2196F3)
                )
                CompactStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Pending",
                    value = requests.count { it.status == "Pending" }.toString(),
                    color = Color(0xFFFF9800)
                )
                CompactStatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Confirmed",
                    value = requests.count { it.status == "Confirmed" }.toString(),
                    color = Color(0xFF4CAF50)
                )
                CompactStatItem(
                    icon = Icons.Default.Done,
                    label = "Done",
                    value = requests.count { it.status == "Completed" }.toString(),
                    color = Color(0xFF9C27B0)
                )
            }
        }

        // Filter Chips - Scrollable Row
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChips(
                    label = "All",
                    count = requests.size,
                    isSelected = selectedFilter == "All",
                    onClick = { selectedFilter = "All" }
                )
            }
            item {
                FilterChips(
                    label = "Pending",
                    count = requests.count { it.status == "Pending" },
                    isSelected = selectedFilter == "Pending",
                    onClick = { selectedFilter = "Pending" }
                )
            }
            item {
                FilterChips(
                    label = "Confirmed",
                    count = requests.count { it.status == "Confirmed" },
                    isSelected = selectedFilter == "Confirmed",
                    onClick = { selectedFilter = "Confirmed" }
                )
            }
            item {
                FilterChips(
                    label = "Completed",
                    count = requests.count { it.status == "Completed" },
                    isSelected = selectedFilter == "Completed",
                    onClick = { selectedFilter = "Completed" }
                )
            }
            item {
                FilterChips(
                    label = "Cancelled",
                    count = requests.count { it.status == "Cancelled" },
                    isSelected = selectedFilter == "Cancelled",
                    onClick = { selectedFilter = "Cancelled" }
                )
            }
        }

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // Requests List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading requests...", color = Color.Gray)
                }
            }
        } else if (!hasCompanies) {
            EmptyStateView(
                icon = Icons.Default.Business,
                title = "No Companies Yet",
                subtitle = "Create a company first to receive pickup requests"
            )
        } else if (filteredRequests.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Inbox,
                title = if (selectedFilter == "All") "No Requests Yet" else "No $selectedFilter Requests",
                subtitle = if (selectedFilter == "All")
                    "Pickup requests from sellers will appear here"
                else
                    "No requests with status: $selectedFilter"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRequests) { request ->
                    PickupRequestCard(request = request, db = db)
                }
            }
        }
    }
}

@Composable
fun CompactStatItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun FilterChips(label: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text("$label ($count)") },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4CAF50),
            selectedLabelColor = Color.White
        )
    )
}



@Composable
fun EmptyStateView(icon: ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun PickupRequestCard(request: PickupRequest, db: FirebaseFirestore) {
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val statusConfigs = when (request.status) {
        "Pending" -> StatusConfig(
            color = Color(0xFFFF9800),
            icon = Icons.Default.Schedule,
            text = "Pending"
        )
        "Confirmed" -> StatusConfig(
            color = Color(0xFF4CAF50),
            icon = Icons.Default.CheckCircle,
            text = "Confirmed"
        )
        "Completed" -> StatusConfig(
            color = Color(0xFF9C27B0),
            icon = Icons.Default.Done,
            text = "Completed"
        )
        "Cancelled" -> StatusConfig(
            color = Color(0xFFF44336),
            icon = Icons.Default.Cancel,
            text = "Cancelled"
        )
        else -> StatusConfig(
            color = Color.Gray,
            icon = Icons.Default.Info,
            text = request.status
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Recycling,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = request.wasteType,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = statusConfigs.color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusConfigs.icon,
                            contentDescription = null,
                            tint = statusConfigs.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusConfigs.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusConfigs.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Request Details - Compact
            RequestDetailRow(Icons.Default.CalendarToday, "Date", request.date)
            RequestDetailRow(Icons.Default.Schedule, "Time", request.timeSlot)
            RequestDetailRow(Icons.Default.LocationOn, "Location", request.location)
            RequestDetailRow(Icons.Default.Phone, "Contact", request.mobileNumber)

            // Action Buttons
            if (request.status != "Cancelled" && request.status != "Completed") {
                Spacer(modifier = Modifier.height(12.dp))

                when (request.status) {
                    "Pending" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showConfirmDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF44336)
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    "Confirmed" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showCompleteDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9C27B0)
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Done, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Complete", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF44336)
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
            },
            title = { Text("Confirm Pickup?") },
            text = { Text("Confirm this pickup request from the seller? They will be notified.") },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        showConfirmDialog = false
                        db.collection("bookings")
                            .document(request.id)
                            .update("status", "Confirmed")
                            .addOnSuccessListener {
                                isProcessing = false
                                Toast.makeText(context, "Pickup confirmed!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFF44336), modifier = Modifier.size(28.dp))
            },
            title = { Text("Cancel Pickup?") },
            text = { Text("Are you sure you want to cancel this pickup request?") },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        showCancelDialog = false
                        db.collection("bookings")
                            .document(request.id)
                            .update("status", "Cancelled")
                            .addOnSuccessListener {
                                isProcessing = false
                                Toast.makeText(context, "Pickup cancelled", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // Complete Dialog
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            icon = {
                Icon(Icons.Default.Done, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(28.dp))
            },
            title = { Text("Mark as Completed?") },
            text = { Text("Mark this pickup as completed? Seller will receive 2 green coins as reward!") },
            confirmButton = {
                Button(
                    // Replace the Complete Dialog button's onClick in PickupRequestCard

                    onClick = {
                        isProcessing = true
                        showCompleteDialog = false

                        val sellerId = request.userId
                        val sellerWalletRef = db.collection("wallet").document(sellerId)

                        // Step 1: Update booking status first
                        db.collection("bookings")
                            .document(request.id)
                            .update("status", "Completed")
                            .addOnSuccessListener {
                                Log.d("PickupComplete", "Booking status updated successfully")

                                // Step 2: Check if wallet exists, create if needed, then award coins
                                sellerWalletRef.get()
                                    .addOnSuccessListener { walletDoc ->
                                        if (!walletDoc.exists()) {
                                            Log.d("PickupComplete", "Wallet doesn't exist, creating new wallet")

                                            // Wallet doesn't exist - create it with initial 2 coins
                                            sellerWalletRef.set(hashMapOf(
                                                "coins" to 2L,
                                                "cashBalance" to 0L
                                            )).addOnSuccessListener {
                                                Log.d("PickupComplete", "Wallet created successfully")

                                                // Create transaction record
                                                sellerWalletRef
                                                    .collection("transactions")
                                                    .add(hashMapOf(
                                                        "coins" to 2L,
                                                        "type" to "earned",
                                                        "description" to "Pickup completed - ${request.companyName}",
                                                        "timestamp" to com.google.firebase.Timestamp.now()
                                                    ))
                                                    .addOnSuccessListener {
                                                        Log.d("PickupComplete", "Transaction record created")
                                                        isProcessing = false
                                                        Toast.makeText(
                                                            context,
                                                            "✅ Completed! Seller earned 2 green coins",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("PickupComplete", "Failed to create transaction", e)
                                                        isProcessing = false
                                                        Toast.makeText(
                                                            context,
                                                            "Completed but transaction record failed: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }.addOnFailureListener { e ->
                                                Log.e("PickupComplete", "Failed to create wallet", e)
                                                isProcessing = false
                                                Toast.makeText(
                                                    context,
                                                    "Error creating wallet: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } else {
                                            Log.d("PickupComplete", "Wallet exists, updating coins")

                                            // Wallet exists - use transaction to update it atomically
                                            db.runTransaction { transaction ->
                                                val snapshot = transaction.get(sellerWalletRef)
                                                val currentCoins = snapshot.getLong("coins") ?: 0L
                                                val newCoins = currentCoins + 2

                                                Log.d("PickupComplete", "Current coins: $currentCoins, New coins: $newCoins")

                                                // Update only the coins field
                                                transaction.update(sellerWalletRef, "coins", newCoins)

                                                // Create transaction record
                                                val transactionRef = sellerWalletRef
                                                    .collection("transactions")
                                                    .document()

                                                transaction.set(transactionRef, hashMapOf(
                                                    "coins" to 2L,
                                                    "type" to "earned",
                                                    "description" to "Pickup completed - ${request.companyName}",
                                                    "timestamp" to com.google.firebase.Timestamp.now()
                                                ))

                                                newCoins // Return value for logging
                                            }.addOnSuccessListener { newCoins ->
                                                Log.d("PickupComplete", "Transaction successful. New balance: $newCoins")
                                                isProcessing = false
                                                Toast.makeText(
                                                    context,
                                                    "✅ Completed! Seller earned 2 green coins",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }.addOnFailureListener { e ->
                                                Log.e("PickupComplete", "Transaction failed", e)
                                                isProcessing = false
                                                Toast.makeText(
                                                    context,
                                                    "Error awarding coins: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("PickupComplete", "Failed to check wallet", e)
                                        isProcessing = false
                                        Toast.makeText(
                                            context,
                                            "Error checking wallet: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("PickupComplete", "Failed to update booking", e)
                                isProcessing = false
                                Toast.makeText(
                                    context,
                                    "Error updating booking: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("Mark Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RequestDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class StatusConfig(
    val color: Color,
    val icon: ImageVector,
    val text: String
)