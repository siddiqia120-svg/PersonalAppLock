package com.security.personallock.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.security.personallock.data.AppLockPreferences
import com.security.personallock.databinding.ItemAppLockBinding

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isLocked: Boolean
)

class AppListAdapter(
    private var apps: List<AppItem>,
    private val prefs: AppLockPreferences,
    private val onLockChanged: (AppItem, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(val binding: ItemAppLockBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppLockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvItemName.text = app.name
        holder.binding.tvItemPackage.text = app.packageName
        holder.binding.ivItemIcon.setImageDrawable(app.icon)

        holder.binding.switchLock.setOnCheckedChangeListener(null)
        holder.binding.switchLock.isChecked = app.isLocked

        holder.binding.switchLock.setOnCheckedChangeListener { _, isChecked ->
            app.isLocked = isChecked
            prefs.setPackageLocked(app.packageName, isChecked)
            onLockChanged(app, isChecked)
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateData(newApps: List<AppItem>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
