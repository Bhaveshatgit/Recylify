package com.tasha.recyclify.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val mobile: String = "",
    val password: String = "",
    val isBuyer: Boolean = true, // true = buyer, false = seller
    // Buyer fields
    val orgName: String? = null,
    val orgLocation: String? = null,
    // Seller fields
    val firstName: String? = null,
    val lastName: String? = null
)


