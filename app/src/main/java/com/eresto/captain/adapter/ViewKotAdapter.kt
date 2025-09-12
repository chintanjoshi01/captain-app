package com.eresto.captain.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
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
    private val context: Context,
    private var menuList: MutableList<CartItemRow>,
    private val setOnItemClick: SetOnItemClick
) : RecyclerView.Adapter<ViewKotAdapter.ViewHolder>() {

    // A single ViewHolder is now sufficient for both layouts.
    inner class ViewHolder(val binding: ItemViewKotCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // We inflate the layout by one name. Android automatically picks the correct
        // version from either `layout/` or `layout-sw600dp/`.
        val binding =
            ItemViewKotCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = menuList[position]
        val binding = holder.binding

        // This binding logic now works for both layouts, as the view IDs are the same.
        binding.textItemName.text = row.item_name
        val instructions = parseNotes(row.notes)
        if (instructions.isNotEmpty()) {
            binding.textSpInst.visibility = View.VISIBLE
            binding.textSpInst.text = instructions
        } else {
            binding.textSpInst.visibility = View.GONE
        }
        binding.tvTotalAmount?.text = (row.item_price * row.qty).toString()

        binding.tvQty.text = row.qty.toString()

        binding.linMin.setOnClickListener {
            if (row.qty > 1) {
                binding.tvTotalAmount?.text = (row.item_price * (row.qty - 1)).toString()
                setOnItemClick.itemDecreased(position, row, binding.tvQty)
            } else {
                showDeleteConfirmationDialog(position, row)
            }
        }
        binding.linAdd.setOnClickListener {
            binding.tvTotalAmount?.text = (row.item_price * (row.qty + 1)).toString()
            setOnItemClick.itemIncreased(position, row, binding.tvQty)
        }
        binding.mainview.setOnClickListener {
            setOnItemClick.onItemClick(position, row, binding.tvQty)
        }
    }

    private fun showDeleteConfirmationDialog(position: Int, row: CartItemRow) {
        val dialogDelete = Dialog(context, R.style.DialogTheme)
        dialogDelete.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDelete.setCancelable(false)
        dialogDelete.setContentView(R.layout.dialog_common_filee)

        val title = dialogDelete.findViewById<AppCompatTextView>(R.id.text_title)
        title.text =
            context.resources.getString(R.string.are_you_sure_do_you_want_to_delete_this_entry)

        val no = dialogDelete.findViewById<AppCompatTextView>(R.id.tv_no)
        no.setOnClickListener { dialogDelete.dismiss() }

        val yes = dialogDelete.findViewById<AppCompatTextView>(R.id.tv_yes)
        yes.setOnClickListener {
            if (position < menuList.size) {
                // The actual view reference doesn't matter much if the activity handles updates
                val dummyView = AppCompatTextView(context)
                setOnItemClick.onItemDeleted(position, row, dummyView)
            }
            dialogDelete.dismiss()
        }
        dialogDelete.show()
    }

    private fun parseNotes(notesJson: String?): String {
        if (notesJson.isNullOrEmpty() || notesJson == "null") {
            return ""
        }
        return try {
            val items = JSONArray(notesJson)
            val instructionList = (0 until items.length()).map { items.getString(it) }
            instructionList.joinToString(", ")
        } catch (e: JSONException) {
            // Fallback for non-JSON or malformed string
            notesJson.replace("[", "").replace("]", "").replace("\"", "")
        }
    }

    // --- Public methods to update the list ---

    fun updateList(list: MutableList<CartItemRow>) {
        menuList = list
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, item: CartItemRow) {
        if (position >= 0 && position < menuList.size) {
            menuList[position] = item
            notifyItemChanged(position)
        }
    }

    interface SetOnItemClick {
        fun itemIncreased(position: Int, item: CartItemRow, view: AppCompatTextView)
        fun itemDecreased(position: Int, item: CartItemRow, view: AppCompatTextView)
        fun onItemDeleted(position: Int, item: CartItemRow, view: AppCompatTextView)
        fun onItemClick(position: Int, item: CartItemRow, view: AppCompatTextView)
    }
}