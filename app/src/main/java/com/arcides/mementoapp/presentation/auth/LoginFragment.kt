package com.arcides.mementoapp.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arcides.mementoapp.R

import com.arcides.mementoapp.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Validación en tiempo real (actualizar el modelo al perder el foco)
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.email = binding.emailEditText.text.toString()
        }

        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.password = binding.passwordEditText.text.toString()
        }

        // ACTUALIZACIÓN: Importa core-ktx para usar doOnTextChanged
        binding.emailEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.email = text.toString() // Actualiza el email mientras escribes
            binding.loginButton.isEnabled = viewModel.isFormValid
        }

        binding.passwordEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.password = text.toString() // Actualiza la contraseña mientras escribes
            binding.loginButton.isEnabled = viewModel.isFormValid
        }

        // Botones
        binding.loginButton.setOnClickListener {
            viewModel.login()
        }

        binding.registerButton.setOnClickListener {
            viewModel.register()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthViewModel.AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.loginButton.isEnabled = false
                        binding.registerButton.isEnabled = false
                    }

                    is AuthViewModel.AuthState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                        // TODO: Navegar a la pantalla principal
                    }

                    is AuthViewModel.AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.loginButton.isEnabled = true
                        binding.registerButton.isEnabled = true
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }

                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.loginButton.isEnabled = viewModel.isFormValid
                        binding.registerButton.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}