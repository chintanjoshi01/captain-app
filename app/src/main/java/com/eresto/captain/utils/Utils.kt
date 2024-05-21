package com.eresto.captain.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.adapter.SelectPrinterAdapter
import com.eresto.captain.model.DataInvoice
import com.eresto.captain.model.InvoiceKot
import com.eresto.captain.model.KotInstance
import com.eresto.captain.model.Orders
import com.eresto.captain.model.PrinterRespo
import com.eresto.captain.model.TakeawayOrder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class Utils {

    companion object {
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        const val printCommand = "windows-1252" //windows-1252
        const val CATEGORY = 1
        const val ITEMS = 2
        const val TAX = 3


        private var dialogSnack: Dialog? = null
        var dialogDismissTry = 0

        private var dialogConst: Dialog? = null

        fun hideKeyboard(context: Context, view: View) {
            view.clearFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken!!, 0)
        }


        fun getImageview(rel: RelativeLayout): ImageView {
            return rel.getChildAt(1) as ImageView
        }

        fun getColorState(color: Int): IntArray {
            return intArrayOf(color, color, color, color)
        }

        fun countTaxDiscount(
            price: Double,
            qty: Int,
            taxJsons: String?,
            disc_percentage: Float,
            disc_type: Int,
            subTotal: Double,
            invoicing: Int
        ): String {
            var discount = 0.0
            var itemValue = (price * qty).toDouble()
            discount = if (disc_percentage < 1) {
                0.0
            } else {
                if (disc_type == 2) {
                    // Percentage
                    disc_percentage.toDouble()
                } else {
                    //Fixed
                    ((disc_percentage.toDouble() * 100) / subTotal)
                }

            }
            if (discount > 0) {
                discount = ((itemValue * discount) / 100)
                itemValue -= discount
            }
            var itemTaxAmt = 0.00
            if (!taxJsons.isNullOrEmpty() && taxJsons != "null") {
                val taxJson = JSONArray(taxJsons)
                for (i in 0 until taxJson.length()) {
                    val taxRate = taxJson.getJSONObject(i).getString("tax_rate").toDouble()
                    itemTaxAmt += (itemValue * taxRate) / 100
                    itemTaxAmt = (itemTaxAmt * 100.0).roundToInt() / 100.0
                }
            }
            if (invoicing == 1) {
                itemValue += itemTaxAmt
            }
            return "$itemTaxAmt|$itemValue|$discount"
        }

        fun checkChips(text: String, chips: ChipGroup) {
            var arrStr = ArrayList<String>()
            if (text.isNotEmpty()) {
                if (text.contains(",")) {
                    arrStr = text.split(",") as ArrayList<String>
                } else {
                    arrStr.add(text)
                }
            }
            for (i in 0 until chips.childCount) {
                val chip = chips.getChildAt(i) as Chip
                val chipText = chip.text.toString()
                if (arrStr.contains(chipText)) {
                    if (!chip.isChecked) {
                        chip.isChecked = true
                    }
                } else {
                    if (chip.isChecked) {
                        chip.isChecked = false
                    }
                }
            }
        }

        fun getCheckedIns(text: String, chips: ChipGroup): String {
            val arrStr = ArrayList<String>()

            if (text.isNotEmpty()) {
                arrStr.addAll(text.split(",").map { it.trim() }.filter { it.isNotBlank() }) // Proper split and trim
            }

            for (i in 0 until chips.childCount) {
                val chip = chips.getChildAt(i) as Chip
                if (chip.isChecked) {
                    if (!arrStr.contains(chip.text.toString())) arrStr.add(chip.text.toString())
                    chip.setTextAppearance(R.style.CheckedChipTextAppearance)
                    chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst_checked)
                } else {
                    arrStr.remove(chip.text.toString())
                    chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
                    chip.setTextAppearance(R.style.ChipTextAppearance)
                }
            }

            return arrStr.joinToString(",") // Proper cleanup of text
        }

        fun selectPrinter(
            activity: Activity,
            order: Orders,
            tableName: String,
            printerList: ArrayList<PrinterRespo>,
            listener: DialogInterface.OnCancelListener,
        ) {
            val dialog = Dialog(activity, R.style.DialogTheme)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.dialog_select_printer)
            val imgBack = dialog.findViewById(R.id.img_back) as ImageButton
            imgBack.setOnClickListener {
                dialog.cancel()
            }
            val txtSelectPrinter =
                dialog.findViewById(R.id.txt_select_printer) as AppCompatTextView
            txtSelectPrinter.text =
                activity.resources.getString(R.string.select_printer_to_print_kots)

            val rvRestoSessions = dialog.findViewById(R.id.rvRestoSessions) as RecyclerView
            rvRestoSessions.layoutManager =
                LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

            val adapter = SelectPrinterAdapter(activity, printerList, object :
                SelectPrinterAdapter.SetOnItemClick {
                override fun onItemClick(position: Int, printer: PrinterRespo) {
                    PrintMaster.printAllKOTFunction(
                        activity,
                        order,
                        tableName,
                        printer.ip_add,
                        printer.port_add
                    ) {
                        listener.onCancel(dialog)
                        dialog.dismiss()
                    }
                    dialog.dismiss()
                }
            })
            dialog.setOnCancelListener(listener)
            rvRestoSessions.adapter = adapter

            val cancel = dialog.findViewById(R.id.text_cancel) as AppCompatTextView
            cancel.visibility = View.GONE
            cancel.setOnClickListener {
            }

            val save = dialog.findViewById(R.id.text_ok) as AppCompatTextView
            save.visibility = View.GONE
            save.setOnClickListener {
            }

            Objects.requireNonNull(dialog.window)
                ?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))

            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window!!.setGravity(Gravity.CENTER)
            dialog.show()
        }

        fun getAgoTimeShort(time: String): String {
            Log.e("dladajla", "kot_order_date :: $time")
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val format2 = SimpleDateFormat("yyyy-MM-dd")
            val formatDisplay = SimpleDateFormat("dd-MM-yy")
            try {
                val past = format.parse(time) //"2016.02.05 AD at 23:59:30"
                val now = Date()
                val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(now.time - past.time)
                val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time)
                val hours: Long = TimeUnit.MILLISECONDS.toHours(now.time - past.time)
                val days: Long = TimeUnit.MILLISECONDS.toDays(now.time - past.time)
                return if (seconds < 60) {
                    ("$seconds sec")
                } else if (minutes < 60) {
                    ("$minutes min")
                } else if (hours < 24) {
                    ("$hours hr ${minutes % 60 + 1} min")
                } else {
                    ("$days days")
                }
            } catch (j: Exception) {
                Log.e("jlfsjfs", "kot_order_date catch ::  :: $time")
                val past = format2.parse(time) //"2016.02.05 AD at 23:59:30"
                val now = Date()
                val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(now.time - past.time)
                val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time)
                val hours: Long = TimeUnit.MILLISECONDS.toHours(now.time - past.time)
                val days: Long = TimeUnit.MILLISECONDS.toDays(now.time - past.time)
                return formatDisplay.format(past)
                j.printStackTrace()
            }
            return time
        }

        fun getIMEI(context: Context): String {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } else {
                tm.deviceId
            }
            return imei
        }

        fun selectPrinter(
            activity: Activity,
            kot: KotInstance?,
            kots: List<InvoiceKot>?,
            kotTake: TakeawayOrder?,
            data: DataInvoice?,
            tableName: String,
            person: Int,
            custAdd: String,
            type: Int,
            printerList: ArrayList<PrinterRespo>,
            listener: DialogInterface.OnCancelListener,
        ) {
            if (printerList.isNullOrEmpty()) {
                listener.onCancel(null)
            } else {
                val dialog = Dialog(activity, R.style.DialogTheme)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(false)
                dialog.setContentView(R.layout.dialog_select_printer)
                val imgBack = dialog.findViewById(R.id.img_back) as ImageButton
                imgBack.setOnClickListener {
                    dialog.cancel()
                }
                val txtSelectPrinter =
                    dialog.findViewById(R.id.txt_select_printer) as AppCompatTextView
                if (kot != null || kotTake != null) {
                    txtSelectPrinter.text =
                        activity.resources.getString(R.string.select_printer_to_print_kot)
                } else if (data != null) {
                    txtSelectPrinter.text =
                        activity.resources.getString(R.string.select_printer_to_print_invoice)
                }

                val rvRestoSessions = dialog.findViewById(R.id.rvRestoSessions) as RecyclerView
                rvRestoSessions.layoutManager =
                    LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

                val adapter = SelectPrinterAdapter(activity, printerList, object :
                    SelectPrinterAdapter.SetOnItemClick {
                    override fun onItemClick(position: Int, printer: PrinterRespo) {
                        val pref = Preferences()
                        val kitchenPrint = pref.getInt(activity, "kitchen_print")
                        when (kitchenPrint) {
                            0 -> {
                                PrintMaster.decidePrintFunctionality(
                                    activity,
                                    kot,
                                    kots,
                                    kotTake,
                                    data,
                                    tableName,
                                    person,
                                    custAdd,
                                    printer,
                                    type
                                ) {
                                    listener.onCancel(dialog)
                                    dialog.dismiss()
                                }
                            }

                            PrintMaster.SEPARATE -> {
                                printer.printer_connection_type_id = -1
                                PrintMaster.decidePrintFunctionality(
                                    activity,
                                    kot,
                                    kots,
                                    kotTake,
                                    data,
                                    tableName,
                                    person,
                                    custAdd,
                                    printer,
                                    type
                                ) {
                                    listener.onCancel(dialog)
                                    dialog.dismiss()
                                }
                            }

                            PrintMaster.SINGLE -> {
                                printer.printer_connection_type_id = -2
                                PrintMaster.decidePrintFunctionality(
                                    activity,
                                    kot,
                                    kots,
                                    kotTake,
                                    data,
                                    tableName,
                                    person,
                                    custAdd,
                                    printer,
                                    type
                                ) {
                                    listener.onCancel(dialog)
                                    dialog.dismiss()
                                }
                            }

                            PrintMaster.BOTH -> {
                                printer.printer_connection_type_id = -1
                                PrintMaster.decidePrintFunctionality(
                                    activity,
                                    kot,
                                    kots,
                                    kotTake,
                                    data,
                                    tableName,
                                    person,
                                    custAdd,
                                    printer,
                                    type
                                ) {
                                    printer.printer_connection_type_id = -2
                                    PrintMaster.decidePrintFunctionality(
                                        activity,
                                        kot,
                                        kots,
                                        kotTake,
                                        data,
                                        tableName,
                                        person,
                                        custAdd,
                                        printer,
                                        type
                                    ) {
                                        listener.onCancel(dialog)
                                        dialog.dismiss()
                                    }
                                }
                            }
                        }
                    }
                })
                dialog.setOnCancelListener(listener)
                rvRestoSessions.adapter = adapter

                val cancel = dialog.findViewById(R.id.text_cancel) as AppCompatTextView
                cancel.visibility = View.GONE
                cancel.setOnClickListener { //dialog.dismiss()
                }

                val save = dialog.findViewById(R.id.text_ok) as AppCompatTextView
                save.visibility = View.GONE
                save.setOnClickListener {

                }

                Objects.requireNonNull(dialog.window)
                    ?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))

                dialog.window!!.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.window!!.setGravity(Gravity.CENTER)
                dialog.show()
            }
        }

        fun getMatrix(activity: Activity): DisplayMetrics {
            val outMetrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display
                display?.getRealMetrics(outMetrics)
            } else {
                @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
                @Suppress("DEPRECATION") display.getMetrics(outMetrics)
            }
            return outMetrics
        }

        fun cancelDialog(bottomSheetDialog: Any?) {
            if (bottomSheetDialog != null) {
                if (bottomSheetDialog is BottomSheetDialog) bottomSheetDialog.cancel()
                else if (bottomSheetDialog is Dialog) bottomSheetDialog.cancel()
            }
        }

        fun displayActionSnackbar(act: Activity?, message: String, type: Int) {

            try {
                dialogSnack = Dialog(act!!, R.style.PauseDialog)
                dialogSnack!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogSnack!!.setContentView(R.layout.dialog_snack)
                dialogSnack!!.setCancelable(true)
                dialogSnack!!.setCanceledOnTouchOutside(true)
                val lin = dialogSnack!!.findViewById<CardView>(R.id.lin)
                val img = dialogSnack!!.findViewById<ImageView>(R.id.img)

                val txtStandard = dialogSnack!!.findViewById<TextView>(R.id.txt_standard)
                txtStandard.text = message
                when (type) {
                    1 -> {
                        img.setImageResource(R.drawable.ic_check_circle_green)
                        txtStandard.setTextColor(act.resources.getColor(R.color.greenText))
                        lin.setCardBackgroundColor(act.resources.getColor(R.color.color_check))
                    }

                    2 -> {
                        img.setImageResource(R.drawable.ic_error_circle_red)
                        txtStandard.setTextColor(act.resources.getColor(R.color.redText))
                        lin.setCardBackgroundColor(act.resources.getColor(R.color.color_delete))
                    }

                    3 -> {
                        img.setImageResource(R.drawable.ic_warning_triangle_yellow)
                        txtStandard.setTextColor(act.resources.getColor(R.color.yellowText))
                        lin.setCardBackgroundColor(act.resources.getColor(R.color.color_pending))
                    }
                }
                Objects.requireNonNull(dialogSnack!!.window)
                    ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogSnack!!.window!!.setLayout(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )

                dialogSnack!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                dialogSnack!!.window!!.setGravity(Gravity.TOP)
                dialogSnack!!.show()

                dialogDismissTry = 0
                dismissDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun dismissDialog() {
            Handler().postDelayed({
                if (dialogSnack != null) {
                    val vas = dialogSnack!!.window
                    var active = false
                    if (vas != null) {
                        active = vas.isActive
                    }
                    if (vas != null) {
                        if (dialogSnack!!.isShowing) dialogSnack!!.dismiss()
                    }
                }
            }, 2000)
        }

        fun isValidDigit(str: String): Boolean {
            if (!str.contains(".")) return true
            return str.split(".").size < 3 && TextUtils.isDigitsOnly(str.replace(".", ""))
        }

        fun addChips(
            activity: Activity,
            sp: String?,
            notes: List<String?>?,
            chips: ChipGroup,
            txtTitle: AppCompatTextView,
            listener: CompoundButton.OnCheckedChangeListener,
        ) {
            chips.removeAllViews() // Clear existing chips before adding new ones

            val itemsSet = HashSet<String>() // Use a HashSet to track added items
            if (!sp.isNullOrEmpty() && sp != "[]" && sp != "null") {
                try {
                    val cleanedSp = sp.replace("null,", "").replace(",null", "").replace("[", "").replace("]", "")
                    val items = JSONArray("[$cleanedSp]") // Ensure valid JSON format

                    txtTitle.text = activity.resources.getString(R.string.select_special_instruction)

                    for (i in 0 until items.length()) {
                        val str = items.getString(i).trim()
                        if (str.isNotEmpty()) {
                            itemsSet.add(str)
                            val chip = createChip(activity, str, listener)
                            chips.addView(chip)
                            if (!notes.isNullOrEmpty() && notes.contains(str)) {
                                chip.isChecked = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Add missing `notes` values that are not in `sp`
            notes?.forEach { note ->
                val cleanNote = note?.trim()
                if (!cleanNote.isNullOrEmpty() && !itemsSet.contains(cleanNote)) {
                    val chip = createChip(activity, cleanNote, listener)
                    chips.addView(chip)
                    chip.isChecked = true
                    itemsSet.add(cleanNote) // Avoid duplicates
                }
            }
        }

        // Helper function to create a Chip
        private fun createChip(activity: Activity, text: String, listener: CompoundButton.OnCheckedChangeListener): Chip {
            return Chip(activity).apply {
                this.text = text
                setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
                isCheckedIconVisible = false
                isCheckable = true
                setTextAppearance(R.style.ChipTextAppearance)
                setOnCheckedChangeListener(listener)
            }
        }



        fun getColors(context: Context, color: Int): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.resources.getColor(color, null)
            } else {
                context.resources.getColor(color)
            }
        }
    }
}