package com.example.mv11

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

data class MyItem(val id: Int, val imageResource: Int, val text: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MyItem

        if (id != other.id) return false
        if (imageResource != other.imageResource) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + imageResource
        result = 31 * result + text.hashCode()
        return result
    }
}

class FeedAdapter : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {
    private var users: MutableList<UserEntity> = mutableListOf()

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val user = users[position]
        val textView = holder.itemView.findViewById<TextView>(R.id.item_text)
        textView.text = "${user.name}\nUID: ${user.uid}\nLat: ${String.format("%.4f", user.lat)}, Lon: ${String.format("%.4f", user.lon)}"
        holder.itemView.findViewById<ImageView>(R.id.item_image).setImageResource(R.drawable.profile_foreground)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<UserEntity>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}

class MyItemDiffCallback(
    private val oldList: List<MyItem>, 
    private val newList: List<MyItem>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

