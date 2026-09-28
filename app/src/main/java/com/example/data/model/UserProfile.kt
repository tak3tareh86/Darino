package com.example.data.model

import com.example.util.IranianPhoneUtils

/**
 * User Profile model representing an authenticated user session.
 */
data class UserProfile(
    val id: String,
    val username: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val phoneVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val maskedPhoneNumber: String
        get() = if (!phoneNumber.isNullOrBlank()) IranianPhoneUtils.maskPhoneNumber(phoneNumber) else "---"
}
