package com.tasha.recyclify.data.model

data class User(
    var uid: String = "",          // Changed from 'val' to 'var'
    var email: String = "",
    var mobile: String = "",
    var isBuyer: Boolean = false,  // Changed from 'val' to 'var'
    var orgName: String? = null,
    var orgLocation: String? = null,
    var orgContact: String? = null,
    var firstName: String? = null,
    var lastName: String? = null
) {
    // No-arg constructor for Firestore
    constructor() : this("", "", "", false, null, null, null, null, null)
}

