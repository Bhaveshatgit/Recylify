package com.tasha.recyclify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
data class Booking(
    val id: String = "",
    val companyName: String = "",
    val wasteType: String = "",
    val location: String = "",
    val mobileNumber: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "Pending", // Pending, Confirmed, Completed, Cancelled
    val userId: String = "",
    val timestamp: Long = 0L
)

// ------------------ MAIN ACTIVITY ------------------
class MyBookingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("My Bookings") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF4CAF50),
                                titleContentColor = Color.White
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }
                        )
                    },
                    containerColor = Color(0xFFF5F5F5)
                ) { padding ->
                    MyBookingsScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

// ------------------ UI ------------------
@Composable
fun MyBookingsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var bookings by remember { mutableStateOf(listOf<Booking>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }

    // Load bookings in real-time with ordering
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            db.collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        isLoading = false
                        return@addSnapshotListener
                    }
                    val data = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    bookings = data
                    isLoading = false
                }
        } ?: run {
            isLoading = false
        }
    }

    // Filter bookings based on status
    val filteredBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            "All" -> bookings
            else -> bookings.filter { it.status == selectedFilter }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats Card
        BookingStatsCard(bookings)

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                label = "All",
                count = bookings.size,
                isSelected = selectedFilter == "All",
                onClick = { selectedFilter = "All" }
            )
            FilterChip(
                label = "Pending",
                count = bookings.count { it.status == "Pending" },
                isSelected = selectedFilter == "Pending",
                onClick = { selectedFilter = "Pending" }
            )
            FilterChip(
                label = "Confirmed",
                count = bookings.count { it.status == "Confirmed" },
                isSelected = selectedFilter == "Confirmed",
                onClick = { selectedFilter = "Confirmed" }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bookings List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else if (filteredBookings.isEmpty()) {
            EmptyBookingsView(selectedFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBookings) { booking ->
                    BookingCard(
                        booking = booking,
                        db = db,
                        onCancelled = {
                            Toast.makeText(
                                context,
                                "Booking cancelled successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun BookingStatsCard(bookings: List<Booking>) {
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
            StatItem(
                icon = Icons.Default.List,
                label = "Total",
                value = bookings.size.toString(),
                color = Color(0xFF2196F3)
            )
            StatItem(
                icon = Icons.Default.Schedule,
                label = "Pending",
                value = bookings.count { it.status == "Pending" }.toString(),
                color = Color(0xFFFF9800)
            )
            StatItem(
                icon = Icons.Default.CheckCircle,
                label = "Confirmed",
                value = bookings.count { it.status == "Confirmed" }.toString(),
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
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
fun FilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text("$label ($count)") },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4CAF50),
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun EmptyBookingsView(filter: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (filter == "All") "No bookings yet" else "No $filter bookings",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start booking pickups to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    db: FirebaseFirestore,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }

    // Status configuration
    val statusConfigs = when (booking.status) {
        "Pending" -> StatusConfig1(
            color = Color(0xFFFF9800),
            icon = Icons.Default.Schedule,
            text = "Pending"
        )
        "Confirmed" -> StatusConfig1(
            color = Color(0xFF4CAF50),
            icon = Icons.Default.CheckCircle,
            text = "Confirmed"
        )
        "Completed" -> StatusConfig1(
            color = Color(0xFF2196F3),
            icon = Icons.Default.Done,
            text = "Completed"
        )
        "Cancelled" -> StatusConfig1(
            color = Color(0xFFF44336),
            icon = Icons.Default.Cancel,
            text = "Cancelled"
        )
        else -> StatusConfig1(
            color = Color.Gray,
            icon = Icons.Default.Info,
            text = booking.status
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Company and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = booking.wasteType,
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

            // Booking Details
            BookingDetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Date",
                value = booking.date
            )
            BookingDetailRow(
                icon = Icons.Default.Schedule,
                label = "Time",
                value = booking.timeSlot
            )
            BookingDetailRow(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = booking.location
            )
            BookingDetailRow(
                icon = Icons.Default.Phone,
                label = "Contact",
                value = booking.mobileNumber
            )

            // Cancel Button (only for Pending/Confirmed bookings)
            if (booking.status == "Pending" || booking.status == "Confirmed") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    enabled = !isCancelling
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFF44336)
                        )
                    } else {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Booking")
                    }
                }
            }
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Cancel Booking?") },
            text = { Text("Are you sure you want to cancel this pickup booking?") },
            confirmButton = {
                Button(
                    onClick = {
                        isCancelling = true
                        showCancelDialog = false
                        db.collection("bookings")
                            .document(booking.id)
                            .update("status", "Cancelled")
                            .addOnSuccessListener {
                                isCancelling = false
                                onCancelled()
                            }
                            .addOnFailureListener { e ->
                                isCancelling = false
                                Toast.makeText(
                                    context,
                                    "Failed to cancel: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep It")
                }
            }
        )
    }
}

@Composable
fun BookingDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
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

data class StatusConfig1(
    val color: Color,
    val icon: ImageVector,
    val text: String
)