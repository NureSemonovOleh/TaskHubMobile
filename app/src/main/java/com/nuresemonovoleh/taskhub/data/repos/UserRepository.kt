package com.nuresemonovoleh.taskhub.data.repos

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
import java.util.UUID // Import for generating unique IDs

class UserRepository {

    private val client = OkHttpClient()
    private val baseUrl = "https://taskhub.linerds.us/api"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // --- Login Function ---
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/auth/login"

            val requestBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody(JSON))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            Log.d("UserRepository", "Login Response Code: ${response.code}")

            val responseBodyString = response.body?.string()
            Log.d("UserRepository", "Login Raw Response Body: ${responseBodyString}")

            if (response.isSuccessful) {
                if (responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true) {
                    Log.e("UserRepository", "Login failed: Server returned HTML (200 OK) instead of JSON.")
                    return@withContext Result.failure(Exception("Login failed: Server returned HTML (200 OK) instead of JSON. This indicates a server-side configuration issue."))
                }

                responseBodyString?.let {
                    val jsonResponse = JSONObject(it)
                    val pid = jsonResponse.getString("pid")
                    val name = jsonResponse.getString("name")
                    val isVerified = jsonResponse.optBoolean("is_verified", false) // Use optBoolean for optional fields
                    val role = jsonResponse.optString("role", "User") // Use optString for optional fields

                    val user = User(
                        id = pid, // Using pid directly as String ID
                        name = name,
                        email = email, // Email is from input, not response
                        isVerified = isVerified,
                        role = role
                    )
                    Result.success(user)

                } ?: Result.failure(Exception("Empty response body for successful login"))
            } else {
                Log.e("UserRepository", "Login Error Response (${response.code}): ${responseBodyString}")

                if (response.code == 403 && responseBodyString != null) {
                    try {
                        val errorJson = JSONObject(responseBodyString)
                        val description = errorJson.optString("description", "Unknown error")
                        if (description == "User email is not verified") {
                            Result.failure(Exception("Email not verified. Please check your email for a verification link."))
                        } else {
                            Result.failure(Exception("Login failed: ${response.code} - $description"))
                        }
                    } catch (jsonE: Exception) {
                        Result.failure(Exception("Login failed: ${response.code} - ${responseBodyString ?: response.message}"))
                    }
                }
                else if (responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true) {
                    Result.failure(Exception("Login failed: Server returned HTML error page instead of JSON. Check API endpoint or server status."))
                } else {
                    Result.failure(Exception("Login failed: ${response.code} - ${responseBodyString ?: response.message}"))
                }
            }
        } catch (e: IOException) {
            Log.e("UserRepository", "Login Network Exception: ${e.message}", e)
            Result.failure(Exception("Network error during login: ${e.message}"))
        } catch (e: Exception) {
            Log.e("UserRepository", "Login General Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Function to get user information using a token ---
    private fun getUserInfo(token: String): User {
        // NOTE: This function's usage is problematic if the /login endpoint
        // does not provide a 'token' in its response.
        // If other API endpoints require a 'token', you'll need to clarify with the API provider
        // how to obtain it after login, as the /login endpoint currently doesn't return one.
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
                    id = json.getString("id"), // Assuming 'id' field is present and String
                    name = json.getString("name"),
                    email = json.getString("email")
                    // If /me returns isVerified or role, add them here too
                    // isVerified = json.optBoolean("is_verified", false),
                    // role = json.optString("role", "User")
                )
            } ?: throw Exception("Empty response body for user info")
        } else {
            Log.e("UserRepository", "getUserInfo Error Response (${response.code}): ${responseBodyString}")
            throw Exception("Failed to fetch user info: ${response.code} - ${responseBodyString ?: response.message}")
        }
    }

    // --- Register Function ---
    suspend fun register(name: String, email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/auth/register"
            val requestBody = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody(JSON))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UserRepository", "Register Response Code: ${response.code}")
            val responseBodyString = response.body?.string() // Read body once
            Log.d("UserRepository", "Register Raw Response Body: ${responseBodyString}")

            if (response.isSuccessful || response.code == 201) { // Check for 200 OK or 201 Created
                // IMPORTANT: Handle empty response body for successful registration
                if (responseBodyString.isNullOrBlank()) {
                    Log.d("UserRepository", "Register Success: Empty response body. Creating User object from input.")
                    // Construct User object from input parameters since API returns no body
                    val registeredUser = User(
                        id = UUID.randomUUID().toString(), // Generate a unique ID for the User object
                        name = name, // Use the name provided in the registration request
                        email = email, // Use the email provided in the registration request
                        isVerified = false, // Default: email needs verification after registration
                        role = "User" // Default role
                    )
                    Result.success(registeredUser)
                }
                // Defensive check for HTML DOCTYPE (unlikely now but good practice)
                else if (responseBodyString.trimStart().startsWith("<!DOCTYPE html>", ignoreCase = true)) {
                    Log.e("UserRepository", "Register failed: Server returned HTML (200 OK) instead of JSON.")
                    Result.failure(Exception("Register failed: Server returned HTML (200 OK) instead of JSON. This indicates a server-side configuration issue."))
                }
                else {
                    // If for some reason a non-empty, non-HTML body is returned on success,
                    // we'll try to parse it. This might happen if API behavior changes.
                    Log.d("UserRepository", "Register Success Raw Response (unexpectedly not empty): $responseBodyString")
                    val jsonResponse = JSONObject(responseBodyString)
                    val pid = jsonResponse.getString("pid")
                    val registeredName = jsonResponse.getString("name")
                    val isVerified = jsonResponse.optBoolean("is_verified", false)
                    val role = jsonResponse.optString("role", "User")
                    val registeredUser = User(
                        id = pid,
                        name = registeredName,
                        email = email, // Email from input, as not always in response for register
                        isVerified = isVerified,
                        role = role
                    )
                    Result.success(registeredUser)
                }

            } else {
                Log.e("UserRepository", "Register Error Response (${response.code}): ${responseBodyString}")

                if (response.code == 409 && responseBodyString != null) {
                    try {
                        val errorJson = JSONObject(responseBodyString)
                        val description = errorJson.optString("description", "Unknown error")
                        if (description == "Entity already exists") {
                            Result.failure(Exception("Registration failed: An account with this email already exists."))
                        } else {
                            Result.failure(Exception("Registration failed: ${response.code} - $description"))
                        }
                    } catch (jsonE: Exception) {
                        Result.failure(Exception("Registration failed: ${response.code} - ${responseBodyString ?: response.message}"))
                    }
                }
                else if (responseBodyString?.trimStart()?.startsWith("<!DOCTYPE html>", ignoreCase = true) == true) {
                    Result.failure(Exception("Register failed: Server returned HTML error page instead of JSON. Check API endpoint or server status."))
                } else {
                    Result.failure(Exception("Register failed: ${response.code} - ${responseBodyString ?: response.message}"))
                }
            }
        } catch (e: IOException) {
            Log.e("UserRepository", "Register Network Exception: ${e.message}", e)
            Result.failure(Exception("Network error during registration: ${e.message}"))
        } catch (e: Exception) {
            Log.e("UserRepository", "Register General Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
