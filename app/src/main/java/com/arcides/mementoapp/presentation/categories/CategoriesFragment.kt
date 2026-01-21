// presentation/categories/CategoriesFragment.kt
package com.arcides.mementoapp.presentation.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.arcides.mementoapp.databinding.FragmentCategoriesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()
    private lateinit var categoriesAdapter: CategoriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                android.R.id.home -> {
                    findNavController().navigateUp()
                    true
                }
                else -> false
            }
        }

        // Configurar RecyclerView
        categoriesAdapter = CategoriesAdapter(
            onCategoryClick = { category ->
                // TODO: Filtrar tareas por categoría
            },
            onCategoryEdit = { category ->
                showEditCategoryDialog(category)
            },
            onCategoryDelete = { category ->
                showDeleteCategoryDialog(category)
            }
        )

        binding.categoriesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = categoriesAdapter
        }

        // Botón para añadir categoría
        binding.fabAddCategory.setOnClickListener {
            showCreateCategoryDialog()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                categoriesAdapter.submitList(categories)

                // Mostrar mensaje si no hay categorías
                if (categories.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.categoriesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.categoriesRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.message.collectLatest { message ->
                message?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun showCreateCategoryDialog() {
        val dialog = CreateCategoryDialog { name, color ->
            viewModel.createCategory(name, color)
        }
        dialog.show(parentFragmentManager, "CreateCategoryDialog")
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialog = CreateCategoryDialog(
            initialCategory = category,
            onCreateCategory = { name, color ->
                val updatedCategory = category.copy(name = name, color = color)
                viewModel.updateCategory(updatedCategory)
            }
        )
        dialog.show(parentFragmentManager, "EditCategoryDialog")
    }

    private fun showDeleteCategoryDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar categoría")
            .setMessage("¿Eliminar '${category.name}'? Las tareas no se eliminarán.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteCategory(category.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}