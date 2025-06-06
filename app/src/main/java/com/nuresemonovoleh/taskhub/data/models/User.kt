package com.nuresemonovoleh.taskhub.data.models

data class User(
    val id: String, // Змінено на String для сумісності з PID або UUID
    val name: String,
    val email: String,
    val isVerified: Boolean = false, // Додано поле isVerified з дефолтним значенням
    val role: String = "User" // Додано поле role з дефолтним значенням
)
