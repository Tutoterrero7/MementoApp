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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentCategoriesBinding
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.presentation.GlobalActionProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoriesFragment : Fragment(), GlobalActionProvider {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()
    private lateinit var categoriesAdapter: CategoriesAdapter
    private var selectedColorForDialog: String? = null

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
        // SOLUCIÓN: Listener para el botón de retroceso en la Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Configurar RecyclerView
        categoriesAdapter = CategoriesAdapter(
            onCategoryClick = { category ->
                // Por ahora no navegamos, solo mostramos el click
                Snackbar.make(binding.root, "Click en: ${category.name}", Snackbar.LENGTH_SHORT).show()
            },
            onCategoryEdit = { category ->
                showEditCategoryDialog(category)
            },
            onCategoryDelete = { category ->
                showDeleteCategoryDialog(category)
            }
        )

        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoriesAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collectLatest { message ->
                message?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    override fun onPrimaryActionClicked() {
        showCreateCategoryDialog()
    }

    fun showCreateCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_category, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val colorGrid = dialogView.findViewById<RecyclerView>(R.id.colorGrid)
        
        // Configurar grid de colores
        val colors = Category.DEFAULT_COLORS
        selectedColorForDialog = colors.first()
        
        val adapter = ColorAdapter(colors, selectedColorForDialog) { selectedColor ->
            selectedColorForDialog = selectedColor
        }
        
        colorGrid.layoutManager = GridLayoutManager(requireContext(), 5)
        colorGrid.adapter = adapter
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Nueva Categoría")
            .setPositiveButton("Crear") { _, _ ->
                val name = nameInput.text?.toString()?.trim()
                val color = selectedColorForDialog ?: colors.first()
                
                if (!name.isNullOrEmpty()) {
                    viewModel.createCategory(name, color)
                } else {
                    Snackbar.make(binding.root, "El nombre es requerido", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_category, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val colorGrid = dialogView.findViewById<RecyclerView>(R.id.colorGrid)
        
        // Pre-llenar datos
        nameInput.setText(category.name)
        selectedColorForDialog = category.color
        
        // Configurar grid de colores
        val colors = Category.DEFAULT_COLORS
        val adapter = ColorAdapter(colors, category.color) { selectedColor ->
            selectedColorForDialog = selectedColor
        }
        
        colorGrid.layoutManager = GridLayoutManager(requireContext(), 5)
        colorGrid.adapter = adapter
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Editar Categoría")
            .setPositiveButton("Guardar") { _, _ ->
                val name = nameInput.text?.toString()?.trim()
                val color = selectedColorForDialog ?: category.color
                
                if (!name.isNullOrEmpty()) {
                    val updatedCategory = category.copy(name = name, color = color)
                    viewModel.updateCategory(updatedCategory)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
