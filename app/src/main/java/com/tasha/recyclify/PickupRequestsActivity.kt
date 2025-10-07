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
import com.google.firebase.firestore.Query
import com.tasha.recyclify.ui.theme.RecyclifyTheme

// ------------------ DATA MODEL ------------------
// Reusing the Booking model but for buyer's perspective
data class PickupRequest(
    val id: String = "",
    val companyName: String = "",
    val companyId: String = "",  // Link to company
    val wasteType: String = "",
    val location: String = "",
    val mobileNumber: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "Pending", // Pending, Confirmed, Completed, Cancelled
    val userId: String = "",  // Seller who booked
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
    var buyerCompanies by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }

    // First, get all companies owned by this buyer
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            db.collection("companies")
                .whereEqualTo("buyerId", uid)
                .get()
                .addOnSuccessListener { companiesSnapshot ->
                    // Get company names
                    buyerCompanies = companiesSnapshot.documents.mapNotNull {
                        it.getString("companyName")
                    }

                    // Now fetch all bookings for these companies
                    if (buyerCompanies.isNotEmpty()) {
                        db.collection("bookings")
                            .whereIn("companyName", buyerCompanies)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    error.printStackTrace()
                                    isLoading = false
                                    return@addSnapshotListener
                                }
                                val data = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(PickupRequest::class.java)?.copy(id = doc.id)
                                } ?: emptyList()
                                requests = data
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }
    }

    // Filter requests
    val filteredRequests = remember(requests, selectedFilter) {
        when (selectedFilter) {
            "All" -> requests
            else -> requests.filter { it.status == selectedFilter }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats Card
        RequestStatsCard(requests)

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChips(
                label = "All",
                count = requests.size,
                isSelected = selectedFilter == "All",
                onClick = { selectedFilter = "All" }
            )
            FilterChips(
                label = "Pending",
                count = requests.count { it.status == "Pending" },
                isSelected = selectedFilter == "Pending",
                onClick = { selectedFilter = "Pending" }
            )
            FilterChips(
                label = "Confirmed",
                count = requests.count { it.status == "Confirmed" },
                isSelected = selectedFilter == "Confirmed",
                onClick = { selectedFilter = "Confirmed" }
            )
            FilterChips(
                label = "Completed",
                count = requests.count { it.status == "Completed" },
                isSelected = selectedFilter == "Completed",
                onClick = { selectedFilter = "Completed" }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Requests List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else if (buyerCompanies.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Business,
                title = "No Companies Yet",
                subtitle = "Create a company first to receive pickup requests"
            )
        } else if (filteredRequests.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Inbox,
                title = if (selectedFilter == "All") "No Requests Yet" else "No $selectedFilter Requests",
                subtitle = "Pickup requests from sellers will appear here"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRequests) { request ->
                    PickupRequestCard(request = request, db = db)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RequestStatsCard(requests: List<PickupRequest>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItems(
                icon = Icons.Default.Inbox,
                label = "Total",
                value = requests.size.toString(),
                color = Color(0xFF2196F3)
            )
            StatItems(
                icon = Icons.Default.Schedule,
                label = "Pending",
                value = requests.count { it.status == "Pending" }.toString(),
                color = Color(0xFFFF9800)
            )
            StatItems(
                icon = Icons.Default.CheckCircle,
                label = "Confirmed",
                value = requests.count { it.status == "Confirmed" }.toString(),
                color = Color(0xFF4CAF50)
            )
            StatItems(
                icon = Icons.Default.Done,
                label = "Completed",
                value = requests.count { it.status == "Completed" }.toString(),
                color = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
fun StatItems(icon: ImageVector, label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
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
            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray.copy(alpha = 0.7f)
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

    // Status configuration
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
        Column(modifier = Modifier.padding(16.dp)) {
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = request.wasteType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = statusConfigs.color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusConfigs.icon,
                            contentDescription = null,
                            tint = statusConfigs.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusConfigs.text,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusConfigs.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Request Details
            RequestDetailRow(Icons.Default.CalendarToday, "Date", request.date)
            RequestDetailRow(Icons.Default.Schedule, "Time", request.timeSlot)
            RequestDetailRow(Icons.Default.LocationOn, "Location", request.location)
            RequestDetailRow(Icons.Default.Phone, "Contact", request.mobileNumber)

            // Action Buttons
            if (request.status != "Cancelled" && request.status != "Completed") {
                Spacer(modifier = Modifier.height(16.dp))

                when (request.status) {
                    "Pending" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showConfirmDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm")
                            }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF44336)
                                )
                            ) {
                                Icon(Icons.Default.Cancel, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject")
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
                                )
                            ) {
                                Icon(Icons.Default.Done, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark as Done")
                            }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF44336)
                                )
                            ) {
                                Icon(Icons.Default.Cancel, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
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
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
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
                Icon(Icons.Default.Warning, null, tint = Color(0xFFF44336), modifier = Modifier.size(32.dp))
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
                Icon(Icons.Default.Done, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(32.dp))
            },
            title = { Text("Mark as Completed?") },
            text = { Text("Mark this pickup as completed? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        showCompleteDialog = false
                        db.collection("bookings")
                            .document(request.id)
                            .update("status", "Completed")
                            .addOnSuccessListener {
                                isProcessing = false
                                Toast.makeText(context, "Pickup marked as completed!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
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