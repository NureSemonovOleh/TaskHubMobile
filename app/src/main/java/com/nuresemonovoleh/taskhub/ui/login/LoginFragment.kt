package com.nuresemonovoleh.taskhub.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nuresemonovoleh.taskhub.databinding.FragmentLoginBinding
import com.nuresemonovoleh.taskhub.R // Імпорт R class

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Використовуємо by viewModels() для отримання екземпляра ViewModel
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Обробник натискання кнопки "Увійти"
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Відображаємо прогрес-бар
                binding.progressBar.visibility = View.VISIBLE
                viewModel.login(email, password) // Викликаємо метод входу у ViewModel
            } else {
                Toast.makeText(requireContext(), "Введіть email і пароль", Toast.LENGTH_SHORT).show()
            }
        }

        // Обробник натискання кнопки "Перейти до реєстрації"
        binding.buttonGoToRegister.setOnClickListener {
            // Переходимо до фрагмента реєстрації за допомогою NavController
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Спостерігаємо за результатом входу з ViewModel
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE // Ховаємо прогрес-бар
            result.onSuccess { user ->
                // Успішний вхід: показуємо повідомлення та переходимо на головний екран
                Toast.makeText(requireContext(), "Успішний вхід: ${user.name}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment) // Перехід на HomeFragment
            }.onFailure { error ->
                // Помилка входу: показуємо повідомлення про помилку
                if (error.message?.contains("Email not verified", ignoreCase = true) == true) {
                    Toast.makeText(requireContext(), "Ваш email не підтверджено. Будь ласка, перевірте свою пошту на наявність посилання для підтвердження.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Помилка: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Спостерігаємо за станом завантаження (для прогрес-бара та стану кнопок)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonLogin.isEnabled = !isLoading
            binding.buttonGoToRegister.isEnabled = !isLoading
            // Якщо у вас була кнопка Google Sign-In, вона б також відключалася тут
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
