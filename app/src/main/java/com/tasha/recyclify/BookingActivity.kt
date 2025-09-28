package com.tasha.recyclify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme

class BookingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val companyName = intent.getStringExtra("companyName") ?: "Unknown Company"
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Book Pickup - $companyName") }
                        )
                    }
                ) { padding ->
                    BookingScreen(
                        companyName = companyName,
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
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = auth.currentUser

    var selectedDate by remember { mutableStateOf("2025-08-30") }
    var selectedTime by remember { mutableStateOf("10:00 AM - 12:00 PM") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Pickup booking for:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(companyName, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(24.dp))

        // Date selection
        Text("Select Date:")
        Spacer(modifier = Modifier.height(4.dp))
        var dateExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = dateExpanded,
            onExpandedChange = { dateExpanded = !dateExpanded }
        ) {
            TextField(
                value = selectedDate,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = dateExpanded, onDismissRequest = { dateExpanded = false }) {
                listOf("2025-08-30", "2025-08-31", "2025-09-01").forEach { date ->
                    DropdownMenuItem(
                        text = { Text(date) },
                        onClick = {
                            selectedDate = date
                            dateExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time slot selection
        Text("Select Time Slot:")
        Spacer(modifier = Modifier.height(4.dp))
        var timeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = timeExpanded,
            onExpandedChange = { timeExpanded = !timeExpanded }
        ) {
            TextField(
                value = selectedTime,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                listOf("10:00 AM - 12:00 PM", "12:00 PM - 2:00 PM", "4:00 PM - 6:00 PM").forEach { slot ->
                    DropdownMenuItem(
                        text = { Text(slot) },
                        onClick = {
                            selectedTime = slot
                            timeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (currentUser == null) {
                Toast.makeText(context, "Please login first!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            val booking = hashMapOf(
                "companyName" to companyName,
                "date" to selectedDate,
                "timeSlot" to selectedTime,
                "status" to "Pending",
                "userId" to currentUser.uid
            )

            db.collection("bookings")
                .add(booking)
                .addOnSuccessListener {
                    Toast.makeText(context, "Booking confirmed!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }) {
            Text("Confirm Pickup")
        }
    }
}
