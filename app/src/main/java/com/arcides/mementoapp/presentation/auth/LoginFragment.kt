package com.arcides.mementoapp.presentation.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            if (viewModel.isFormValid) {
                viewModel.login()
            } else {
                Snackbar.make(binding.root, "Por favor, revisa los datos ingresados", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.registerButton.setOnClickListener {
            if (viewModel.isFormValid) {
                viewModel.register()
            } else {
                Snackbar.make(binding.root, "Por favor, revisa los datos ingresados", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.forgotPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }
    }

    private fun showResetPasswordDialog() {
        val container = FrameLayout(requireContext())
        val input = EditText(requireContext()).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 40
                marginEnd = 40
                topMargin = 20
            }
            layoutParams = lp
        }
        container.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recuperar contraseña")
            .setMessage("Ingresa tu email para recibir un enlace de recuperación")
            .setView(container)
            .setPositiveButton("Enviar") { _, _ ->
                val email = input.text.toString().trim()
                viewModel.resetPassword(email)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.email = binding.emailEditText.text.toString().trim()
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
                            val snackbar = Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT)
                            val params = snackbar.view.layoutParams as FrameLayout.LayoutParams
                            params.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                            params.topMargin = 100 // Ajuste para que no esté pegado al borde superior
                            snackbar.view.layoutParams = params
                            snackbar.show()

                            if (state.message.contains("Login") || state.message.contains("exit")) {
                                navigateToHome()
                                viewModel.resetState()
                            }
                        }

                        is AuthViewModel.AuthState.Error -> {
                            showLoading(false)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            viewModel.resetState()
                        }

                        is AuthViewModel.AuthState.Idle -> {
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
        binding.emailEditText.isEnabled = !loading
        binding.passwordEditText.isEnabled = !loading
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