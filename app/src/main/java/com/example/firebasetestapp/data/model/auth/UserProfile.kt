package com.example.firebasetestapp.data.model.auth

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    @ServerTimestamp val createdAt: Date? = null
)