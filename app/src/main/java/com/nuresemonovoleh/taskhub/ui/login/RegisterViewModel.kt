package com.nuresemonovoleh.taskhub.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuresemonovoleh.taskhub.data.models.User
import com.nuresemonovoleh.taskhub.data.repos.UserRepository
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.register(name, email, password)
            _registerResult.value = result
            _isLoading.value = false
        }
    }
}