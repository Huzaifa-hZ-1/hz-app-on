package com.hz.appon.onboarding

import android.view.LayoutInflater
import android.view.View
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
 * Each item is a card that toggles between selected (primary colour) and
 * unselected (surface colour) states. ListAdapter uses DiffUtil for minimal updates.
 */
class CategoryAdapter(
    private val onToggle: (categoryId: Int) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemCategorySelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            val ctx = binding.root.context
            binding.textCategoryName.text = category.name
            binding.textCheckmark.visibility = if (category.isSelected) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onToggle(category.id) }

            if (category.isSelected) {
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.primary)
                )
                binding.textCategoryName.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                binding.textCheckmark.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.surface)
                )
                binding.textCategoryName.setTextColor(
                    ContextCompat.getColor(ctx, R.color.text_primary)
                )
            }
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
