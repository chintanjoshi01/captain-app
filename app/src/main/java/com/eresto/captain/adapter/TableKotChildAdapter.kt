package com.eresto.captain.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.databinding.ItemViewkotItemsBinding
import com.eresto.captain.model.CartItemRow
import com.eresto.captain.model.Item
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.Utils
import java.util.*

class TableKotChildAdapter(
    var context: Context,
    var menuList: List<Item>,
    var setOnItemClick: SetOnItemClick?
) : RecyclerView.Adapter<TableKotChildAdapter.ViewHolder>(),
    Filterable {
    var menuFilterList = ArrayList<Item>()

    init {
        menuFilterList = menuList as ArrayList<Item>
    }

    inner class ViewHolder(val binding: ItemViewkotItemsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemViewkotItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = menuFilterList.size

    override fun onBindViewHolder(holder: TableKotChildAdapter.ViewHolder, position: Int) {
        val row = menuFilterList[position]
        holder.binding.textItemName.text = row.item_name
        if (row.isChecked) {
            holder.binding.imgMinus.visibility = View.VISIBLE
            holder.binding.count.visibility = View.VISIBLE
        } else {
            holder.binding.imgMinus.visibility = View.GONE
            holder.binding.count.visibility = View.GONE
        }
        holder.binding.count.text = row.count.toString()
        if (row.is_fsi == 1) {
            holder.binding.imageStar.visibility = View.VISIBLE
        } else {
            holder.binding.imageStar.visibility = View.GONE
        }
        if (row.is_nonveg == 1) {
            holder.binding.textItemName.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_symbol_nonveg,
                0,
                0,
                0
            )
        } else {
            holder.binding.textItemName.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_symbol_veg,
                0,
                0,
                0
            )
        }
        holder.binding.imgPlus.setOnClickListener {
            holder.binding.imgMinus.visibility = View.VISIBLE
            holder.binding.count.visibility = View.VISIBLE
            row.count = row.count + 1
            notifyItemChanged(position)
            setOnItemClick!!.onItemUpdate(position, row, row.count)
        }
        holder.binding.imgMinus.setOnClickListener {
            row.count = row.count - 1
            notifyItemChanged(position)
            if (row.count > 0) {
                setOnItemClick!!.onItemUpdate(position, row, row.count)
            } else {
                row.isChecked = false
                setOnItemClick!!.onItemDelete(position, row, row.count)
            }
        }
        holder.binding.cbCat.visibility = View.GONE
        holder.binding.llAdd.visibility = View.VISIBLE
        holder.binding.llAdd.setOnClickListener {
            setOnItemClick!!.onItemClicked(position, row)
        }
        holder.binding.llView.setOnClickListener {
            setOnItemClick!!.onItemClicked(position, row)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return Utils.ITEMS
    }

    fun refreshList(tableId: Int, position: Int) {
        val db = DBHelper(context)
        val tableItemList: List<CartItemRow> = db.GetCartItems(tableId)
        if (position == -1) {
            for (item in menuList) {
                item.isChecked = false
                item.count = 0
                item.kot_ncv = 0
                item.localId = 0
                item.local_sp_inst = ""
            }
        }
        for (table in tableItemList) {
            for (item in menuList) {
                if (table.id == item.id) {
                    item.isChecked = true
                    item.count = table.qty
                    item.kot_ncv = table.kot_ncv
                    item.localId = table.id
                    item.local_sp_inst = table.notes
                }
            }
        }

        if (position == -1) {
            notifyDataSetChanged()
        } else {
            notifyItemChanged(position)
        }
    }

    fun setItemList(list: List<Item>) {
        this.menuList = list
        this.menuFilterList = menuList as ArrayList<Item>
        notifyDataSetChanged()
    }

    interface SetOnItemClick {
        fun onItemClicked(position: Int, item: Item)
        fun onItemUpdate(position: Int, item: Item, values: Int)
        fun onItemDelete(position: Int, item: Item, values: Int)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                menuFilterList = if (charSearch.isEmpty()) {
                    menuList as ArrayList<Item>
                } else {
                    val resultList = ArrayList<Item>()
                    for (row in menuList) {
                        if (row.item_name.lowercase(Locale.ROOT)
                                .contains(charSearch.lowercase(Locale.ROOT)) || (row.item_short_name
                                ?: "").lowercase(Locale.ROOT)
                                .contains(charSearch.lowercase(Locale.ROOT))
                        ) {
                            resultList.add(row)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = menuFilterList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                menuFilterList = results?.values as ArrayList<Item>
                notifyDataSetChanged()
            }
        }
    }
}