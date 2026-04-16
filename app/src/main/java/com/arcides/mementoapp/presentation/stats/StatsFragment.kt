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
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.presentation.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
