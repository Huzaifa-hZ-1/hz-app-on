package com.hz.appon.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hz.appon.R
import com.hz.appon.data.model.Category
import com.hz.appon.databinding.ItemCategoryCardBinding

/** Adapter for the home screen category cards. Each card has a unique colour from the palette. */
class CategoryCardAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryCardAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemCategoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, position: Int) {
            binding.textCategoryName.text = category.name
            binding.root.setOnClickListener { onCategoryClick(category) }
            binding.cardCategory.setCardBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    CARD_COLORS[position % CARD_COLORS.size]
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position), position)

    private object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(old: Category, new: Category) = old.id == new.id
        override fun areContentsTheSame(old: Category, new: Category) = old == new
    }

    companion object {
        private val CARD_COLORS = intArrayOf(
            R.color.cat_color_1, R.color.cat_color_2, R.color.cat_color_3,
            R.color.cat_color_4, R.color.cat_color_5, R.color.cat_color_6
        )
    }
}
