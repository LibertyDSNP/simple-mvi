package com.unfinished.simplemvi.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unfinished.simplemvi.example.databinding.ViewSearchResultBinding

class SearchResultAdapter(
    private val searchResults: List<String>,
    private val searchResultSelectedListener: (searchResult: String) -> Unit
): RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ViewSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)).apply {
            this.binding.root.setOnClickListener {
                searchResultSelectedListener(searchResults[this.layoutPosition])
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.text = searchResults[position]
    }

    override fun getItemCount(): Int = searchResults.size

    inner class ViewHolder(val binding: ViewSearchResultBinding): RecyclerView.ViewHolder(binding.root)
}