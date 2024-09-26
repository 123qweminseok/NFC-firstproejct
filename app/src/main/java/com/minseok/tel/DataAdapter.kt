package com.minseok.tel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

class DataAdapter(
    private var items: List<DataItem>,
    private val onDeleteClickListener: (DataItem) -> Unit
) : RecyclerView.Adapter<DataAdapter.DataViewHolder>() {

    class DataViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.itemTextView)
        val deleteIcon: ImageView = view.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return DataViewHolder(view)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = "${item.id}: ${item.value}"

        holder.deleteIcon.setOnClickListener {
            onDeleteClickListener(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<DataItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
