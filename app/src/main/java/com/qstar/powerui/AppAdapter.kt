package com.qstar.powerui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private var items: List<AppItem>,
    private var selectedPackage: String?,
    private val onItemClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPackage: TextView = view.findViewById(R.id.tvPackage)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPackage.text = item.packageName

        if (item.icon != null) {
            holder.ivIcon.setImageDrawable(item.icon)
        } else {
            holder.ivIcon.setImageResource(R.mipmap.ic_launcher)
        }

        val isSelected = item.packageName == selectedPackage
        holder.itemView.isSelected = isSelected
        holder.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AppItem>, newSelectedPackage: String?) {
        items = newItems
        selectedPackage = newSelectedPackage
        notifyDataSetChanged()
    }

    fun setSelected(pkg: String?) {
        selectedPackage = pkg
        notifyDataSetChanged()
    }
}
