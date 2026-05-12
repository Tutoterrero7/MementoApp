package com.arcides.mementoapp.presentation.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentStatsBinding
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.presentation.GlobalActionProvider
import com.arcides.mementoapp.presentation.home.HomeViewModel
import com.arcides.mementoapp.utils.TaskDialogHelper
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class StatsFragment : Fragment(), GlobalActionProvider {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by activityViewModels()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var cachedCategories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adjustBottomPadding()
        observeStats()
    }

    private fun adjustBottomPadding() {
        // Esperamos a que la vista esté dibujada para leer alturas reales
        binding.statsContentLayout.post {
            val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
            val bottomNavHeight = bottomNav?.height ?: 0

            // Barra de navegación del sistema (gestos o botones)
            val insets = ViewCompat.getRootWindowInsets(requireView())
            val systemNavBar = insets
                ?.getInsets(WindowInsetsCompat.Type.systemBars())
                ?.bottom ?: 0

            // El fragment ya llega hasta debajo del bottomNav (están solapados),
            // así que el padding necesario = altura del bottomNav + nav system bar + 24dp extra
            val extraDp = (24 * resources.displayMetrics.density).toInt()
            val totalPadding = bottomNavHeight + systemNavBar + extraDp

            binding.statsContentLayout.setPadding(
                binding.statsContentLayout.paddingLeft,
                binding.statsContentLayout.paddingTop,
                binding.statsContentLayout.paddingRight,
                totalPadding
            )
        }
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collectLatest { categories ->
                        cachedCategories = categories
                    }
                }
                launch {
                    viewModel.uiState.collectLatest { state ->
                        if (state is HomeViewModel.HomeUiState.Success) {
                            updateUI(state.tasks)
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(tasks: List<Task>) {
        val total = tasks.size
        val completed = tasks.count { it.status == Task.TaskStatus.COMPLETED }
        val pending = tasks.count { it.status == Task.TaskStatus.PENDING }
        val inProgress = tasks.count { it.status == Task.TaskStatus.IN_PROGRESS }

        // ── Resumen principal ──
        binding.totalTasksText.text = total.toString()
        binding.completedTasksText.text = completed.toString()
        binding.pendingTasksText.text = pending.toString()
        binding.inProgressTasksText.text = inProgress.toString()
        binding.completedTasksSummaryText.text = completed.toString()

        val progress = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
        binding.circularProgress.setProgress(progress, true)
        binding.completionPercentageText.text = "$progress%"

        // ── Prioridad ──
        val high = tasks.count { it.priority == Task.Priority.HIGH }
        val medium = tasks.count { it.priority == Task.Priority.MEDIUM }
        val low = tasks.count { it.priority == Task.Priority.LOW }
        val maxPriority = maxOf(high, medium, low, 1)

        binding.highPriorityCountText.text = high.toString()
        binding.mediumPriorityCountText.text = medium.toString()
        binding.lowPriorityCountText.text = low.toString()
        binding.highPriorityBar.setProgressCompat((high * 100 / maxPriority), true)
        binding.mediumPriorityBar.setProgressCompat((medium * 100 / maxPriority), true)
        binding.lowPriorityBar.setProgressCompat((low * 100 / maxPriority), true)

        // ── Fechas límite ──
        val now = Date()
        val withDeadline = tasks.filter { it.dueDate != null && it.status != Task.TaskStatus.COMPLETED }
        val overdue = withDeadline.count { it.dueDate!!.before(now) }
        val onTime = withDeadline.count { !it.dueDate!!.before(now) }
        val noDeadline = tasks.count { it.dueDate == null }

        binding.overdueTasksText.text = overdue.toString()
        binding.onTimeTasksText.text = onTime.toString()
        binding.noDeadlineTasksText.text = noDeadline.toString()

        // ── Por categoría ──
        updateCategoryStats(tasks)
    }

    private fun updateCategoryStats(tasks: List<Task>) {
        val container = binding.categoryStatsContainer
        val noCatsText = binding.noCategoriesText

        // Eliminar filas anteriores (mantener el TextView de "sin categorías")
        val childCount = container.childCount
        if (childCount > 1) {
            container.removeViews(1, childCount - 1)
        }

        val categories = cachedCategories
        if (categories.isEmpty()) {
            noCatsText.visibility = View.VISIBLE
            return
        }
        noCatsText.visibility = View.GONE

        val totalTasks = tasks.size.coerceAtLeast(1)

        categories.forEach { category ->
            val categoryTasks = tasks.filter { it.categoryId == category.id }
            val count = categoryTasks.size
            val completedCount = categoryTasks.count { it.status == Task.TaskStatus.COMPLETED }
            val percent = (count * 100 / totalTasks)

            // Contenedor de fila
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(14) }
            }

            // Fila superior: nombre + conteo
            val headerRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(6) }
            }

            val catDot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).also {
                    it.marginEnd = dpToPx(8)
                    it.topMargin = dpToPx(3)
                }
                background = androidx.core.content.res.ResourcesCompat.getDrawable(
                    resources, R.drawable.circle_shape, null
                )
                try {
                    background.setTint(Color.parseColor(category.color))
                } catch (e: Exception) {
                    background.setTint(ContextCompat.getColor(requireContext(), R.color.purple_500))
                }
            }

            val nameText = TextView(requireContext()).apply {
                text = category.name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            }

            val countText = TextView(requireContext()).apply {
                text = "$completedCount/$count"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            headerRow.addView(catDot)
            headerRow.addView(nameText)
            headerRow.addView(countText)

            // Barra de progreso
            val bar = LinearProgressIndicator(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                isIndeterminate = false
                max = 100
                try {
                    setIndicatorColor(Color.parseColor(category.color))
                } catch (e: Exception) {
                    setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
                }
                trackCornerRadius = dpToPx(4)
                setProgressCompat(percent, true)
            }

            rowLayout.addView(headerRow)
            rowLayout.addView(bar)
            container.addView(rowLayout)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

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
                if (cachedCategories.isEmpty()) {
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, "No hay categorías. Créalas en Inicio → Categorías", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
                    return@showAddTaskDialog
                }
                val categoryNames = cachedCategories.map { it.name }.toTypedArray()
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar categoría")
                    .setItems(categoryNames) { _, which -> onSelect(cachedCategories[which]) }
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