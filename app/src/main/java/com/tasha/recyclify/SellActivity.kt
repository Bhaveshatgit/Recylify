package com.tasha.recyclify.ui.sell

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.tasha.recyclify.BookingActivity
import com.tasha.recyclify.ui.theme.RecyclifyTheme

data class RecyclerCompany(
    val name: String,
    val location: String,
    val notes: String
)

class SellActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Sell to Authorized Collectors") }
                        )
                    }
                ) { padding ->
                    SellScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun SellScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val context = LocalContext.current

    val companies = listOf(
        RecyclerCompany("Green India E-Waste & Recycling OPC", "Thane, Maharashtra", "Authorized dismantler (valid till 2026)"),
        RecyclerCompany("Green IT Recycling Center / Centre", "Pune, Maharashtra", "Authorized dismantlers"),
        RecyclerCompany("Green Planet Recycling Solutions", "Thane, Maharashtra", "Authorized dismantler"),
        RecyclerCompany("Green Tech Solution Industries", "Solapur, Maharashtra", "Authorization expired"),
        RecyclerCompany("Green Valley E Waste Management", "Palghar, Maharashtra", "Authorized dismantler"),
        RecyclerCompany("Greenscape Eco Management Pvt. Ltd.", "Maharashtra", "Authorization expired"),
        RecyclerCompany("J.S. Enterprises", "Pune, Maharashtra", "Authorized dismantler"),
        RecyclerCompany("JRS Recycling Solutions Pvt. Ltd.", "Thane, Maharashtra", "Authorized dismantler"),
        RecyclerCompany("ECS Environment Pvt. Ltd.", "Ahmedabad", "Formal recycler, data sanitization"),
        RecyclerCompany("3R Recycler Pvt. Ltd.", "New Delhi", "Clean-engineering e-waste recycler"),
        RecyclerCompany("E Parisaraa Pvt. Ltd.", "Bengaluru", "Secure disposal & recycling"),
        RecyclerCompany("Koscove E-Waste Pvt. Ltd.", "Dadri/NCR", "Pickup + R2-certified processing"),
        RecyclerCompany("Resource E Waste Solutions Pvt. Ltd.", "New Delhi", "Sustainable, compliant recycling"),
        RecyclerCompany("Pro E-Waste Recycling", "Faridabad", "Newer recycler (since 2020)"),
        RecyclerCompany("Hulladek Recycling", "Kolkata", "Authorized under 2022 rules"),
        RecyclerCompany("Star E Processors", "Mumbai / Raipur", "CPCB-authorized full-service recycler"),
        RecyclerCompany("Elxion", "Bengaluru", "ISO- & KSPCB-licensed, full services"),
        RecyclerCompany("EWRI (E-Waste Recyclers India)", "Delhi/NCR", "Mechanical recycling specialists"),
        RecyclerCompany("Bharat E Waste Recycling Co.", "Mumbai / Nationwide", "End-to-end ethical recycling"),
        RecyclerCompany("Eco-Tech Recycling", "Mumbai", "Prominent Mumbai-based recycler"),
        RecyclerCompany("Green India E-Waste Recycling", "Pan-India", "Compliance-supported recycler")
    )


    // 🔍 Filter + Search
    val filteredCompanies = companies.filter { company ->
        val matchesQuery = company.name.contains(query, true) || company.location.contains(query, true)
        val matchesFilter = when (filter) {
            "Authorized" -> !company.notes.contains("expired", true)
            "Expired" -> company.notes.contains("expired", true)
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by name or city") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )

        // Filter dropdown
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Filter: $filter")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = { filter = "All"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Authorized") },
                        onClick = { filter = "Authorized"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Expired") },
                        onClick = { filter = "Expired"; expanded = false }
                    )
                }
            }
        }

        // Company list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredCompanies) { company ->
                CompanyCard(company) {
                    // Navigate to Booking screen
                    val intent = Intent(context, BookingActivity::class.java)
                    intent.putExtra("companyName", company.name)
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun CompanyCard(company: RecyclerCompany, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = company.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = company.location, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = company.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
