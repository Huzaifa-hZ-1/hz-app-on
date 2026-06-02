package com.hz.appon.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hz.appon.R
import com.hz.appon.data.model.Category
import com.hz.appon.databinding.ItemCategorySelectBinding

/**
 * RecyclerView adapter for the onboarding category selection list.
 *
 * In Android, RecyclerView.Adapter recycles view holders — only the data changes,
 * not the views themselves. ListAdapter uses DiffUtil to compute minimal updates.
 */
class CategoryAdapter(
    private val onToggle: (categoryId: Int) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemCategorySelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.textCategoryName.text = category.name
            binding.checkboxCategory.isChecked = category.isSelected
            binding.root.setOnClickListener { onToggle(category.id) }
            binding.checkboxCategory.setOnClickListener { onToggle(category.id) }

            val bgColor = if (category.isSelected)
                ContextCompat.getColor(binding.root.context, R.color.purple_200)
            else
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            binding.root.setBackgroundColor(bgColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategorySelectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(old: Category, new: Category) = old.id == new.id
        override fun areContentsTheSame(old: Category, new: Category) = old == new
    }
}
