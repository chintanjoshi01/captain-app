package com.eresto.captain.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.eresto.captain.R
import com.eresto.captain.databinding.ItemTakeawayOrderKotListBinding
import com.eresto.captain.databinding.ItemTakeawayOrderSubItemsListBinding
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.KotInstance
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.ItemServingStatus
import com.eresto.captain.utils.Utils
import org.json.JSONArray
import org.json.JSONException
import java.util.Locale

class DineKOTAdapter(
    var context: Context,
    var isEdit: Boolean,
    var isDeleteShow: Boolean,
    var isUpdate: Boolean,
    var isDelete: Boolean,
    var menuList: List<Any>,
    var database: DBHelper,
    var setOnItemClick: SetOnItemClick?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class ViewHolderItemKot(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        internal var layout =
            binding.root.findViewById<CardView>(R.id.category_parent_container_kot)
        internal var textOrder = binding.root.findViewById<TextView>(R.id.text_order)
        internal var textSectionTableName =
            binding.root.findViewById<TextView>(R.id.text_section_table_name)
        internal var txtTimeTakeaway = binding.root.findViewById<TextView>(R.id.txt_time_takeaway)
        internal var imgDelete = binding.root.findViewById<ImageButton>(R.id.img_delete)
        internal var imgEdit = binding.root.findViewById<ImageButton>(R.id.img_edit)
        internal var recyclerViewItem =
            binding.root.findViewById<RecyclerView>(R.id.recyclerviewItem_kot)
        internal var upArrowImg =
            binding.root.findViewById<AppCompatImageButton>(R.id.btn_arrow_kot)
        internal var txtTime = binding.root.findViewById<TextView>(R.id.txt_time)
    }

    inner class ViewHolderOrderSub(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        internal var layoutChild = binding.root.findViewById<RelativeLayout>(R.id.layoutChild)
        internal var txtSectionName =
            binding.root.findViewById<AppCompatTextView>(R.id.text_section_name)
        internal var textSPInst = binding.root.findViewById<AppCompatTextView>(R.id.text_sp_inst)
        internal var txtQty = binding.root.findViewById<AppCompatTextView>(R.id.txt_qty)
        internal var v = binding.root.findViewById<View>(R.id.v)
        internal var imageDeleteKotItem =
            binding.root.findViewById<ImageView>(R.id.image_deleteKotItem)
        internal var linerLayout = binding.root.findViewById<LinearLayout>(R.id.lin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            Utils.CATEGORY -> {
                val binding = ItemTakeawayOrderKotListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ViewHolderItemKot(binding)
            }

            Utils.ITEMS -> {
                val binding = ItemTakeawayOrderSubItemsListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ViewHolderOrderSub(binding)
            }

            else -> {
                val binding = ItemTakeawayOrderKotListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ViewHolderItemKot(binding)
            }
        }
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (menuList[position] is KotInstance) {
            val row = menuList[position] as KotInstance
            (holder as ViewHolderItemKot).textSectionTableName.text =
                "-${row.short_name ?: "-"}"
            holder.textOrder.text = "#${row.instance}"
            holder.txtTime.text = Utils.getAgoTimeShort(row.kot_order_date)

            if (row.soft_delete == 1) {
                holder.layout.visibility = View.GONE
            }


            if (row.isExpanded) {
                holder.recyclerViewItem!!.visibility = View.VISIBLE
            } else {
                holder.recyclerViewItem!!.visibility = View.GONE
            }
            holder.recyclerViewItem!!.layoutManager =
                LinearLayoutManager(context, RecyclerView.VERTICAL, false)

            if (row.item.isNotEmpty()) {
                val adapter =
                    DineKOTAdapter(
                        context,
                        isEdit,
                        isDeleteShow,
                        isUpdate,
                        isDelete,
                        row.item,
                        database,
                        object :
                            DineKOTAdapter.SetOnItemClick {
                            override fun onItemChecked(
                                position: Int,
                                isChecked: Boolean,
                                orderKotId: Int
                            ) {
                                if (isUpdate) if (setOnItemClick != null) setOnItemClick!!.onItemChecked(
                                    position,
                                    isChecked,
                                    orderKotId
                                )
                            }

                            override fun onPrintKOT(position: Int, kot: KotInstance) {

                            }

                            override fun onKOTEdit(position: Int, orderKotId: KotInstance) {

                            }

                            override fun onItemClick(position: Int, item: ItemQSR) {
                                if (isUpdate) if (setOnItemClick != null) setOnItemClick!!.onItemClick(
                                    position,
                                    item
                                )
                            }

                            override fun onKOTDelete(position: Int, orderKotId: String) {

                            }

                            override fun onLongPress(position: Int, orderKotId: KotInstance) {

                            }

                            override fun onKOTChecked(
                                position: Int,
                                toString: Boolean,
                                orderKotId: KotInstance
                            ) {

                            }

                            override fun onKotItemDelete(
                                position: Int,
                                orderKotId: Int,
                                kot: KotInstance?
                            ) {
                                if (isDelete) if (setOnItemClick != null) setOnItemClick!!.onKotItemDelete(
                                    position,
                                    orderKotId,
                                    row
                                )
                            }
                        })
                holder.recyclerViewItem!!.adapter = adapter
            }

            holder.upArrowImg.setOnClickListener {
                if (setOnItemClick != null) setOnItemClick!!.onKOTEdit(position, row)
            }
            holder.layout.setOnClickListener {
                if (row.isExpanded) {
                    row.isExpanded = false
                    holder.upArrowImg.rotation = 0f
                    holder.recyclerViewItem!!.visibility = View.GONE
                } else {
                    row.isExpanded = true
                    holder.upArrowImg.rotation = 180F
                    holder.recyclerViewItem!!.visibility = View.VISIBLE
                }
            }
            holder.imgEdit.setOnClickListener {
                if (isUpdate) if (setOnItemClick != null) setOnItemClick!!.onKOTEdit(position, row)
            }
            holder.imgDelete.setOnClickListener {
                if (isDelete) if (setOnItemClick != null) setOnItemClick!!.onKOTDelete(
                    position,
                    row.kot_instance_id
                )
            }
            holder.layout.setOnLongClickListener {
                if (isUpdate) if (setOnItemClick != null) setOnItemClick!!.onLongPress(
                    position,
                    row
                )
                true
            }
        } else if (menuList[position] is ItemQSR) {
            val row = menuList[position] as ItemQSR
            row.item_name = row.item_name.uppercase(Locale.US)
            (holder as ViewHolderOrderSub).txtSectionName.text = row.item_name
            holder.txtQty!!.text = "x${row.qty}"
            // 1. Get the service time.
            val serviceTime = database.getDeliveryTimeById(row.item_id) ?: 15

            // 2. Use the updated helper function. It now correctly handles the `created_at` string.
            val (status, duration) = Utils.determineItemStatus(row, serviceTime)
            if (row.isd != 0) {
                holder.imageDeleteKotItem.setImageResource(R.drawable.ic_check_circle)
            } else if (status == ItemServingStatus.WITHIN_SERVICE_TIME) {
                holder.imageDeleteKotItem.setImageResource(R.drawable.ic_wthin_time)
            } else if (status == ItemServingStatus.BEYOND_SERVICE_TIME) {
                holder.imageDeleteKotItem.setImageResource(R.drawable.ic_service_time)
            }

            if (row.soft_delete == 1) {
                holder.linerLayout.visibility = View.GONE
            }
            if (row.sp_inst != null && row.sp_inst != "" && row.sp_inst != "[\"\"]") {
                holder.textSPInst.visibility = View.VISIBLE
                try {
                    val items = JSONArray(row.sp_inst)
                    var inst = ""
                    for (i in 0 until items.length()) {
                        inst += items.getString(i) + ","
                    }
                    if (inst.isNotEmpty()) {
                        inst = inst.substring(0, inst.length - 1)
                    }
                    holder.textSPInst.text = inst
                } catch (e: JSONException) {
                    holder.textSPInst.text = row.sp_inst
                    e.printStackTrace()
                }
            } else {
                holder.textSPInst.visibility = View.GONE
            }

            if (position < menuList.size - 1) {
                holder.v.visibility = View.VISIBLE
            } else {
                holder.v.visibility = View.GONE
            }

            holder.imageDeleteKotItem?.setOnClickListener {
                if (setOnItemClick != null) setOnItemClick!!.onKotItemDelete(position, row.id, null)
            }

            holder.layoutChild.setOnClickListener {
                if (setOnItemClick != null) setOnItemClick!!.onItemClick(position, row)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (menuList[position] is KotInstance) {
            Utils.CATEGORY
        } else if (menuList[position] is ItemQSR) {
            Utils.ITEMS
        } else {
            Utils.CATEGORY
        }
    }

    fun SetOnItemClickListner(setOnItemClick: SetOnItemClick?) {
        this.setOnItemClick = setOnItemClick
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