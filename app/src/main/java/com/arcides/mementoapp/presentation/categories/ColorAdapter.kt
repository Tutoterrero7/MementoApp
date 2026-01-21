package com.arcides.mementoapp.presentation.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arcides.mementoapp.R

class ColorAdapter(
    private val colors: List<String>,
    private var selectedColor: String? = null,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_selection, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val colorCircle: View = view.findViewById(R.id.colorCircle)
        private val selectedIndicator: View = view.findViewById(R.id.selectedIndicator)

        fun bind(colorHex: String) {
            colorCircle.setBackgroundColor(Color.parseColor(colorHex))
            
            selectedIndicator.visibility = if (colorHex == selectedColor) {
                View.VISIBLE
            } else {
                View.GONE
            }

            itemView.setOnClickListener {
                val previousSelected = selectedColor
                selectedColor = colorHex
                onColorSelected(colorHex)
                
                // Refrescar para mostrar el indicador de selección
                notifyDataSetChanged() 
            }
        }
    }
}