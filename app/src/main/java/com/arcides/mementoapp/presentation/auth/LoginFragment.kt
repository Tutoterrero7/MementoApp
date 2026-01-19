package com.arcides.mementoapp.presentation.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        setupTextWatchers()
        setupObservers()
    }

    private fun setupViews() {
        binding.loginButton.setOnClickListener {
            // TEMPORAL: Usar datos de prueba
            viewModel.email = "test@test.com"
            viewModel.password = "123456"
            binding.emailEditText.setText("test@test.com")
            binding.passwordEditText.setText("123456")

            viewModel.login()
        }

        binding.registerButton.setOnClickListener {
            viewModel.register()
        }

        binding.forgotPasswordText.setOnClickListener {
            Snackbar.make(binding.root, "Funcionalidad próximamente", Snackbar.LENGTH_SHORT).show()
        }

        // PRUEBA DIAGNÓSTICA - Botón para debug
        binding.loginButton.setOnLongClickListener {
            testFirebaseConnection()
            true
        }
    }

    private fun testFirebaseConnection() {
        Log.d("DEBUG_LOGIN", "=== INICIANDO DIAGNÓSTICO ===")
        
        // 1. Verificar campos
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        Log.d("DEBUG_LOGIN", "Email: $email, Password: ${password.length} chars")
        
        // 2. Verificar Firebase Auth
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            Log.d("DEBUG_LOGIN", "Firebase Auth inicializado: ${auth.app.name}")
            Log.d("DEBUG_LOGIN", "Current user: ${auth.currentUser?.email ?: "Ninguno"}")
        } catch (e: Exception) {
            Log.e("DEBUG_LOGIN", "Error Firebase: ${e.message}")
        }
        
        // 3. Verificar ViewModel
        Log.d("DEBUG_LOGIN", "ViewModel email: ${viewModel.email}")
        Log.d("DEBUG_LOGIN", "ViewModel password length: ${viewModel.password.length}")
        Log.d("DEBUG_LOGIN", "isFormValid: ${viewModel.isFormValid}")
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.email = binding.emailEditText.text.toString()
                viewModel.password = binding.passwordEditText.text.toString()
                binding.loginButton.isEnabled = viewModel.isFormValid
            }
        }
        
        binding.emailEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collectLatest { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Loading -> {
                            showLoading(true)
                        }

                        is AuthViewModel.AuthState.Success -> {
                            showLoading(false)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                            navigateToHome()
                        }

                        is AuthViewModel.AuthState.Error -> {
                            showLoading(false)
                            Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_LONG).show()
                        }

                        else -> {
                            showLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !loading && viewModel.isFormValid
        binding.registerButton.isEnabled = !loading
    }

    private fun navigateToHome() {
        if (findNavController().currentDestination?.id == R.id.loginFragment) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}