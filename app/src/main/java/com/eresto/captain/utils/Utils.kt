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
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
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
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.adapter.SelectPrinterAdapter
import com.eresto.captain.model.DataInvoice
import com.eresto.captain.model.InvoiceKot
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.ItemQSRs
import com.eresto.captain.model.KotInstance
import com.eresto.captain.model.Orders
import com.eresto.captain.model.PrinterRespo
import com.eresto.captain.model.TakeawayOrder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.TimeZone
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
                arrStr.addAll(text.split(",").map { it.trim() }
                    .filter { it.isNotBlank() }) // Proper split and trim
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

        fun getAgoTimeShort(time: String?): String {
            Log.e("dladajla", "kot_order_date :: $time")

            if (time.isNullOrBlank()) {
                return "" // or "N/A"
            }

            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val format2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatDisplay = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

            return try {
                // First format
                val past = format.parse(time)
                val now = Date()

                val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - past.time)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time)
                val hours = TimeUnit.MILLISECONDS.toHours(now.time - past.time)
                val days = TimeUnit.MILLISECONDS.toDays(now.time - past.time)

                when {
                    seconds < 60 -> "$seconds sec"
                    minutes < 60 -> "$minutes min"
                    hours < 24 -> "$hours hr ${minutes % 60 + 1} min"
                    else -> "$days days"
                }
            } catch (j: Exception) {
                try {
                    // Second format (ISO UTC)
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")

                    val past = sdf.parse(time)
                    formatDisplay.format(past) // e.g. "21-08-25"
                } catch (e: Exception) {
                    Log.e("jlfsjfs", "kot_order_date final catch :: $time")
                    "" // fallback if nothing works
                }
            }
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
            if (!sp.isNullOrBlank() && sp != "[]" && sp != "null" && sp != "[\"\"]") {
                try {
                    val cleanedSp = sp.replace("null,", "").replace(",null", "").replace("[", "")
                        .replace("]", "")
                    val items = JSONArray("[$cleanedSp]") // Ensure valid JSON format

                    txtTitle.text =
                        activity.resources.getString(R.string.select_special_instruction)

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

        fun addChipsRounded(
            activity: Activity,
            sp: String?,
            notesList: List<String?>?,
            chips: ChipGroup,
            txtTitle: AppCompatTextView,
            listener: CompoundButton.OnCheckedChangeListener,
        ) {
            // Helper function to parse JSON arrays safely
            fun parseJsonArray(input: String): List<String> {
                return try {
                    JSONArray(input).let { jsonArray ->
                        List(jsonArray.length()) { jsonArray.getString(it).trim() }
                    }
                } catch (e: Exception) {
                    Log.e("addChips", "Error parsing JSON array: $input, $e")
                    emptyList()
                }
            }

            // Process notesList to handle both JSON arrays and plain strings
            val notes = notesList?.flatMap { note ->
                when {
                    note.isNullOrBlank() -> emptyList()
                    note.startsWith("[") && note.endsWith("]") -> parseJsonArray(note) // Handle JSON arrays
                    else -> listOf(
                        note.replace("\\/", "/")
                            .replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                    ) // Treat as plain string
                }
            }?.distinctBy { it.lowercase() } ?: emptyList()

            // Process sp (special instructions) similarly
            val spItems = sp?.let {
                try {
                    JSONArray(it).let { jsonArray ->
                        List(jsonArray.length()) { jsonArray.getString(it).trim() }
                    }
                } catch (e: Exception) {
                    Log.e("addChips", "Error parsing sp: $sp, $e")
                    sp.split(",").map { it.trim() }
                }
            }?.filter { it.isNotEmpty() && it != "null" }?.distinctBy { it.lowercase() }
                ?: emptyList()

            Log.e("addChips", "Processed Notes: $notes")
            Log.e("addChips", "Processed Special Instructions: $spItems")

            // Set the title
            txtTitle.text = activity.resources.getString(R.string.select_special_instruction)

            // Combine, filter, and remove duplicates (case-insensitive)
            val uniqueItems = (notes + spItems).distinctBy { it.lowercase() }

            // Clear existing chips before adding new ones
            chips.removeAllViews()

            uniqueItems.forEach { str ->
                val chip = LayoutInflater.from(
                    ContextThemeWrapper(activity, R.style.Widget_App_Chip_Round_New)
                ).inflate(
                    R.layout.item_chip_choice_round,
                    chips,
                    false
                ) as Chip
                chip.apply {
                    text = str
                    isCheckedIconVisible = false
                    isCheckable = true
                    isChecked = false
                    setOnCheckedChangeListener(listener)
                }
                Log.e("addChips", "Adding chip: $str")
                // Set the checked state based on the notes list
                chips.addView(chip)
                if (notes.any { it.equals(str, ignoreCase = true) }) {
                    Log.e("addChips", "Adding IFF : $str")
                    chip.isChecked = true
                }
            }
        }

        // Helper function to create a Chip
        private fun createChip(
            activity: Activity,
            text: String,
            listener: CompoundButton.OnCheckedChangeListener
        ): Chip {
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

        /**
         * Parses a date string in "yyyy-MM-dd HH:mm:ss" format and converts it to epoch milliseconds.
         *
         * CRITICAL: This function assumes the server time is in UTC. If it's in a different
         * timezone, the ZoneId must be changed to match (e.g., ZoneId.systemDefault()).
         *
         * @param timestampString The date string from the API.
         * @return The epoch milliseconds as a Long, or 0L if parsing fails.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun parseTimestampStringToMillis(timestampString: String?): Long {
            if (timestampString.isNullOrBlank()) {
                return 0L
            }

            // 1. Define the exact format of the incoming string.
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            return try {
                // 2. Parse the string into a LocalDateTime object (which has no timezone).
                val localDateTime = LocalDateTime.parse(timestampString, formatter)

                // 3. Convert it to a ZonedDateTime, assuming the server time is UTC. This is vital for accuracy.
                val zonedDateTime = localDateTime.atZone(ZoneId.of("IST"))

                // 4. Get the final value in milliseconds since the epoch.
                zonedDateTime.toInstant().toEpochMilli()

            } catch (e: DateTimeParseException) {
                // If the string is in an unexpected format, log the error and return 0.
                e.printStackTrace()
                0L
            }
        }


        /**
         * Determines the current serving status and duration of an order item.
         *
         * @param row The data object for the item, which includes a `created_at` string.
         * @param serviceTimeMinutes The maximum allowed service time for this item in minutes.
         * @return A Pair containing the calculated ItemServingStatus and the duration in minutes.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun determineItemStatus(
            row: ItemQSR,
            serviceTimeMinutes: Int
        ): Pair<ItemServingStatus, Int> {
            // Case 1: The item is already marked as delivered. This logic does not change.
            if (row.isd != 0) {
                return Pair(ItemServingStatus.DELIVERED, row.dtd ?: 0)
            }

            // Case 2: The item is NOT delivered. Calculate its current state.
            // *** THIS IS THE ONLY PART THAT CHANGES ***
            val orderTimestampMillis = parseTimestampStringToMillis(row.created_at)

            if (orderTimestampMillis == 0L) {
                // If parsing failed or there's no timestamp, default to a safe state.
                return Pair(ItemServingStatus.WITHIN_SERVICE_TIME, 0)
            }

            // The rest of the logic is identical to before.
            val elapsedMilliseconds = System.currentTimeMillis() - orderTimestampMillis
            val elapsedMinutes = (elapsedMilliseconds / 60000).toInt()

            return if (elapsedMinutes <= serviceTimeMinutes) {
                Pair(ItemServingStatus.WITHIN_SERVICE_TIME, elapsedMinutes)
            } else {
                Pair(ItemServingStatus.BEYOND_SERVICE_TIME, elapsedMinutes)
            }
        }
    }


}

enum class ItemServingStatus {
    DELIVERED,
    WITHIN_SERVICE_TIME,
    BEYOND_SERVICE_TIME
}

// A data class to hold all the information the dialog needs
data class ItemStatusDetails(
    val itemName: String,
    val quantity: Int,
    val status: ItemServingStatus,
    val specialInstructions: String?,
    val serviceTimeMinutes: Int,
    val durationMinutes: Int
)