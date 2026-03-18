package com.arcides.mementoapp.presentation.auth

import android.os.Bundle
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
import com.arcides.mementoapp.databinding.FragmentResetPasswordBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        binding.updateButton.setOnClickListener {
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (password.length < 6) {
                binding.passwordInputLayout.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.confirmPasswordInputLayout.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            // Para resetear password vía Supabase Auth link, usamos updateProfile o una función específica
            // Supabase permite actualizar el password si hay una sesión activa (que el link crea)
            viewModel.changePassword("", password, confirmPassword)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collectLatest { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.updateButton.isEnabled = false
                        }
                        is AuthViewModel.AuthState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Snackbar.make(binding.root, "Contraseña actualizada. Inicia sesión.", Snackbar.LENGTH_LONG).show()
                            findNavController().navigate(R.id.loginFragment)
                            viewModel.resetState()
                        }
                        is AuthViewModel.AuthState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.updateButton.isEnabled = true
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                        else -> {
                            binding.progressBar.visibility = View.GONE
                            binding.updateButton.isEnabled = true
                        }
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
