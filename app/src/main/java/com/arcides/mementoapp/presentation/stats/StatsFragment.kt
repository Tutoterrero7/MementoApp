package com.arcides.mementoapp.presentation.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arcides.mementoapp.databinding.FragmentStatsBinding
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.presentation.GlobalActionProvider
import com.arcides.mementoapp.presentation.home.HomeViewModel
import com.arcides.mementoapp.utils.TaskDialogHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class StatsFragment : Fragment(), GlobalActionProvider {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeStats()
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    if (state is HomeViewModel.HomeUiState.Success) {
                        updateUI(state.tasks)
                    }
                }
            }
        }
    }

    private fun updateUI(tasks: List<Task>) {
        val total = tasks.size
        val completed = tasks.count { it.status == Task.TaskStatus.COMPLETED }
        val pending = total - completed

        binding.totalTasksText.text = total.toString()
        binding.completedTasksText.text = completed.toString()
        binding.pendingTasksText.text = pending.toString()

        val progress = if (total > 0) (completed.toFloat() / total.toFloat() * 100).toInt() else 0
        binding.circularProgress.setProgress(progress, true)
        binding.completionPercentageText.text = "$progress%"
    }

    override fun onPrimaryActionClicked() {
        showAddTaskDialog()
    }

    private fun showAddTaskDialog() {
        val categories = viewModel.categories.value
        TaskDialogHelper.showAddTaskDialog(
            context = requireContext(),
            fragmentManager = parentFragmentManager,
            dateFormatter = dateFormatter,
            timeFormatter = timeFormatter,
            onCategorySelectRequested = { _, onSelect: (Category?) -> Unit ->
                if (categories.isEmpty()) {
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, "No hay categorías. Créalas en Inicio → Categorías", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
                    return@showAddTaskDialog
                }
                val categoryNames = categories.map { it.name }.toTypedArray()
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar categoría")
                    .setItems(categoryNames) { _, which -> onSelect(categories[which]) }
                    .setNeutralButton("Sin categoría") { _, _ -> onSelect(null) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onTaskCreated = { title, description, priority, categoryId, dueDate ->
                viewModel.createTask(title, description, priority, categoryId, dueDate)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}