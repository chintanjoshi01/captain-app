package com.eresto.captain.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.model.GetTables
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.KeyUtils.Companion.DELIVERY
import com.eresto.captain.utils.KeyUtils.Companion.ROOM
import com.eresto.captain.utils.KeyUtils.Companion.TAKEAWAY
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.Utils

class TableListAdapter(
    private val context: Context,
    private var list: List<GetTables>,
    private var type: Int,
    var setOnItemClick: SetOnItemClick?
) :
    RecyclerView.Adapter<TableListAdapter.TableRespoViewHolder>() {
    var pref: Preferences? = Preferences()
    var userRole = pref!!.getInt(context, KeyUtils.roleId)
    val states =
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_pressed)
        )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRespoViewHolder {
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.item_table_list_dine_in, parent, false)
        return TableRespoViewHolder(view)
    }

    fun updateData(newTableList: List<GetTables>) {
        list = newTableList
        notifyDataSetChanged() // Notify adapter without changing the scroll position
    }

    fun getData(): List<GetTables> {
        return list
    }

    override fun onBindViewHolder(holder: TableRespoViewHolder, position: Int) {
        val data = list[position]
        holder.txtTitle!!.text = data.tab_label
        holder.txtTitle!!.setTextColor(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                R.color.colorSecondary
            ) else context.resources.getColor(R.color.colorSecondary)
        )
        holder.imgDine!!.setImageResource(
            when (data.tab_type) {
                TAKEAWAY->R.drawable.ic_takeaway_icon
                ROOM->R.drawable.ic_rooms_icon
                DELIVERY->R.drawable.ic_delivery_order
                else->R.drawable.ic_dine_in
            }
        )

        holder.mView!!.setOnLongClickListener {
            if (setOnItemClick != null) setOnItemClick!!.onEdit(position, data)
            true
        }
        holder.card!!.radius = context.resources.getDimension(com.intuit.sdp.R.dimen._4sdp)
//        holder.txtTitle!!.setTextColor(
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
//                R.color.colorSecondary
//            ) else context.resources.getColor(R.color.colorSecondary)
//        )
//        if (data.noc == 0) {
//            holder.relCust!!.visibility = View.GONE
//        } else {
//            holder.relCust!!.visibility = View.VISIBLE
//        }
//        if (data.is_ok == 0) {
//            val animBlink = AnimationUtils.loadAnimation(context, R.anim.blink)
//            holder.imgDine!!.startAnimation(animBlink)
//        } else {
//            holder.imgDine!!.clearAnimation()
//        }

        var color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(R.color.color_pendingItem_LightRed)
        } else {
            context.resources.getColor(R.color.color_pendingItem_LightRed)
        }

        when (data.tab_status) {
            2 -> {
                color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getColor(R.color.colorPrimary)
                } else {
                    context.resources.getColor(R.color.colorPrimary)
                }
                holder.card!!.setCardBackgroundColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.light_pink
                    ) else context.resources.getColor(R.color.light_pink)
                )
                holder.mView!!.setOnClickListener {
                    if (setOnItemClick != null) setOnItemClick!!.onItemClick(position, data)
                }
            }

            3 -> {
                color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getColor(R.color.color_white)
                } else {
                    context.resources.getColor(R.color.color_white)
                }
                holder.card!!.radius = context.resources.getDimension(com.intuit.sdp.R.dimen._20sdp)
                holder.card!!.setCardBackgroundColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.getColor(R.color.colorPrimary)
                    } else {
                        context.resources.getColor(R.color.colorPrimary)
                    }
                )
                holder.txtTitle!!.setTextColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.color_white
                    ) else context.resources.getColor(R.color.color_white)
                )
                holder.mView!!.setOnClickListener {
                    if (setOnItemClick != null) setOnItemClick!!.onItemClick(position, data)
                }
            }

            4 -> {
                color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getColor(R.color.color_white)
                } else {
                    context.resources.getColor(R.color.color_white)
                }

                holder.card!!.setCardBackgroundColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.short_day
                    ) else context.resources.getColor(R.color.short_day)
                )
                holder.txtTitle!!.setTextColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.color_white
                    ) else context.resources.getColor(R.color.color_white)
                )

            }

            5 -> {
                color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getColor(R.color.short_day)
                } else {
                    context.resources.getColor(R.color.short_day)
                }

                holder.card!!.setCardBackgroundColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.golden_color
                    ) else context.resources.getColor(R.color.golden_color)
                )
                holder.txtTitle!!.setTextColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.short_day
                    ) else context.resources.getColor(R.color.short_day)
                )

                holder.mView!!.setOnClickListener {
                    if (setOnItemClick != null) setOnItemClick!!.onItemClick(position, data)
                }
            }

            else -> {
                color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getColor(R.color.color_pendingItem_LightRed)
                } else {
                    context.resources.getColor(R.color.color_pendingItem_LightRed)
                }
                holder.card!!.setCardBackgroundColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(
                        R.color.color_white
                    ) else context.resources.getColor(R.color.color_white)
                )
                holder.mView!!.setOnClickListener {
                    if (setOnItemClick != null) setOnItemClick!!.onItemClick(position, data)
                }
            }
        }

        val myList = ColorStateList(states, Utils.getColorState(color))
        holder.imgDine!!.imageTintList = myList
//        if (data.tat == 0) {
//            holder.imgBlink!!.visibility = View.GONE
//        } else {
//            holder.imgBlink!!.visibility = View.VISIBLE
//            val animBlink = AnimationUtils.loadAnimation(context, R.anim.blink)
//            holder.imgBlink!!.startAnimation(animBlink)
//        }
        holder.mView!!.setOnClickListener {
            if (setOnItemClick != null) setOnItemClick!!.onItemClick(position, data)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setAdapter(list: List<GetTables>, type: Int) {
        this.list = list
        this.type = type
        notifyDataSetChanged()
    }

    class TableRespoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var txtTitle: TextView? = null
        var imgDine: ImageView? = null
        var imgBlink: ImageView? = null
        var custOrder: TextView? = null
        var relCust: RelativeLayout? = null
        var card: CardView? = null
        var mView: View? = null

        init {
            txtTitle = view.findViewById(R.id.txt_name)
            imgBlink = view.findViewById(R.id.img_blink)
            imgDine = view.findViewById(R.id.img_dine)
            custOrder = view.findViewById(R.id.cust_order)
            relCust = view.findViewById(R.id.rel_cust)
            card = view.findViewById(R.id.card)
            mView = view
        }
    }

    fun SetOnItemClickListner(setOnItemClick: SetOnItemClick?) {
        this.setOnItemClick = setOnItemClick
    }

    interface SetOnItemClick {
        fun onItemClick(position: Int, tab: GetTables)
        fun onEdit(position: Int, tab: GetTables)
    }
}