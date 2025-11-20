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
    private var items: MutableList<MyItem> = mutableListOf()

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.itemView.findViewById<ImageView>(R.id.item_image).setImageResource(items[position].imageResource)
        holder.itemView.findViewById<TextView>(R.id.item_text).text = items[position].text
    }

    override fun getItemCount() = items.size

    // Metóda pre notifyDataSetChanged()
    fun resetItems(newItems: List<MyItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // Prekreslí celý zoznam
    }

    // Metóda pre notifyItemInserted()
    fun addItem(item: MyItem) {
        items.add(0, item)
        notifyItemInserted(0) // Informuje o pridaní položky na pozícii 0
    }

    // Metóda pre notifyItemRemoved()
    fun removeItem() {
        if (items.isNotEmpty()) {
            items.removeAt(0)
            notifyItemRemoved(0) // Informuje o odstránení položky na pozícii 0
        }
    }

    // Metóda pre notifyItemChanged()
    fun changeFirstItem() {
        if (items.isNotEmpty()) {
            val updatedItem = items[0].copy(text = "Zmenený text! (${System.currentTimeMillis()})")
            items[0] = updatedItem
            notifyItemChanged(0) // Informuje o zmene položky na pozícii 0
        }
    }

    // Metóda pre notifyItemRangeChanged()
    fun changeRangeOfItems() {
        if (items.size >= 2) {
            items[0] = items[0].copy(text = "Zmenený rozsah 1")
            items[1] = items[1].copy(text = "Zmenený rozsah 2")
            notifyItemRangeChanged(0, 2) // Informuje o zmene 2 položiek od pozície 0
        }
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

