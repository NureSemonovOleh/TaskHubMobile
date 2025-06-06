package com.nuresemonovoleh.taskhub.data.repos

import com.nuresemonovoleh.taskhub.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    // 🔹 Тестова база користувачів (імітація БД)
    private val users = mutableListOf<User>(
        User(id = 1, name = "Oleh", email = "oleh@test.com", password = "1234"),
        User(id = 2, name = "Anna", email = "anna@test.com", password = "qwerty")
    )

    // 🔐 Фейковий логін
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val user = users.find { it.email == email && it.password == password }
        return@withContext if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("Невірна пошта або пароль"))
        }
    }

    // 🆕 Фейкова реєстрація
    suspend fun register(name: String, email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            if (users.any { it.email == email }) {
                return@withContext Result.failure(Exception("Користувач з такою поштою вже існує"))
            }
            val newUser = User(
                id = users.size + 1,
                name = name,
                email = email,
                password = password
            )
            users.add(newUser)
            return@withContext Result.success(newUser)
        }

    // 🔄 Зміна паролю
    suspend fun changePassword(userId: Int, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val user = users.find { it.id == userId }
            return@withContext if (user != null) {
                val index = users.indexOf(user)
                users[index] = user.copy(password = newPassword)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Користувача не знайдено"))
            }
        }
}