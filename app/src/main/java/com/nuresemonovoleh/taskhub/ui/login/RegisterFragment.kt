package com.nuresemonovoleh.taskhub.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nuresemonovoleh.taskhub.R
import com.nuresemonovoleh.taskhub.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Кнопка реєстрації
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

            if (validateRegisterInput(name, email, password, confirmPassword)) {
                viewModel.register(name, email, password)
            }
        }

        // Повернутися до логіну
        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateRegisterInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                binding.editTextName.error = "Введіть ім'я"
                false
            }
            email.isEmpty() -> {
                binding.editTextEmail.error = "Введіть email"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.editTextEmail.error = "Введіть правильний email"
                false
            }
            password.isEmpty() -> {
                binding.editTextPassword.error = "Введіть пароль"
                false
            }
            password.length < 4 -> {
                binding.editTextPassword.error = "Пароль має бути мінімум 4 символи"
                false
            }
            password != confirmPassword -> {
                binding.editTextConfirmPassword.error = "Паролі не співпадають"
                false
            }
            else -> true
        }
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            binding.buttonRegister.isEnabled = true

            if (result.isSuccess) {
                val user = result.getOrNull()
                Toast.makeText(
                    requireContext(),
                    "Реєстрація успішна! Ласкаво просимо, ${user?.name}!",
                    Toast.LENGTH_SHORT
                ).show()
                // Перехід до головного екрана
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Помилка реєстрації",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonRegister.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}