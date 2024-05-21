package com.eresto.captain.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.model.PrinterRespo

class SelectPrinterAdapter(
    private val context: Context,
    private val list: List<PrinterRespo>,
    var setOnItemClick: SetOnItemClick?
) : RecyclerView.Adapter<SelectPrinterAdapter.MenuRespoViewHolder>() {
    private var selectedPosition = -1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MenuRespoViewHolder {
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.item_printer_selection, parent, false)
        return MenuRespoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuRespoViewHolder, position: Int) {
        val menuData = list[position]
        holder.cbSessionName?.text = menuData.printer_name

        holder.cbSessionName?.setOnClickListener {
            if (setOnItemClick != null)
                setOnItemClick!!.onItemClick(position, menuData)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MenuRespoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var cbSessionName: TextView? = null

        init {
            cbSessionName = view.findViewById(R.id.txt_printer)
        }
    }

    fun SetOnItemClickListner(setOnItemClick: SetOnItemClick?) {
        this.setOnItemClick = setOnItemClick
    }

    interface SetOnItemClick {
        fun onItemClick(position: Int, printer: PrinterRespo)
    }
}
