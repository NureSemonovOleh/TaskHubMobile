package com.nuresemonovoleh.taskhub.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nuresemonovoleh.taskhub.data.models.User
import com.nuresemonovoleh.taskhub.data.repos.UserRepository
import kotlinx.coroutines.launch

// LoginViewModel тепер успадковує AndroidViewModel, щоб мати доступ до Application context
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    // Ініціалізуємо UserRepository, передаючи Application context
    private val repository = UserRepository(application.applicationContext)

    // LiveData для результату входу
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    // LiveData для управління станом завантаження (прогрес-бар)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Функція для входу за допомогою email та паролю
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true // Показати прогрес-бар
            val result = repository.login(email, password) // Викликати метод входу з репозиторію
            _loginResult.value = result // Оновити LiveData з результатом
            _isLoading.value = false // Сховати прогрес-бар
        }
    }

    // Функція для очищення даних користувача (виходу з системи)
    fun logout() {
        repository.clearUserData()
        // Можливо, тут потрібно скинути стан LiveData, якщо ви повертаєтеся до екрана логіну
    }
}
