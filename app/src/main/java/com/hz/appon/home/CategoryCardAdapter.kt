package com.hz.appon.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hz.appon.data.model.Category
import com.hz.appon.databinding.ItemCategoryCardBinding

/** Adapter for the home screen category cards. Each card starts a quiz on tap. */
class CategoryCardAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryCardAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemCategoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.textCategoryName.text = category.name
            binding.root.setOnClickListener { onCategoryClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryCardBinding.inflate(
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
