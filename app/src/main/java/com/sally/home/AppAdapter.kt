package com.sally.home

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AppInfo(val label: String, val pkg: String, val cn: ComponentName, val icon: Drawable)

class AppAdapter(
    private val showLabel: Boolean,
    private val onClick: (AppInfo) -> Unit,
    private val onMenu: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {
    private var items: List<AppInfo> = emptyList()
    fun submit(l: List<AppInfo>) { items = l; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, i: Int) {
        val a = items[i]
        h.icon.setImageDrawable(a.icon)
        h.label.text = a.label
        h.label.visibility = if (showLabel) View.VISIBLE else View.GONE
        h.itemView.setOnClickListener { onClick(a) }
        h.itemView.setOnLongClickListener { onMenu(a, h.itemView); true }
    }
}
