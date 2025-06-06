package com.nuresemonovoleh.taskhub.data.repos

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nuresemonovoleh.taskhub.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class UserRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val baseUrl = "https://taskhub.linerds.us/api"
    private val json = "application/json; charset=utf-8".toMediaType()

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    private object PrefKeys {
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email"
        const val USER_IS_VERIFIED = "user_is_verified"
        // const val USER_ROLE = "user_role" // Видалено
        const val IS_LOGGED_IN = "is_logged_in"
    }

    private fun saveUser(user: User) {
        sharedPrefs.edit().apply {
            putString(PrefKeys.USER_ID, user.id)
            putString(PrefKeys.USER_NAME, user.name)
            putString(PrefKeys.USER_EMAIL, user.email)
            putBoolean(PrefKeys.USER_IS_VERIFIED, user.isVerified)
            // remove(PrefKeys.USER_ROLE) // Видаляємо старе поле, якщо воно було
            putBoolean(PrefKeys.IS_LOGGED_IN, true)
            apply()
        }
        Log.d("UserRepository", "User data saved to SharedPreferences: ${user.email}")
    }

    fun getSavedUser(): User? {
        if (!sharedPrefs.getBoolean(PrefKeys.IS_LOGGED_IN, false)) {
            return null
        }
        val id = sharedPrefs.getString(PrefKeys.USER_ID, null)
        val name = sharedPrefs.getString(PrefKeys.USER_NAME, null)
        val email = sharedPrefs.getString(PrefKeys.USER_EMAIL, null)
        val isVerified = sharedPrefs.getBoolean(PrefKeys.USER_IS_VERIFIED, false)
        // val role = sharedPrefs.getString(PrefKeys.USER_ROLE, "User") // Видалено
        return if (id != null && name != null && email != null) {
            User(id, name, email, isVerified) // Видалено 'role'
        } else {
            null
        }
    }

    fun clearUserData() {
        sharedPrefs.edit().clear().apply()
        Log.d("UserRepository", "User data cleared from SharedPreferences.")
    }

    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        var finalResult: Result<User>
        try {
            val url = "$baseUrl/auth/login"
            val requestBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody(json))
                .addHeader("Content-Type", "application/json")
                .build()
            val response = client.newCall(request).execute()
            Log.d("UserRepository", "Login Response Code: ${response.code}")
            val responseBodyString = response.body?.string()
            Log.d("UserRepository", "Login Raw Response Body: ${responseBodyString}")

            if (response.isSuccessful) {
                if (responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true) {
                    Log.e("UserRepository", "Login failed: Server returned HTML (200 OK) instead of JSON.")
                    finalResult = Result.failure(Exception("Login failed: Server returned HTML (200 OK) instead of JSON. This indicates a server-side configuration issue."))
                } else {
                    finalResult = responseBodyString?.let {
                        try {
                            val jsonResponse = JSONObject(it)
                            val pid = jsonResponse.getString("pid")
                            val name = jsonResponse.getString("name")
                            val isVerified = jsonResponse.optBoolean("is_verified", false)
                            // val role = jsonResponse.optString("role", "User") // Видалено
                            val user = User(id = pid, name = name, email = email, isVerified = isVerified) // Видалено 'role'
                            saveUser(user)
                            Result.success(user)
                        } catch (e: Exception) {
                            Log.e("UserRepository", "Login JSON Parsing Exception: ${e.message}", e)
                            Result.failure(Exception("Login failed: Invalid JSON response format."))
                        }
                    } ?: Result.failure(Exception("Empty response body for successful login"))
                }
            } else {
                Log.e("UserRepository", "Login Error Response (${response.code}): ${responseBodyString}")
                finalResult = when {
                    response.code == 403 && responseBodyString != null -> {
                        try {
                            val errorJson = JSONObject(responseBodyString)
                            val description = errorJson.optString("description", "Unknown error")
                            if (description == "User email is not verified") {
                                Result.failure(Exception("Email not verified. Please check your email for a verification link."))
                            } else {
                                Result.failure(Exception("Login failed: ${response.code} - $description"))
                            }
                        } catch (jsonE: Exception) {
                            Result.failure(Exception("Login failed: ${response.code} - ${responseBodyString ?: "Unknown error body"}"))
                        }
                    }
                    responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true -> {
                        Result.failure(Exception("Login failed: Server returned HTML error page instead of JSON. Check API endpoint or server status."))
                    }
                    else -> {
                        Result.failure(Exception("Login failed: ${response.code} - ${responseBodyString ?: "Unknown error"}"))
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("UserRepository", "Login Network Exception: ${e.message}", e)
            finalResult = Result.failure(Exception("Network error during login: ${e.message}"))
        } catch (e: Exception) {
            Log.e("UserRepository", "Login General Exception: ${e.message}", e)
            finalResult = Result.failure(e)
        }
        return@withContext finalResult
    }

    private fun getUserInfo(token: String): User {
        val url = "$baseUrl/me"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()
        val response = client.newCall(request).execute()
        Log.d("UserRepository", "getUserInfo Response Code: ${response.code}")
        val responseBodyString = response.body?.string()
        Log.d("UserRepository", "getUserInfo Raw Response Body: ${responseBodyString}")
        if (response.isSuccessful) {
            if (responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true) {
                Log.e("UserRepository", "getUserInfo failed: Server returned HTML (200 OK) instead of JSON.")
                throw Exception("Failed to fetch user info: Server returned HTML (200 OK). This indicates a server-side configuration issue.")
            }
            responseBodyString?.let {
                val json = JSONObject(it)
                return User(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    email = json.getString("email")
                    // 'role' не використовується при отриманні інформації про користувача
                )
            } ?: throw Exception("Empty response body for user info")
        } else {
            Log.e("UserRepository", "getUserInfo Error Response (${response.code}): ${responseBodyString}")
            throw Exception("Failed to fetch user info: ${response.code} - ${responseBodyString ?: "Unknown error"}")
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        var finalResult: Result<User>
        try {
            val url = "$baseUrl/auth/register"
            val requestBody = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody(json))
                .addHeader("Content-Type", "application/json")
                .build()
            val response = client.newCall(request).execute()
            Log.d("UserRepository", "Register Response Code: ${response.code}")
            val responseBodyString = response.body?.string()
            Log.d("UserRepository", "Register Raw Response Body: ${responseBodyString}")

            if (response.isSuccessful || response.code == 201) {
                if (responseBodyString.isNullOrBlank()) {
                    Log.d("UserRepository", "Register Success: Empty response body. Creating User object from input.")
                    val registeredUser = User(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        email = email,
                        isVerified = false
                        // 'role' видалено
                    )
                    saveUser(registeredUser)
                    finalResult = Result.success(registeredUser)
                }
                else if (responseBodyString.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true) {
                    Log.e("UserRepository", "Register failed: Server returned HTML (200 OK) instead of JSON.")
                    finalResult = Result.failure(Exception("Register failed: Server returned HTML (200 OK) instead of JSON. This indicates a server-side configuration issue."))
                }
                else {
                    Log.d("UserRepository", "Register Success Raw Response (unexpectedly not empty): $responseBodyString")
                    finalResult = try {
                        val jsonResponse = JSONObject(responseBodyString)
                        val pid = jsonResponse.getString("pid")
                        val registeredName = jsonResponse.getString("name")
                        val isVerified = jsonResponse.optBoolean("is_verified", false)
                        // val role = jsonResponse.optString("role", "User") // Видалено
                        val registeredUser = User(id = pid, name = registeredName, email = email, isVerified = isVerified) // Видалено 'role'
                        saveUser(registeredUser)
                        Result.success(registeredUser)
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Register JSON Parsing Exception: ${e.message}", e)
                        Result.failure(Exception("Register failed: Invalid JSON response format on success."))
                    }
                }

            } else {
                Log.e("UserRepository", "Register Error Response (${response.code}): ${responseBodyString}")

                finalResult = when {
                    response.code == 409 && responseBodyString != null -> {
                        try {
                            val errorJson = JSONObject(responseBodyString)
                            val description = errorJson.optString("description", "Unknown error")
                            if (description == "Entity already exists") {
                                Result.failure(Exception("Registration failed: An account with this email already exists."))
                            } else {
                                Result.failure(Exception("Registration failed: ${response.code} - $description"))
                            }
                        } catch (jsonE: Exception) {
                            Result.failure(Exception("Registration failed: ${response.code} - ${responseBodyString ?: "Unknown error body"}"))
                        }
                    }
                    responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true -> {
                        Result.failure(Exception("Register failed: Server returned HTML error page instead of JSON. Check API endpoint or server status."))
                    }
                    else -> {
                        Result.failure(Exception("Register failed: ${response.code} - ${responseBodyString ?: "Unknown error"}"))
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("UserRepository", "Register Network Exception: ${e.message}", e)
            finalResult = Result.failure(Exception("Network error during registration: ${e.message}"))
        } catch (e: Exception) {
            Log.e("UserRepository", "Register General Exception: ${e.message}", e)
            finalResult = Result.failure(e)
        }
        return@withContext finalResult
    }
}
