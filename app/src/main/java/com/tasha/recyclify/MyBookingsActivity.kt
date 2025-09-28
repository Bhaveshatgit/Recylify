package com.tasha.recyclify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme

// ------------------ DATA MODEL ------------------
data class Booking(
    val id: String = "",
    val wasteType: String = "",
    val quantity: String = "",
    val status: String = "Pending", // Pending, Done, Cancelled
    val userId: String = ""
)

// ------------------ MAIN ACTIVITY ------------------
class MyBookingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecyclifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
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

    // Load bookings in real-time
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            db.collection("bookings")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }
                    val data = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    bookings = data
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Bookings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (bookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings) { booking ->
                    BookingCard(booking = booking, db = db)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- CLEAR ALL BOOKINGS BUTTON ----------------
            Button(
                onClick = {
                    currentUser?.uid?.let { uid ->
                        db.collection("bookings")
                            .whereEqualTo("userId", uid)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val batch = db.batch()
                                snapshot.documents.forEach { doc ->
                                    batch.delete(doc.reference)
                                }
                                batch.commit().addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "All bookings cleared successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Bookings")
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking, db: FirebaseFirestore) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Waste Type: ${booking.wasteType}", style = MaterialTheme.typography.bodyLarge)
            Text("Quantity: ${booking.quantity}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${booking.status}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        // Mark booking as Done
                        db.collection("bookings").document(booking.id)
                            .update("status", "Done")
                            .addOnSuccessListener {
                                currentUser?.uid?.let { uid ->
                                    db.collection("wallet")
                                        .document(uid)
                                        .update("coins", FieldValue.increment(1))
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Booking marked as Done. +1 Green Coin added!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener {
                                            // Create wallet if not exists
                                            db.collection("wallet").document(uid)
                                                .set(mapOf("coins" to 1))
                                            Toast.makeText(
                                                context,
                                                "Wallet created. +1 Green Coin added!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                    },
                    enabled = booking.status == "Pending"
                ) {
                    Text("Done")
                }

                OutlinedButton(
                    onClick = {
                        db.collection("bookings").document(booking.id)
                            .update("status", "Cancelled")
                    },
                    enabled = booking.status == "Pending"
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
