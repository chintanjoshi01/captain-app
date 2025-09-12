package com.eresto.captain.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.model.GetTables
import com.eresto.captain.utils.KeyUtils.Companion.DELIVERY
import com.eresto.captain.utils.KeyUtils.Companion.ROOM
import com.eresto.captain.utils.KeyUtils.Companion.TAKEAWAY
import com.eresto.captain.utils.Utils
import com.google.android.material.card.MaterialCardView

class TableListAdapter(
    private val context: Context,
    private var list: List<GetTables>,
    private var type: Int,
    var setOnItemClick: SetOnItemClick?
) :
    RecyclerView.Adapter<TableListAdapter.TableRespoViewHolder>() {

    private val isTabletLayout: Boolean

    init {
        // This correctly checks if we should use tablet styling
        val columnCount = context.resources.getInteger(R.integer.table_grid_columns)
        isTabletLayout = columnCount > 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRespoViewHolder {
        val inflater = LayoutInflater.from(context)
        // Ensure you are using your final item layout file
        val view: View = inflater.inflate(R.layout.item_table_list_dine_in, parent, false)
        return TableRespoViewHolder(view)
    }

    fun updateData(newTableList: List<GetTables>) {
        list = newTableList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TableRespoViewHolder, position: Int) {
        val data = list[position]
        holder.txtTitle!!.text = data.tab_label

        holder.imgDine!!.setImageResource(
            when (data.tab_type) {
                TAKEAWAY -> R.drawable.ic_takeaway_icon
                ROOM -> R.drawable.ic_rooms_icon
                DELIVERY -> R.drawable.ic_delivery_order
                else -> R.drawable.ic_dine_in
            }
        )

        // *** THIS IS THE KEY PART THAT CHOOSES THE STYLE ***
       /* if (isTabletLayout) {
            applyTabletStyling(holder, data)
        } else {
            applyMobileStyling(holder, data)
        }*/

        applyTabletStyling(holder, data)

        // Apply dynamic horizontal/vertical layout for the item's content
        applyItemOrientation(holder)


        holder.mView!!.setOnClickListener {
            setOnItemClick?.onItemClick(position, data)
        }
        holder.mView!!.setOnLongClickListener {
            setOnItemClick?.onEdit(position, data)
            true
        }
    }

    private fun applyItemOrientation(holder: TableRespoViewHolder) {
        // This logic handles the vertical/horizontal switch for the icon and text
        val isHorizontal = context.resources.getBoolean(R.bool.is_item_layout_horizontal)
        if (holder.itemContentLayout != null) {
            if (isHorizontal) {
                holder.itemContentLayout.orientation = LinearLayout.HORIZONTAL
                val params = holder.txtTitle!!.layoutParams as LinearLayout.LayoutParams
                params.marginStart =
                    context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                params.topMargin = 0
                holder.txtTitle.layoutParams = params
            } else {
                /*holder.itemContentLayout.orientation = LinearLayout.VERTICAL
                val params = holder.txtTitle!!.layoutParams as LinearLayout.LayoutParams
                params.marginStart = 0
                params.topMargin =
                    context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                holder.txtTitle.layoutParams = params*/
            }
        }
    }


    private fun applyTabletStyling(holder: TableRespoViewHolder, data: GetTables) {
        // Reset to default style first
        holder.card!!.strokeWidth =
            context.resources.getDimensionPixelSize(R.dimen.stoke_width)
        holder.card.strokeColor = ContextCompat.getColor(context, R.color.transparent)


        when (data.tab_status) {
            // *** NEW: Style for status 2 (Running Table) ***
            2 -> {
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.table_running_background
                    )
                )
                holder.card.strokeColor =
                    ContextCompat.getColor(context, R.color.table_running_border)
                holder.card.strokeWidth =
                    context.resources.getDimensionPixelSize(R.dimen.stoke_width) // Make border thicker
                holder.txtTitle!!.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorSecondary
                    )
                )
                holder.imgDine!!.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
            }

            // *** Style for status 3 (Closed/Occupied Table) ***
            3 -> {
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.table_closed_background
                    )
                )

                holder.txtTitle!!.setTextColor(ContextCompat.getColor(context, R.color.color_white))
                holder.imgDine!!.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_white))
                holder.card.strokeWidth = 0 // No border for solid colors
            }

            // Style for status 5 (Billed Table)
            5 -> {
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.table_pending_rc_background
                    )
                )
                holder.card.strokeColor =
                    ContextCompat.getColor(context, R.color.table_pending_rc_border)
                holder.txtTitle!!.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorSecondary
                    )
                )
                holder.imgDine!!.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
                holder.card.strokeWidth = 0 // No border for solid colors
            }

            // Default Style (Available Table)
            else -> {
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.table_free_background
                    )
                )
                holder.card.strokeColor =
                    ContextCompat.getColor(context, R.color.table_free_border)
                holder.txtTitle!!.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorSecondary
                    )
                )
                holder.imgDine!!.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
                // Use the default border set at the top of the function
            }
        }
    }

    private fun applyMobileStyling(holder: TableRespoViewHolder, data: GetTables) {
        // --- THIS IS YOUR ORIGINAL CODE FOR MOBILE, IT REMAINS UNCHANGED ---
        holder.card!!.radius = context.resources.getDimension(com.intuit.sdp.R.dimen._4sdp)
        holder.txtTitle!!.setTextColor(ContextCompat.getColor(context, R.color.colorSecondary))

        var color = ContextCompat.getColor(context, R.color.color_pendingItem_LightRed)

        when (data.tab_status) {
            2 -> {
                color = ContextCompat.getColor(context, R.color.colorPrimary)
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.light_pink
                    )
                )
            }

            3 -> {
                color = ContextCompat.getColor(context, R.color.color_white)
                holder.card.radius = context.resources.getDimension(com.intuit.sdp.R.dimen._20sdp)
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorPrimary
                    )
                )
                holder.txtTitle.setTextColor(ContextCompat.getColor(context, R.color.color_white))
            }

            4 -> {
                color = ContextCompat.getColor(context, R.color.color_white)
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.short_day
                    )
                )
                holder.txtTitle.setTextColor(ContextCompat.getColor(context, R.color.color_white))
            }

            5 -> {
                color = ContextCompat.getColor(context, R.color.short_day)
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.golden_color
                    )
                )
                holder.txtTitle.setTextColor(ContextCompat.getColor(context, R.color.short_day))
            }

            else -> {
                color = ContextCompat.getColor(context, R.color.color_pendingItem_LightRed)
                holder.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_white
                    )
                )
            }
        }

        val states = arrayOf(intArrayOf(android.R.attr.state_enabled))
        val myList = ColorStateList(states, Utils.getColorState(color))
        holder.imgDine!!.imageTintList = myList
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class TableRespoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        // This must match the IDs in your final XML file
        val itemContentLayout: LinearLayout? = view.findViewById(R.id.item_content_layout)
        val txtTitle: TextView? = view.findViewById(R.id.txt_name)
        val imgDine: ImageView? = view.findViewById(R.id.img_dine)
        val card: MaterialCardView? = view.findViewById(R.id.card)
        val mView: View? = view
    }

    interface SetOnItemClick {
        fun onItemClick(position: Int, tab: GetTables)
        fun onEdit(position: Int, tab: GetTables)
    }
}