package com.eresto.captain.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.databinding.ItemViewKotCategoryBinding
import com.eresto.captain.model.CartItemRow
import org.json.JSONArray
import org.json.JSONException

class ViewKotAdapter(
    var context: Context,
    var menuList: MutableList<CartItemRow>,
    var setOnItemClick: SetOnItemClick?
) : RecyclerView.Adapter<ViewKotAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemViewKotCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemViewKotCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: ViewKotAdapter.ViewHolder, position: Int) {
        val row = menuList[position]
        holder.binding.textItemName.text = row.item_name
        var inst = ""
        if (row.notes != null && row.notes != "null") {
            try {
                val items = JSONArray(row.notes)
                for (i in 0 until items.length()) {
                    inst += items.getString(i) + ","
                }
                if (inst.isNotEmpty()) {
                    inst = inst.substring(0, inst.length - 1)
                }
            } catch (e: JSONException) {
                inst = row.notes.toString()
            }
        }
        holder.binding.textSpInst.text = inst

        holder.binding.tvQty.text = row.qty.toString()

        holder.binding.linMin.setOnClickListener {
            if (row.qty > 1) {
                setOnItemClick!!.itemDecreased(
                    position,
                    row,
                    holder.binding.tvQty
                )
            } else {
                val dialogDelete = Dialog(context, R.style.DialogTheme)
                dialogDelete.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogDelete.setCancelable(false)
                dialogDelete.setContentView(R.layout.dialog_common_filee)
                dialogDelete.show()

                val title = dialogDelete.findViewById(R.id.text_title) as AppCompatTextView
                title.text =
                    context.resources.getString(R.string.are_you_sure_do_you_want_to_delete_this_entry)

                val no = dialogDelete.findViewById(R.id.tv_no) as AppCompatTextView
                no.setOnClickListener {
                    dialogDelete.dismiss()
                }
                val yes = dialogDelete.findViewById(R.id.tv_yes) as AppCompatTextView
                yes.setOnClickListener {
                    if (position < menuList.size) {
                        setOnItemClick!!.onItemDeleted(position, row, holder.binding.tvQty)
                        menuList.removeAt(position)
                    }
                    dialogDelete.dismiss()
                }
            }
        }
        holder.binding.linAdd.setOnClickListener {
            setOnItemClick!!.itemIncreased(position, row, holder.binding.tvQty)
        }
        holder.binding.mainview.setOnClickListener {
            setOnItemClick!!.onItemClick(position, row, holder.binding.tvQty)
        }
    }


    interface SetOnItemClick {
        fun itemIncreased(position: Int, item: CartItemRow, view: AppCompatTextView)
        fun itemDecreased(position: Int, item: CartItemRow, view: AppCompatTextView)
        fun onItemDeleted(position: Int, item: CartItemRow, view: AppCompatTextView)
        fun onItemClick(position: Int, item: CartItemRow, view: AppCompatTextView)
    }
}