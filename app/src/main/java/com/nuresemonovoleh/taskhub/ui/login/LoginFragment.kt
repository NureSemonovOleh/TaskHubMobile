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
import com.nuresemonovoleh.taskhub.R // Import for R class

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Введіть email і пароль", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Removed: Google Sign-In Button Listener

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { user ->
                Toast.makeText(requireContext(), "Успішний вхід: ${user.name}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }.onFailure { error ->
                if (error.message?.contains("Email not verified", ignoreCase = true) == true) {
                    Toast.makeText(requireContext(), "Ваш email не підтверджено. Будь ласка, перевірте свою пошту на наявність посилання для підтвердження.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Помилка: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Removed: Observe Google Sign-In result from ViewModel

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonLogin.isEnabled = !isLoading
            binding.buttonGoToRegister.isEnabled = !isLoading
            // Removed: binding.buttonGoogleSignIn.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
