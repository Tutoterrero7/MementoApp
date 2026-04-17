package com.arcides.mementoapp.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentSettingsBinding
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.presentation.GlobalActionProvider
import com.arcides.mementoapp.presentation.home.HomeViewModel
import com.arcides.mementoapp.utils.TaskDialogHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment(), GlobalActionProvider {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnUpdateProfile.setOnClickListener {
            val newName = binding.etEditName.text.toString()
            viewModel.updateName(newName)
        }

        binding.btnChangePassword.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            viewModel.changePassword(currentPassword, newPassword)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collectLatest { user ->
                        if (user != null) {
                            binding.tvUserName.text = if (user.name.isNotBlank()) user.name else "Sin nombre"
                            binding.tvUserEmail.text = user.email
                            if (binding.etEditName.text.isNullOrBlank()) {
                                binding.etEditName.setText(user.name)
                            }
                        } else {
                            // Si el usuario es nulo, es que se cerró sesión
                            findNavController().navigate(R.id.action_global_loginFragment)
                        }
                    }
                }

                launch {
                    viewModel.loading.collectLatest { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnUpdateProfile.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.message.collectLatest { msg ->
                        msg?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
    }

    override fun onPrimaryActionClicked() {
        showAddTaskDialog()
    }

    private fun showAddTaskDialog() {
        TaskDialogHelper.showAddTaskDialog(
            context = requireContext(),
            fragmentManager = parentFragmentManager,
            dateFormatter = dateFormatter,
            timeFormatter = timeFormatter,
            onCategorySelectRequested = { _, onSelect: (Category?) -> Unit ->
                val categories = homeViewModel.categories.value
                val categoryNames = categories.map { it.name }.toTypedArray()

                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar categoría")
                    .setItems(categoryNames) { _, which ->
                        onSelect(categories[which])
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onTaskCreated = { title: String, description: String, priority: Task.Priority, categoryId: String, dueDate: Date? ->
                homeViewModel.createTask(title, description, priority, categoryId, dueDate)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
