package com.tasha.recyclify

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get data from intent - passed from company listing screen
        val companyName = intent.getStringExtra("companyName") ?: "Unknown Company"
        val companyId = intent.getStringExtra("companyId") ?: ""
        val buyerId = intent.getStringExtra("buyerId") ?: ""

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Schedule Pickup") },
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
                    BookingScreen(
                        companyName = companyName,
                        companyId = companyId,
                        buyerId = buyerId,
                        auth = auth,
                        db = firestore,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    companyName: String,
    companyId: String,
    buyerId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val scrollState = rememberScrollState()

    // State variables
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var selectedWasteType by remember { mutableStateOf("E-Waste") }
    var userLocation by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Generate time slots (9 AM to 6 PM)
    val timeSlots = remember {
        listOf(
            "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM",
            "05:00 PM", "06:00 PM"
        )
    }

    val wasteTypes = listOf(
        "E-Waste", "Wet-Waste", "Dry-Waste", "PET-Waste",
        "Plastic", "Metal", "Paper/Cardboard", "Glass",
        "Textiles", "Other"
    )

    // Date formatter
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Company Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Booking Pickup With",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        // Contact Information Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Mobile Number
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { if (it.length <= 10) mobileNumber = it },
                    label = { Text("Mobile Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = userLocation,
                    onValueChange = { userLocation = it },
                    label = { Text("Pickup Location") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    placeholder = { Text("Enter your complete address") }
                )
            }
        }

        // Waste Type Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Waste Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                var wasteExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = wasteExpanded,
                    onExpandedChange = { wasteExpanded = !wasteExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedWasteType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wasteExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = wasteExpanded,
                        onDismissRequest = { wasteExpanded = false }
                    ) {
                        wasteTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedWasteType = type
                                    wasteExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Date Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val today = calendar.clone() as Calendar

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(year, month, day)
                                selectedDate = calendar.time
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.minDate = today.timeInMillis
                            today.add(Calendar.DAY_OF_MONTH, 30)
                            datePicker.maxDate = today.timeInMillis
                        }.show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDate?.let { dateFormatter.format(it) } ?: "Choose Date",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Time Slot Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Time Slot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(timeSlots) { slot ->
                        TimeSlotChip(
                            time = slot,
                            isSelected = selectedTimeSlot == slot,
                            onClick = { selectedTimeSlot = slot }
                        )
                    }
                }
            }
        }

        // Confirm Button
        Button(
            onClick = {
                when {
                    mobileNumber.length != 10 -> {
                        Toast.makeText(context, "Please enter valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                    }
                    userLocation.isBlank() -> {
                        Toast.makeText(context, "Please enter pickup location", Toast.LENGTH_SHORT).show()
                    }
                    selectedDate == null -> {
                        Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                    }
                    selectedTimeSlot == null -> {
                        Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
                    }
                    currentUser == null -> {
                        Toast.makeText(context, "Please login first!", Toast.LENGTH_SHORT).show()
                    }
                    buyerId.isBlank() -> {
                        Toast.makeText(context, "Invalid company data", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        isSubmitting = true
                        // Create booking with buyerId for direct querying
                        val booking = hashMapOf(
                            "companyName" to companyName,
                            "companyId" to companyId,
                            "buyerId" to buyerId,  // CRITICAL: Link to buyer
                            "wasteType" to selectedWasteType,
                            "date" to dateFormatter.format(selectedDate!!),
                            "timeSlot" to selectedTimeSlot!!,
                            "location" to userLocation,
                            "mobileNumber" to mobileNumber,
                            "status" to "Pending",
                            "userId" to currentUser.uid,  // Seller who made the booking
                            "timestamp" to System.currentTimeMillis()
                        )

                        db.collection("bookings")
                            .add(booking)
                            .addOnSuccessListener {
                                isSubmitting = false
                                Toast.makeText(context, "Booking confirmed successfully!", Toast.LENGTH_LONG).show()
                                (context as? ComponentActivity)?.finish()
                            }
                            .addOnFailureListener {
                                isSubmitting = false
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Pickup Booking", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun TimeSlotChip(
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF4CAF50) else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF4CAF50) else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color.White else Color.DarkGray,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}