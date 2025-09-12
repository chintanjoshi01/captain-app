package com.eresto.captain.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.model.MenuData

class CategoryAdapter(
    private val context: Context,
    private val categories: List<MenuData>,
    private val listener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    interface OnCategoryClickListener {
        fun onCategoryClick(category: MenuData, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_category_tablet, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.textView.text = category.category_name
        holder.itemView.isSelected = (selectedPosition == position)
        if (holder.itemView.isSelected) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.textView.setTextColor(context.resources.getColor(R.color.colorPrimary, null))
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.textView.setTextColor(context.resources.getColor(R.color.color_white, null))
            }
        }

        holder.itemView.setOnClickListener {
            if (selectedPosition == holder.adapterPosition) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            listener.onCategoryClick(category, position)
        }
    }

    fun setSelected(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textView: android.widget.TextView = view.findViewById(R.id.tv_category_name)
    }
}