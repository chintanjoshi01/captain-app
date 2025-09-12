package com.eresto.captain.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.databinding.ItemTakeawayOrderKotListBinding
import com.eresto.captain.databinding.ItemTakeawayOrderSubItemsListBinding
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.KotInstance
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.ItemServingStatus
import com.eresto.captain.utils.MaxHeightRecyclerView
import com.eresto.captain.utils.Utils

import org.json.JSONArray
import org.json.JSONException
import java.util.Locale

class DineKOTAdapter(
    private val context: Context,
    private val isEdit: Boolean,
    private val isDeleteShow: Boolean,
    private val isUpdate: Boolean,
    private val isDelete: Boolean,
    private val menuList: List<Any>,
    private val database: DBHelper,
    private var setOnItemClick: SetOnItemClick?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class ViewHolderItemKot(val binding: ItemTakeawayOrderKotListBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ViewHolderOrderSub(val binding: ItemTakeawayOrderSubItemsListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Utils.CATEGORY -> {
                val binding = ItemTakeawayOrderKotListBinding.inflate(inflater, parent, false)
                ViewHolderItemKot(binding)
            }
            else -> {
                val binding = ItemTakeawayOrderSubItemsListBinding.inflate(inflater, parent, false)
                ViewHolderOrderSub(binding)
            }
        }
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            Utils.CATEGORY -> {
                val kotHolder = holder as ViewHolderItemKot
                val kotInstance = menuList[position] as KotInstance
                bindKotViewHolder(kotHolder, kotInstance, position)
            }

            Utils.ITEMS -> {
                val itemHolder = holder as ViewHolderOrderSub
                val itemQSR = menuList[position] as ItemQSR
                bindItemViewHolder(itemHolder, itemQSR, position)
            }
        }
    }

    private fun bindKotViewHolder(holder: ViewHolderItemKot, row: KotInstance, position: Int) {
        with(holder.binding) {
            textSectionTableName.text = "-${row.short_name ?: "-"}"
            textOrder.text = "#${row.instance}"
            txtTime.text = Utils.getAgoTimeShort(row.kot_order_date)

            // Hide the entire card if it's soft-deleted
            categoryParentContainerKot.visibility =
                if (row.soft_delete == 1) View.GONE else View.VISIBLE

            // Handle expand/collapse state
            recyclerviewItemKot.visibility = if (row.isExpanded) View.VISIBLE else View.GONE
//            btnArrowKot.rotation = if (row.isExpanded) 180f else 0f

            // Setup inner RecyclerView
            setupInnerRecyclerView(holder, row, position)

            // Click Listener on HEADER ONLY (prevents intercept while scrolling inner RV)
            categoryParentContainerKot.setOnClickListener {
                row.isExpanded = !row.isExpanded
                notifyItemChanged(position)
            }

            btnArrowKot.setOnClickListener {
                if (isUpdate) setOnItemClick?.onKOTEdit(position, row)
            }

            imgDelete.setOnClickListener {
                if (isDelete) setOnItemClick?.onKOTDelete(position, row.kot_instance_id)
            }

            categoryParentContainerKot.setOnLongClickListener {
                if (isUpdate) setOnItemClick?.onLongPress(position, row)
                true
            }
        }
    }

    private fun setupInnerRecyclerView(
        holder: ViewHolderItemKot,
        row: KotInstance,
        parentPosition: Int
    ) {
        val innerRecyclerView = holder.binding.recyclerviewItemKot
        with(innerRecyclerView) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

            // isNestedScrollingEnabled should be false when using requestDisallowInterceptTouchEvent
            // as they are two different ways of handling the same problem. The latter is more reliable.
            isNestedScrollingEnabled = false

            if (row.item.isNotEmpty()) {
                val adapter = DineKOTAdapter(
                    context, isEdit, isDeleteShow, isUpdate, isDelete, row.item, database,
                    object : SetOnItemClick {
                        override fun onItemChecked(pos: Int, isChecked: Boolean, orderKotId: Int) {
                            if (isUpdate) setOnItemClick?.onItemChecked(pos, isChecked, orderKotId)
                        }

                        override fun onItemClick(pos: Int, item: ItemQSR) {
                            if (isUpdate) setOnItemClick?.onItemClick(pos, item)
                        }

                        override fun onKotItemDelete(pos: Int, orderKotId: Int, kot: KotInstance?) {
                            if (isDelete) setOnItemClick?.onKotItemDelete(pos, orderKotId, row)
                        }

                        override fun onPrintKOT(pos: Int, kot: KotInstance) {}
                        override fun onKOTEdit(pos: Int, kot: KotInstance) {
                            if (isUpdate) setOnItemClick?.onKOTEdit(pos, kot)
                        }
                        override fun onKOTDelete(pos: Int, instanceId: String) {}
                        override fun onLongPress(pos: Int, kot: KotInstance) {}
                        override fun onKOTChecked(pos: Int, isChecked: Boolean, kot: KotInstance) {}
                    }
                )
                this.adapter = adapter

                // If we have our custom MaxHeightRecyclerView, set the cap programmatically
             /*   if (this is MaxHeightRecyclerView) {
                    val itemHeight = context.resources.getDimensionPixelSize(R.dimen.kot_sub_item_height)
                    (this as MaxHeightRecyclerView).setMaxHeightPx(itemHeight * 4)
                }*/

                // *** THIS IS THE FIX FOR THE SCROLLING ISSUE ***
                val touchListener = View.OnTouchListener { view, motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // When the user first touches the inner RecyclerView,
                            // tell the parent RecyclerView to stop intercepting touch events.
                            view.parent.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // When the user lifts their finger,
                            // allow the parent to intercept again.
                            view.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    // Let the inner RecyclerView handle the touch event.
                    // Returning 'false' means we haven't consumed the event.
                    false
                }
                setOnTouchListener(touchListener)

            } else {
                layoutParams = layoutParams?.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }
    }


    private fun bindItemViewHolder(holder: ViewHolderOrderSub, row: ItemQSR, position: Int) {
        with(holder.binding) {
            // Hide if soft deleted
            root.visibility = if (row.soft_delete == 1) View.GONE else View.VISIBLE
            if (row.soft_delete == 1) return

            textSectionName.text = row.item_name.uppercase(Locale.getDefault())
            txtQty.text = "x${row.qty}"

            // Status Icon Logic
            val serviceTime = database.getDeliveryTimeById(row.item_id) ?: 15
            val (status, _) = Utils.determineItemStatus(row, serviceTime)

            val statusIconRes = when {
                row.isd != 0 -> R.drawable.ic_check_circle
                status == ItemServingStatus.WITHIN_SERVICE_TIME -> R.drawable.ic_wthin_time
                status == ItemServingStatus.BEYOND_SERVICE_TIME -> R.drawable.ic_service_time
                else -> R.drawable.ic_wthin_time
            }
            imageDeleteKotItem.setImageResource(statusIconRes)

            // Special Instructions
            val instructions = parseInstructions(row.sp_inst)
            if (instructions.isNotEmpty()) {
                textSpInst.visibility = View.VISIBLE
                textSpInst.text = instructions
            } else {
                textSpInst.visibility = View.GONE
            }

            // Divider visibility for all but the last item
            v.visibility = if (position < menuList.size - 1) View.VISIBLE else View.GONE

            // --- Click Listeners ---
            imageDeleteKotItem.setOnClickListener {
                setOnItemClick?.onKotItemDelete(position, row.id, null)
            }

            layoutChild.setOnClickListener {
                setOnItemClick?.onItemClick(position, row)
            }
        }
    }

    private fun parseInstructions(spInstJson: String?): String {
        if (spInstJson.isNullOrBlank() || spInstJson == "[\"\"]") {
            return ""
        }
        return try {
            val items = JSONArray(spInstJson)
            (0 until items.length())
                .map { items.getString(it) }
                .filter { it.isNotBlank() }
                .joinToString(separator = ", ")
        } catch (e: JSONException) {
            spInstJson // Return original string if it's not valid JSON
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (menuList[position]) {
            is KotInstance -> Utils.CATEGORY
            is ItemQSR -> Utils.ITEMS
            else -> throw IllegalArgumentException("Unsupported type at position $position")
        }
    }

    fun setOnItemClickListener(listener: SetOnItemClick?) {
        this.setOnItemClick = listener
    }

    interface SetOnItemClick {
        fun onKOTChecked(position: Int, isChecked: Boolean, kot: KotInstance)
        fun onKOTDelete(position: Int, instanceId: String)
        fun onKOTEdit(position: Int, kot: KotInstance)
        fun onLongPress(position: Int, kot: KotInstance)
        fun onItemClick(position: Int, item: ItemQSR)
        fun onItemChecked(position: Int, isChecked: Boolean, id: Int)
        fun onKotItemDelete(position: Int, orderKotId: Int, kot: KotInstance?)
        fun onPrintKOT(position: Int, kot: KotInstance)
    }
}