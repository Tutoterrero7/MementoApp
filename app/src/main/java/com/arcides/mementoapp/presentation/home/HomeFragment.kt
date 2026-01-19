package com.arcides.mementoapp.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcides.mementoapp.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var tasksAdapter: TasksAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        // Mostrar email del usuario
        val userEmail = auth.currentUser?.email ?: "Usuario"
        binding.welcomeText.text = "Bienvenido, $userEmail"
    }

    private fun setupUI() {
        // Configurar RecyclerView
        tasksAdapter = TasksAdapter(
            onTaskChecked = { taskId, isChecked ->
                viewModel.toggleTaskStatus(taskId, isChecked)
            },
            onTaskDeleted = { taskId ->
                showDeleteConfirmation(taskId)
            }
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTasks()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Botón para añadir tarea
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasks.collectLatest { tasks ->
                        tasksAdapter.submitList(tasks)
                        binding.tasksRecyclerView.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.message.collectLatest { message ->
                        message?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearMessage()
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun showAddTaskDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Título de la tarea"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Tarea")
            .setMessage("¿Qué necesitas recordar?")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.createTask(title)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(taskId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de eliminar esta tarea?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteTask(taskId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}