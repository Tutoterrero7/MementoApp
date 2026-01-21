// presentation/categories/CategoriesAdapter.kt
package com.arcides.mementoapp.presentation.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcides.mementoapp.databinding.ItemCategoryBinding
import com.arcides.mementoapp.domain.models.Category

class CategoriesAdapter(
    private val onCategoryClick: (Category) -> Unit,
    private val onCategoryEdit: (Category) -> Unit,
    private val onCategoryDelete: (Category) -> Unit
) : ListAdapter<Category, CategoriesAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            // Nombre y color
            binding.categoryName.text = category.name
            binding.taskCount.text = "${category.taskCount} tareas"

            // Color de fondo
            try {
                binding.colorIndicator.setBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                binding.colorIndicator.setBackgroundColor(Color.parseColor("#2196F3"))
            }

            // Listeners
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }

            binding.editButton.setOnClickListener {
                onCategoryEdit(category)
            }

            binding.deleteButton.setOnClickListener {
                onCategoryDelete(category)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}