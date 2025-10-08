package com.tasha.recyclify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasha.recyclify.ui.theme.RecyclifyTheme

// ------------------ DATA MODEL ------------------
data class Company(
    val id: String = "",
    val companyName: String = "",
    val location: String = "",
    val contactNumber: String = "",
    val wasteTypesAccepted: List<String> = emptyList(),
    val pricePerKg: String = "",
    val description: String = "",
    val buyerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

// ------------------ MAIN ACTIVITY ------------------
class CompanyManagementActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecyclifyTheme {
                var showCreateDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("My Requirements") },
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
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showCreateDialog = true },
                            containerColor = Color(0xFF4CAF50)
                        ) {
                            Icon(Icons.Default.Add, "Add Company", tint = Color.White)
                        }
                    },
                    containerColor = Color(0xFFF5F5F5)
                ) { padding ->
                    MyCompaniesScreen(Modifier.padding(padding))
                }

                if (showCreateDialog) {
                    CreateCompanyDialog(
                        onDismiss = { showCreateDialog = false },
                        onCompanyCreated = { showCreateDialog = false }
                    )
                }
            }
        }
    }
}

// ------------------ MY COMPANIES LIST ------------------
@Composable
fun MyCompaniesScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var companies by remember { mutableStateOf(listOf<Company>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            db.collection("companies")
                .whereEqualTo("buyerId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        isLoading = false
                        return@addSnapshotListener
                    }
                    val data = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Company::class.java)?.copy(id = doc.id)
                    }?.sortedByDescending { it.createdAt } ?: emptyList()
                    companies = data
                    isLoading = false
                }
        }
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
    } else if (companies.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No companies yet", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text("Tap + to create your first company", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(companies) { company ->
                CompanyCard(company, db)
            }
        }
    }
}

@Composable
fun CompanyCard(company: Company, db: FirebaseFirestore) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = company.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(company.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                Surface(
                    color = if (company.isActive) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (company.isActive) "Active" else "Inactive",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (company.isActive) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(Icons.Default.Phone, "Contact", company.contactNumber)
            InfoRow(Icons.Default.AttachMoney, "Price", "₹${company.pricePerKg}/kg")

            if (company.wasteTypesAccepted.isNotEmpty()) {
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Recycling, null, Modifier.size(20.dp), tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Waste Types", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            company.wasteTypesAccepted.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (company.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Description, null, Modifier.size(20.dp), tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Description", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            company.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = {
                        db.collection("companies").document(company.id)
                            .update("isActive", !company.isActive)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (company.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (company.isActive) "Pause" else "Activate")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showEditDialog) {
        EditCompanyDialog(
            company = company,
            onDismiss = { showEditDialog = false },
            onCompanyUpdated = { showEditDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFF44336)) },
            title = { Text("Delete Company?") },
            text = { Text("This will permanently delete ${company.companyName}. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("companies").document(company.id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Company deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), tint = Color(0xFF4CAF50))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

// ------------------ EDIT COMPANY DIALOG ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCompanyDialog(
    company: Company,
    onDismiss: () -> Unit,
    onCompanyUpdated: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scrollState = rememberScrollState()

    // Only editable fields
    var pricePerKg by remember { mutableStateOf(company.pricePerKg) }
    var description by remember { mutableStateOf(company.description) }
    var selectedWasteTypes by remember { mutableStateOf(company.wasteTypesAccepted.toSet()) }
    var isUpdating by remember { mutableStateOf(false) }

    val wasteTypes = listOf(
        "E-Waste", "Wet-Waste", "Dry-Waste", "PET-Waste",
        "Plastic", "Metal", "Paper/Cardboard", "Glass", "Textiles"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                Text(
                    "Edit Company",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Non-editable fields (display only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Company Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow(Icons.Default.Business, "Company Name", company.companyName)
                        InfoRow(Icons.Default.LocationOn, "Location", company.location)
                        InfoRow(Icons.Default.Phone, "Contact Number", company.contactNumber)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "These details are pre-populated from your profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Editable fields
                Text(
                    "Editable Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pricePerKg,
                    onValueChange = { pricePerKg = it },
                    label = { Text("Price per Kg (₹)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF4CAF50)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        focusedLabelColor = Color(0xFF4CAF50)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = Color(0xFF4CAF50)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        focusedLabelColor = Color(0xFF4CAF50)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Waste Types Accepted:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                wasteTypes.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { type ->
                            FilterChip(
                                selected = selectedWasteTypes.contains(type),
                                onClick = {
                                    selectedWasteTypes = if (selectedWasteTypes.contains(type)) {
                                        selectedWasteTypes - type
                                    } else {
                                        selectedWasteTypes + type
                                    }
                                },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4CAF50),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            when {
                                pricePerKg.isBlank() -> Toast.makeText(context, "Enter price per kg", Toast.LENGTH_SHORT).show()
                                selectedWasteTypes.isEmpty() -> Toast.makeText(context, "Select at least one waste type", Toast.LENGTH_SHORT).show()
                                else -> {
                                    isUpdating = true
                                    val updates = hashMapOf<String, Any>(
                                        "pricePerKg" to pricePerKg,
                                        "description" to description,
                                        "wasteTypesAccepted" to selectedWasteTypes.toList()
                                    )

                                    db.collection("companies").document(company.id)
                                        .update(updates)
                                        .addOnSuccessListener {
                                            isUpdating = false
                                            Toast.makeText(context, "Company updated successfully!", Toast.LENGTH_SHORT).show()
                                            onCompanyUpdated()
                                        }
                                        .addOnFailureListener {
                                            isUpdating = false
                                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}

// ------------------ CREATE COMPANY DIALOG ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompanyDialog(
    onDismiss: () -> Unit,
    onCompanyCreated: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scrollState = rememberScrollState()

    // User data from Firebase
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }

    // Form fields
    var companyName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var pricePerKg by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedWasteTypes by remember { mutableStateOf(setOf<String>()) }
    var isCreating by remember { mutableStateOf(false) }

    val wasteTypes = listOf(
        "E-Waste", "Wet-Waste", "Dry-Waste", "PET-Waste",
        "Plastic", "Metal", "Paper/Cardboard", "Glass", "Textiles"
    )

    // Load user data from Firebase
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    userData = document.data
                    // Pre-populate fields from user data
                    companyName = (document.data?.get("orgName") as? String) ?: ""
                    location = (document.data?.get("orgLocation") as? String) ?: ""
                    contactNumber = (document.data?.get("mobile") as? String) ?: ""
                    isLoadingUser = false
                }
                .addOnFailureListener {
                    isLoadingUser = false
                    Toast.makeText(context, "Error loading user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            if (isLoadingUser) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    Text(
                        "Add Stock Requirement",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Pre-populated fields (non-editable)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "From Your Profile",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { },
                                label = { Text("Company Name") },
                                leadingIcon = { Icon(Icons.Default.Business, null, tint = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color.LightGray,
                                    disabledLabelColor = Color.Gray,
                                    disabledLeadingIconColor = Color.Gray,
                                    disabledTextColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = location,
                                onValueChange = { },
                                label = { Text("Location") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color.LightGray,
                                    disabledLabelColor = Color.Gray,
                                    disabledLeadingIconColor = Color.Gray,
                                    disabledTextColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = contactNumber,
                                onValueChange = { },
                                label = { Text("Contact Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color.LightGray,
                                    disabledLabelColor = Color.Gray,
                                    disabledLeadingIconColor = Color.Gray,
                                    disabledTextColor = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Editable fields
                    Text(
                        "Additional Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pricePerKg,
                        onValueChange = { pricePerKg = it },
                        label = { Text("Price per Kg (₹)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Waste Types Accepted:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    wasteTypes.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { type ->
                                FilterChip(
                                    selected = selectedWasteTypes.contains(type),
                                    onClick = {
                                        selectedWasteTypes = if (selectedWasteTypes.contains(type)) {
                                            selectedWasteTypes - type
                                        } else {
                                            selectedWasteTypes + type
                                        }
                                    },
                                    label = { Text(type) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF4CAF50),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isCreating
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                when {
                                    companyName.isBlank() -> Toast.makeText(context, "Company name is required", Toast.LENGTH_SHORT).show()
                                    location.isBlank() -> Toast.makeText(context, "Location is required", Toast.LENGTH_SHORT).show()
                                    contactNumber.isBlank() -> Toast.makeText(context, "Contact number is required", Toast.LENGTH_SHORT).show()
                                    pricePerKg.isBlank() -> Toast.makeText(context, "Enter price per kg", Toast.LENGTH_SHORT).show()
                                    selectedWasteTypes.isEmpty() -> Toast.makeText(context, "Select at least one waste type", Toast.LENGTH_SHORT).show()
                                    else -> {
                                        isCreating = true
                                        val company = hashMapOf(
                                            "companyName" to companyName,
                                            "location" to location,
                                            "contactNumber" to contactNumber,
                                            "pricePerKg" to pricePerKg,
                                            "description" to description,
                                            "wasteTypesAccepted" to selectedWasteTypes.toList(),
                                            "buyerId" to auth.currentUser!!.uid,
                                            "createdAt" to System.currentTimeMillis(),
                                            "isActive" to true
                                        )

                                        db.collection("companies").add(company)
                                            .addOnSuccessListener {
                                                isCreating = false
                                                Toast.makeText(context, "Company created successfully!", Toast.LENGTH_SHORT).show()
                                                onCompanyCreated()
                                            }
                                            .addOnFailureListener {
                                                isCreating = false
                                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isCreating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Create")
                            }
                        }
                    }
                }
            }
        }
    }
}