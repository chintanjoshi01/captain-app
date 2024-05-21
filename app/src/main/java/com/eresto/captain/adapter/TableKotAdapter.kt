package com.eresto.captain.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.databinding.ItemViewkotHeaderBinding
import com.eresto.captain.model.CartItemRow
import com.eresto.captain.model.Item
import com.eresto.captain.model.MenuData
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.Utils
import com.eresto.captain.adapter.TableKotChildAdapter

class TableKotAdapter(
    var context: Context,
    var menuList: List<MenuData>,
    var setOnItemClick: SetOnItemClick?
) : RecyclerView.Adapter<TableKotAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemViewkotHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemViewkotHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: TableKotAdapter.ViewHolder, position: Int) {
        val row = menuList[position]

        holder.binding.textCategoryName.text = row.category_name

        holder.binding.recyclerviewItem.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val color = if (row.item_cat_id == -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getColor(R.color.color_light_pink_fade)
            } else {
                context.resources.getColor(R.color.color_light_pink_fade)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getColor(R.color.color_white)
            } else {
                context.resources.getColor(R.color.color_white)
            }
        }

        holder.binding.relItem.setBackgroundColor(color)
        holder.binding.categoryParentContainer.setCardBackgroundColor(color)

        if (!row.items.isNullOrEmpty()) {
            val adapter = TableKotChildAdapter(context, row.items, object :
                TableKotChildAdapter.SetOnItemClick {
                override fun onItemClicked(position: Int, item: Item) {
                    setOnItemClick!!.onItemClicked(holder.adapterPosition, item, row)
                }

                override fun onItemDelete(position: Int, item: Item, values: Int) {
                    setOnItemClick!!.onItemDelete(position, item, row, values)
                }

                override fun onItemUpdate(position: Int, item: Item, values: Int) {
                    setOnItemClick!!.onItemUpdate(holder.adapterPosition, item, row, values)
                }
            })
            holder.binding.recyclerviewItem.adapter = adapter
        }

        holder.binding.categoryParentContainer.setOnClickListener {
            if (row.isExpanded) {
                row.isExpanded = false
                holder.binding.recyclerviewItem.visibility = View.GONE
            } else {
                row.isExpanded = true
                holder.binding.recyclerviewItem.visibility = View.VISIBLE
            }
        }
    }

    fun setItemList(list: List<MenuData>) {
        this.menuList = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return Utils.CATEGORY
    }

    fun refreshList(tableId: Int, position: Int) {
        val db = DBHelper(context)
        val tableItemList: List<CartItemRow> = db.GetCartItems(tableId)
        var mPosition=position
        /**Check all item false*/
        if(mPosition==-1) {
            for (cat in menuList) {
                for (item in cat.items) {
                    item.isChecked = false
                    item.count = 0
                    item.kot_ncv = 0
                    item.localId = 0
                    item.local_sp_inst = ""
                }
            }
        }
        /**Check cart item true*/
        for (cat in menuList) {
            for (table in tableItemList) {
                for (item in cat.items) {
                    if (table.id == item.id) {
                        if(item.item_cat_id==-1){
                            mPosition=-1
                        }
                        item.isChecked = true
                        item.count = table.qty
                        item.kot_ncv = table.kot_ncv
                        item.localId = table.id
                        item.local_sp_inst = table.notes
                    }
                }
            }
        }

        if (mPosition == -1) {
            notifyDataSetChanged()
        } else {
            notifyItemChanged(mPosition)
            setOnItemClick!!.onItemScroll(mPosition)
        }
    }

    interface SetOnItemClick {
        fun onItemClicked(position: Int, item: Item, parent: MenuData)
        fun onItemScroll(position: Int)
        fun onItemUpdate(position: Int, item: Item, parent: MenuData, values: Int)
        fun onItemDelete(position: Int, item: Item, parent: MenuData, values: Int)
    }
}
