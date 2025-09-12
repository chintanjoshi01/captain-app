package com.eresto.captain.base

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.eresto.captain.R
import com.eresto.captain.data.SocketManager
import com.eresto.captain.databinding.DialogPrinterAndKotSettingBinding
import com.eresto.captain.databinding.DialogSnackBottomBinding
import com.eresto.captain.model.CartItemRow
import com.eresto.captain.model.DataInvoice
import com.eresto.captain.model.InvoiceKot
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.KotInstance
import com.eresto.captain.model.Orders
import com.eresto.captain.model.PrinterRespo
import com.eresto.captain.model.TakeawayOrder
import com.eresto.captain.ui.LoginActivity
import com.eresto.captain.ui.tables.TabActivity
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.FirebaseLogger
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.PrintMaster
import com.eresto.captain.utils.PrintMaster.Companion.BOTH
import com.eresto.captain.utils.PrintMaster.Companion.KITCHENWISEPRINT
import com.eresto.captain.utils.PrintMaster.Companion.SEPARATE
import com.eresto.captain.utils.PrintMaster.Companion.SINGLE
import com.eresto.captain.utils.SocketForegroundService
import com.eresto.captain.utils.Utils
import com.eresto.captain.utils.Utils.Companion.displayActionSnackbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

open class BaseActivity : AppCompatActivity() {

    companion object {
        // --- FIX #2: Add a constant for the 30-minute session timeout ---
        const val SESSION_TIMEOUT_MINUTES = 30L

        val ERROR = 0
        val CONNECT = 1
        val GET_TABLE = 2
        val KOT = 3
        val GET_TABLE_ORDER = 4
        val CLOSE_ORDER = 5
        val SHOW_PROGRESS = 100
        val HIDE_PROGRESS = 7
        val LOGOUT = 8
        val CUSTOMER_DETAILS = 9
        var WARRING = 99
        var TABLE_CLOSED = 98
        var EDIT_KOT = 6
    }

    var db: DBHelper? = null
    var pref: Preferences? = null
    var dialog: Dialog? = null
    var isProcessing = false
    var activity: Activity? = null

    // --- FIX #1: Add a variable to track the currently displayed error dialog ---
    private var errorDialog: DialogInterface? = null

    var restoCurrency = ""
    var persons = 1
    var lastReceiver = ""
    var currentActivity: AppCompatActivity? = null
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    var currentJsonData = JSONObject()
    var mOnDialogClick: OnResponseFromServerPOS? = null
    var isBroadcastSet = false
    val states =
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_pressed)
        )

    interface OnResponseFromServerPOS {
        fun onResponse(json: String)
    }

    interface OnDialogClick {
        fun onOk()
        fun onCancel()
    }

    fun EditText.onlyUppercase() {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        filters = arrayOf(InputFilter.AllCaps())
    }

    fun setCallBack(onDialogClick: OnResponseFromServerPOS) {
        mOnDialogClick = onDialogClick
    }

    fun findPrinter(id: Int, printerList: List<PrinterRespo>): PrinterRespo? {
        for (i in printerList) {
            if (i.id == id) {
                return i
            }
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = this.resources.getColor(R.color.colorPrimary)
        }
        activity = this
        currentActivity = this
        pref = Preferences()

        restoCurrency = pref!!.getStr(applicationContext, KeyUtils.restoCurrency)
        db = DBHelper(this)
        setStatusBarColor()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        // Dismiss any lingering dialogs to prevent window leaks
        errorDialog?.dismiss()
        errorDialog = null
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("BaseActivityReceiver", "Received action: ${intent.action}")

            if (intent.action == SocketForegroundService.ACTION_LOGOUT) {
                pref!!.clearSharedPreference(this@BaseActivity)
                db!!.DeleteDB()
                val i = Intent(this@BaseActivity, LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
                return
            }

            if (intent.action == lastReceiver) {
                if (!intent.getStringExtra("error").isNullOrEmpty()) {
                    _isConnected.postValue(false)

                    // --- FIX #1: Check if an error dialog is already showing before creating a new one ---
                    if (errorDialog != null) {
                        Log.w(
                            "BaseActivityReceiver",
                            "An error dialog is already visible. Ignoring new error."
                        )
                        return
                    }

                    val message = intent.getStringExtra("error")!!
                    val isTryAgain = intent.getBooleanExtra("is_try_again", false)
                    val retryCount = intent.getIntExtra("retryCont", 0)
                    val isShowCancel = intent.getBooleanExtra("isShowCancel", true)
                    val isTableClosed = intent.getBooleanExtra("isTableClosed", false)
                    val buttonName = if (isTableClosed) "Go Back" else {
                        if (isTryAgain) "Retry" else "Reconnect"
                    }

                    displayActionReconnectSnackbarBottom(
                        this@BaseActivity,
                        message,
                        2,
                        isShowCancel,
                        buttonName,
                        object : OnDialogClick {
                            override fun onOk() {
                                try {
                                    if (isTableClosed) {
                                        finish()
                                        return
                                    }
                                    if (isTryAgain && retryCount < 3) {
                                        sendMessageToServer(currentJsonData, lastReceiver)
                                    } else if (isTryAgain) {
                                        dismissProgressDialog()
                                        sendMessageToServer(currentJsonData, lastReceiver)
                                    } else {
                                        pref!!.clearSharedPreference(this@BaseActivity)
                                        db!!.DeleteDB()
                                        val mIntent =
                                            Intent(this@BaseActivity, LoginActivity::class.java)
                                        mIntent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(mIntent)
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    FirebaseLogger.logException(
                                        e,
                                        "displayActionReconnectSnackbarBottom_onOk"
                                    )
                                }
                            }

                            override fun onCancel() {
                                finish()
                            }
                        })
                } else {
                    _isConnected.postValue(true)
                    // --- FIX #1: If we get a success message, dismiss any old error dialog ---
                    errorDialog?.dismiss()
                    errorDialog = null

                    val action = JSONObject(intent.getStringExtra("ua")!!)
                    val sendMessage = action.getInt("aa")
                    when (sendMessage) {
                        SHOW_PROGRESS -> showOfflineProgressDialog(this@BaseActivity)
                        HIDE_PROGRESS -> dismissOfflineProgressDialog()
                        CONNECT -> {
                            dismissOfflineProgressDialog()
                            pref!!.setBool(this@BaseActivity, true, "is_login")
                            val mIntent = Intent(this@BaseActivity, TabActivity::class.java)
                            startActivity(mIntent)
                            finish()
                        }

                        GET_TABLE -> {
                            dismissOfflineProgressDialog()
                            mOnDialogClick?.onResponse("")
                        }

                        CLOSE_ORDER -> {
                            dismissOfflineProgressDialog()
                            mOnDialogClick?.onResponse("")
                        }

                        GET_TABLE_ORDER -> {
                            dismissOfflineProgressDialog()
                            mOnDialogClick?.onResponse(intent.getStringExtra("pv")!!)
                        }

                        KOT, EDIT_KOT -> {
                            dismissProgressDialog()
                            finish()
                        }

                        LOGOUT -> {
                            dismissOfflineProgressDialog()
                            pref!!.clearSharedPreference(this@BaseActivity)
                            db!!.DeleteDB()
                            val i = Intent(this@BaseActivity, LoginActivity::class.java)
                            i.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(i)
                            mOnDialogClick?.onResponse(intent.getStringExtra("pv")!!)
                        }
                    }
                }
            }
        }
    }

    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = resources.getColor(R.color.color_white)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }
    }

    fun showOfflineProgressDialog(context: Context) {
        try {
            if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
            dialog = Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_progress)
                val img = findViewById<ImageView>(R.id.img)
                Glide.with(context).asGif().load(R.raw.sync_eresto_gif).into(img)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCancelable(false)
                show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissOfflineProgressDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            dialog = null
        }
    }

    fun showProgressDialog(context: Context) {
        try {
            if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
            dialog = Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_progress)
                val img = findViewById<ImageView>(R.id.img)
                Glide.with(context).asGif().load(R.raw.loading).into(img)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCancelable(false)
                show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissProgressDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            dialog = null
        }
    }
    /**
     * Main entry point. Chooses and displays the correct dialog type based on the device.
     * This is a safe, robust function that uses View Binding.
     *
     * @param activity The context. Can be null, and the function will safely exit.
     * @param message The message to display in the dialog.
     * @param type An integer (1: success, 2: error, 3: warning) to determine the icon and color.
     * @param showCancel Whether to show the cancel button.
     * @param okButtonText The text for the primary action button.
     * @param onDialogClick The callback interface for button clicks.
     */
    fun displayActionSnackbarBottom(
        activity: Activity?,
        message: String,
        type: Int,
        showCancel: Boolean,
        okButtonText: String,
        onDialogClick: OnDialogClick
    ) {
        // FIX: Add a safety check to prevent crashes if the activity is null or finishing.
        if (activity == null || activity.isFinishing) {
            return
        }

        try {
            // 1. Inflate the layout using the generated View Binding class.
            val binding = DialogSnackBottomBinding.inflate(activity.layoutInflater)
            val isTablet = activity.resources.getBoolean(R.bool.isTablet)
            val dialog: DialogInterface

            if (isTablet) {
                // --- TABLET IMPLEMENTATION: Use a standard, centered Dialog ---
                val modalDialog = Dialog(activity, R.style.App_Dialog_Alert)
                modalDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                modalDialog.setContentView(binding.root) // Use the binding's root view
                modalDialog.setCancelable(false)
                modalDialog.setCanceledOnTouchOutside(false)
                dialog = modalDialog
            } else {
                // --- PHONE IMPLEMENTATION: Use a BottomSheetDialog ---
                val bottomSheet = BottomSheetDialog(activity)
                bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE)
                bottomSheet.setContentView(binding.root) // Use the binding's root view
                bottomSheet.setCancelable(false)
                bottomSheet.setCanceledOnTouchOutside(false)
                bottomSheet.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog = bottomSheet
            }

            // 2. Pass the binding object to the worker function to set up the logic.
            setupActionDialogViews(activity, dialog, binding, message, type, showCancel, okButtonText, onDialogClick)

            // 3. Show the correctly configured dialog.
            when (dialog) {
                is Dialog -> dialog.show()
                is BottomSheetDialog -> dialog.show()
            }



            if (isTablet && dialog is Dialog) {
                val window = dialog.window
                if (window != null) {
                    // 1. Get the screen's width
                    val displayMetrics = activity.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels

                    // 2. Calculate 40% of the screen width
                    val dialogWidth = (screenWidth * 0.40).toInt()

                    // 3. Get the current layout params and update them
                    val layoutParams = WindowManager.LayoutParams()
                    layoutParams.copyFrom(window.attributes)
                    layoutParams.width = dialogWidth
                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT // Keep height adaptive

                    // 4. Apply the new layout params to the dialog's window
                    window.attributes = layoutParams
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Worker function that populates the views using the type-safe binding object.
     * This function contains all the shared logic and is independent of the dialog type.
     */
    private fun setupActionDialogViews(
        activity: Activity,
        dialog: DialogInterface,
        binding: DialogSnackBottomBinding, // Accept the binding object
        message: String,
        type: Int,
        showCancel: Boolean,
        okButtonText: String,
        onDialogClick: OnDialogClick
    ) {
        // Use the binding object to access all views directly and safely.
        binding.apply {
            // --- Populate Views ---
            txtStandard.text = message
            btnAddSession.text = okButtonText
            btnCancel.visibility = if (showCancel) View.VISIBLE else View.GONE

            // --- Set UI based on `type` ---
            when (type) {
                1 -> { // Success
                    img.setImageResource(R.drawable.ic_check_circle_fill_green)
                    // FIX: Use ContextCompat for backward compatibility and correctness.
                    txtStandard.setTextColor(ContextCompat.getColor(activity, R.color.greenText))
                }
                2 -> { // Error
                    img.setImageResource(R.drawable.ic_cancel_red_fill)
                    txtStandard.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary))
                }
                3 -> { // Warning
                    img.setImageResource(R.drawable.ic_warning_yellow_fill)
                    txtStandard.setTextColor(ContextCompat.getColor(activity, R.color.yellowText))
                }
            }

            // --- Set Click Listeners ---
            btnAddSession.setOnClickListener {
                onDialogClick.onOk()
                dialog.dismiss()
            }
            btnCancel.setOnClickListener {
                onDialogClick.onCancel()
                dialog.dismiss()
            }
        }
    }

    /**
     * Main entry point for showing a reconnect/action dialog.
     * It checks if the device is a tablet and shows a modal Dialog,
     * otherwise, it shows a BottomSheetDialog.
     */
    fun displayActionReconnectSnackbarBottom(
        activity: Activity?,
        message: String,
        type: Int, // Parameter is preserved even if unused in the original snippet
        showCancel: Boolean,
        okButtonText: String,
        onDialogClick: OnDialogClick
    ) {
        if (activity == null || activity.isFinishing) {
            return // Avoid trying to show a dialog on a destroyed activity
        }

        try {
            _isConnected.postValue(false)
            val isTablet = activity.resources.getBoolean(R.bool.isTablet)
            val dialogView =
                activity.layoutInflater.inflate(R.layout.dialog_snack_bottom_reconnect, null, false)

            val dialog: DialogInterface // Use the common interface

            if (isTablet) {
                // --- TABLET IMPLEMENTATION: Use a standard, centered Dialog ---
                val modalDialog = Dialog(activity, R.style.App_Dialog_Alert)
                modalDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                modalDialog.setContentView(dialogView)
                modalDialog.setCancelable(false)
                modalDialog.setCanceledOnTouchOutside(false)

                // Assign to the class member variable
                this.errorDialog = modalDialog
                dialog = modalDialog

            } else {
                // --- PHONE IMPLEMENTATION: Use a BottomSheetDialog ---
                val bottomSheet = BottomSheetDialog(activity)
                bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE)
                bottomSheet.setContentView(dialogView)
                bottomSheet.setCancelable(false)
                bottomSheet.setCanceledOnTouchOutside(false)

                // Style the bottom sheet
                bottomSheet.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                bottomSheet.window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )

                // Assign to the class member variable
                this.errorDialog = bottomSheet
                dialog = bottomSheet
            }

            // Setup the shared logic for both dialog types
            setupReconnectDialogViewsAndLogic(
                dialog,
                dialogView,
                message,
                showCancel,
                okButtonText,
                onDialogClick
            )

            // Show the dialog
            when (dialog) {
                is Dialog -> dialog.show()
                is BottomSheetDialog -> dialog.show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Worker function that contains the shared UI logic for the reconnect dialog.
     * It's agnostic to whether its container is a Dialog or a BottomSheetDialog.
     */
    private fun setupReconnectDialogViewsAndLogic(
        dialog: DialogInterface,
        dialogView: View,
        message: String,
        showCancel: Boolean,
        okButtonText: String,
        onDialogClick: OnDialogClick
    ) {
        // --- View Initializations (using the inflated dialogView) ---
        val txtStandard = dialogView.findViewById<TextView>(R.id.txt_standard)
        val btnAddSession = dialogView.findViewById<AppCompatButton>(R.id.btn_add_session)
        val btnCancel = dialogView.findViewById<AppCompatButton>(R.id.btn_cancel)

        // --- Populate Views ---
        txtStandard.text = message
        btnAddSession.text = okButtonText
        btnCancel.visibility = if (showCancel) View.VISIBLE else View.GONE

        // --- Set Click Listeners ---
        btnAddSession.setOnClickListener {
            errorDialog = null // Clean up the reference
            onDialogClick.onOk()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            errorDialog = null // Clean up the reference
            onDialogClick.onCancel()
            dialog.dismiss()
        }
    }

    fun sendMessageToServer(jsonObj: JSONObject, action: String) {
        val serviceIntent = Intent(this, SocketForegroundService::class.java)
        Log.d("BaseActivity", "Sending message for action '$action': $jsonObj")
        serviceIntent.putExtra("json", jsonObj.toString())
        ContextCompat.startForegroundService(this, serviceIntent)

        currentJsonData = jsonObj
        lastReceiver = action
        SocketForegroundService.CURRENT_ACTION = action
        isBroadcastSet = true
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(action)
        )
    }

    override fun onResume() {
        super.onResume()
        _isConnected.value = SocketManager.getInstance(this).isConnected()

        // --- FIX #2: Updated session timeout logic ---
        val currentTime = System.currentTimeMillis()
        try {
            val pauseTime = pref!!.getLng(this, "pause_time")
            if (pauseTime > 0) {
                val difference = currentTime - pauseTime
                val minutesPassed = TimeUnit.MILLISECONDS.toMinutes(difference)

                // Check against the 30-minute constant
                if (minutesPassed >= SESSION_TIMEOUT_MINUTES && pref!!.getBool(this, "is_login")) {
                    displayActionSnackbarBottom(
                        this,
                        "Your session has expired. Please log in again.",
                        3, // Warning type
                        false,
                        "Login",
                        object : OnDialogClick {
                            override fun onOk() {
                                // Perform a full logout and redirect
                                pref!!.clearSharedPreference(this@BaseActivity)
                                db!!.DeleteDB()
                                val intent = Intent(this@BaseActivity, LoginActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }

                            override fun onCancel() {}
                        })
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onPause()
        // --- FIX #2: Save the current time for session tracking ---
        val currentTime = System.currentTimeMillis()
        pref!!.setLng(this, currentTime, "pause_time")
    }

    private fun printKOT(kotInstanceId: String, tableId: Int, type: Int) {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val strDate: String = dateFormat.format(date)
        val listItem = ArrayList<ItemQSR>()
        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        for (item in list) {
            listItem.add(
                ItemQSR(
                    0,
                    item.id,
                    item.item_name.uppercase(Locale.US),
                    strDate,
                    item.item_price,
                    item.qty,
                    item.item_short_name,
                    0,
                    item.notes,
                    0,
                    0,
                    item.kitchen_cat_id,
                    item.kot_ncv,
                    item.item_tax_amt, "",
                    item.item_amt
                )
            )
        }

        db!!.deleteItemOfTable(tableId)
        if (pref!!.getBool(this, "prn_setting")) {
            val printKOTSubmission = pref!!.getBool(this, "kot_print_submission")
            if (printKOTSubmission) {
                val printerList = db!!.GetPrinters()
                if (printerList.isNotEmpty()) {
                    dismissProgressDialog()
                    val resultIntent = Intent()
                    resultIntent.putExtra("exit", true)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                persons = 1
            } else {
                dismissProgressDialog()
                finish()
            }
        } else {
            dismissProgressDialog()
            finish()
        }
    }

    fun dialogDefaultPrinterSetting(context: Context) {
        val binding = DialogPrinterAndKotSettingBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()
        dialog.setCancelable(false)
        var kotTypePrinter = pref!!.getInt(context, "kot_type_printer")
        var kotDefaultPrinter = pref!!.getInt(context, "kot_default_printer")
        var kotDesignTypePrinter = pref!!.getInt(context, "kot_design_type")
        if (kotDesignTypePrinter == 0) {
            kotDesignTypePrinter = 1
        }
        binding.cbKotPrintSubmission.isChecked = pref!!.getBool(context, "kot_print_submission")

        val designPrinterType: Array<String> = resources.getStringArray(R.array.printer_design_type)
        binding.kotPrinterDesignType.setText(designPrinterType[kotDesignTypePrinter - 1])
        val defaultPrinterType: Array<String> = resources.getStringArray(R.array.printer_type)
        binding.kotPrinterType.setText(
            defaultPrinterType[pref!!.getInt(
                context,
                "kot_type_printer"
            )]
        )

        val printerList = db!!.GetPrinters()
        val defaultPrinter: Array<String> = Array(printerList.size) { "" }
        for (i in printerList.indices) {
            defaultPrinter[i] = printerList[i].printer_name
        }
        binding.kotDefaultPrinter.setText(
            if (pref!!.getInt(context, "kot_default_printer") != 0) {
                findPrinter(
                    pref!!.getInt(context, "kot_default_printer"),
                    printerList
                )?.printer_name
            } else ""
        )
        setupAutoCompleteTextView(context, binding.kotDefaultPrinter, defaultPrinter) { _, result ->
            kotDefaultPrinter = printerList[result.position].id
        }
        setupAutoCompleteTextView(
            context,
            binding.kotPrinterType,
            defaultPrinterType
        ) { _, result ->
            kotTypePrinter = result.position
        }
        setupAutoCompleteTextView(
            context,
            binding.kotPrinterDesignType,
            designPrinterType
        ) { _, result ->
            kotDesignTypePrinter = result.position + 1
        }

        binding.cbKitchenCat.setOnCheckedChangeListener { _, isChecked ->
            binding.linKitchenCat.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.cbKitchenCatSection.setOnCheckedChangeListener { _, isChecked ->
            binding.linPrinterSection.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.txtMinus.setOnClickListener {
            val currentCount = binding.txtKotPrint.text.toString().toIntOrNull() ?: 1
            if (currentCount > 1) {
                binding.txtKotPrint.setText((currentCount - 1).toString())
            }
        }
        binding.txtPlus.setOnClickListener {
            val currentCount = binding.txtKotPrint.text.toString().toIntOrNull() ?: 0
            binding.txtKotPrint.setText((currentCount + 1).toString())
        }
        binding.tvCancel.setOnClickListener { dialog.dismiss() }
        binding.tvSave.setOnClickListener {
            pref!!.setBool(context, true, "cb_kot")
            pref!!.setBool(context, binding.cbKotPrintSubmission.isChecked, "kot_print_submission")
            pref!!.setInt(context, kotTypePrinter, "kot_type_printer")
            pref!!.setInt(context, kotDefaultPrinter, "kot_default_printer")
            pref!!.setInt(context, binding.txtKotPrint.text.toString().toInt(), "kot_copies")
            pref!!.setInt(context, kotDesignTypePrinter, "kot_design_type")
            pref!!.setBool(context, true, "isDefaultPrinterSetting")
            displayActionSnackbar(this, "Printer settings has been updated", 1)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.shape_grey_border_white)
    }

    data class AutoCompleteResult(val position: Int, val name: String)

    private fun setupAutoCompleteTextView(
        context: Context,
        autoCompleteTextView: AutoCompleteTextView,
        items: Array<String>,
        onItemSelected: (autoCompleteId: Int, result: AutoCompleteResult) -> Unit
    ) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, items)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = items[position]
            val result = AutoCompleteResult(position, selectedItem)
            onItemSelected(autoCompleteTextView.id, result)
        }
    }

    fun printKOTMain(
        kot: KotInstance?,
        kots: List<InvoiceKot>?,
        kotTakeaway: TakeawayOrder?,
        orders: Orders?,
        dataInvoice: DataInvoice?,
        tableName: String,
        strPerson: Int,
        custAddress: String,
        orderType: Int,
        onDialogClick: OnDialogClick
    ) {
        val type =
            if (dataInvoice != null) pref!!.getInt(this, "invoice_type_printer") else pref!!.getInt(
                this,
                "kot_type_printer"
            )
        val isDefaultSetting = pref!!.getBool(this, "isDefaultPrinterSetting")
        if (pref!!.getBool(this, "prn_setting")) {
            val printerList = db!!.GetPrinters()
            if (type == 0) {
                val printSubmission = pref!!.getBool(this, "kot_print_submission")
                if (printSubmission) {
                    val kotType = pref!!.getInt(this, "kot_type_printer")
                    val defaultPrinterId = pref!!.getInt(this, "kot_default_printer")
                    val kitchenPrint = pref!!.getInt(this, "kitchen_print")
                    if (kotType == 0) {
                        if (defaultPrinterId != 0) {
                            val printer = findPrinter(defaultPrinterId, printerList)
                            if (printer != null) {
                                // Logic to print using the found printer
                                if (orders == null) {
                                    handleSinglePrint(
                                        kot,
                                        kots,
                                        kotTakeaway,
                                        dataInvoice,
                                        tableName,
                                        strPerson,
                                        custAddress,
                                        printer,
                                        orderType,
                                        kitchenPrint,
                                        onDialogClick
                                    )
                                } else {
                                    PrintMaster.printAllKOTFunction(
                                        this,
                                        orders,
                                        tableName,
                                        printer.ip_add,
                                        printer.port_add
                                    ) {
                                        if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                    }
                                }
                            } else {
                                // Default printer not found, show selector
                                showPrinterSelector(
                                    kot,
                                    orders,
                                    kotTakeaway,
                                    tableName,
                                    strPerson,
                                    custAddress,
                                    orderType,
                                    printerList,
                                    onDialogClick
                                )
                            }
                        } else {
                            // No default printer set
                            if (kitchenPrint == KITCHENWISEPRINT) {
                                val printer = PrinterRespo(-1, "", -1, 0, "", "", "", "")
                                handleSinglePrint(
                                    kot,
                                    null,
                                    kotTakeaway,
                                    dataInvoice,
                                    tableName,
                                    strPerson,
                                    custAddress,
                                    printer,
                                    orderType,
                                    kitchenPrint,
                                    onDialogClick
                                )
                            } else {
                                showPrinterSelector(
                                    kot,
                                    orders,
                                    kotTakeaway,
                                    tableName,
                                    strPerson,
                                    custAddress,
                                    orderType,
                                    printerList,
                                    onDialogClick
                                )
                            }
                        }
                    } else {
                        // KOT type is not 0, show selector
                        showPrinterSelector(
                            kot,
                            orders,
                            kotTakeaway,
                            tableName,
                            strPerson,
                            custAddress,
                            orderType,
                            printerList,
                            onDialogClick
                        )
                    }
                } else {
                    onDialogClick.onOk()
                }
            } else {
                if (!isDefaultSetting) {
                    dialogDefaultPrinterSetting(this)
                } else {
                    showPrinterSelector(
                        kot,
                        orders,
                        kotTakeaway,
                        tableName,
                        strPerson,
                        custAddress,
                        orderType,
                        printerList,
                        onDialogClick
                    )
                }
            }
        } else {
            onDialogClick.onOk()
        }
    }

    private fun handleSinglePrint(
        kot: KotInstance?,
        kots: List<InvoiceKot>?,
        kotTakeaway: TakeawayOrder?,
        dataInvoice: DataInvoice?,
        tableName: String,
        strPerson: Int,
        custAddress: String,
        printer: PrinterRespo,
        orderType: Int,
        kitchenPrint: Int,
        onDialogClick: OnDialogClick
    ) {
        when (kitchenPrint) {
            0 -> PrintMaster.decidePrintFunctionality(
                this,
                kot,
                kots,
                kotTakeaway,
                dataInvoice,
                tableName,
                strPerson,
                custAddress,
                printer,
                orderType
            ) { if (it) onDialogClick.onOk() else onDialogClick.onCancel() }

            SEPARATE -> {
                printer.printer_connection_type_id = -1
                PrintMaster.decidePrintFunctionality(
                    this,
                    kot,
                    kots,
                    kotTakeaway,
                    dataInvoice,
                    tableName,
                    strPerson,
                    custAddress,
                    printer,
                    orderType
                ) { if (it) onDialogClick.onOk() else onDialogClick.onCancel() }
            }

            SINGLE -> {
                printer.printer_connection_type_id = -2
                PrintMaster.decidePrintFunctionality(
                    this,
                    kot,
                    kots,
                    kotTakeaway,
                    dataInvoice,
                    tableName,
                    strPerson,
                    custAddress,
                    printer,
                    orderType
                ) { if (it) onDialogClick.onOk() else onDialogClick.onCancel() }
            }

            BOTH -> {
                printer.printer_connection_type_id = -1
                PrintMaster.decidePrintFunctionality(
                    this,
                    kot,
                    kots,
                    kotTakeaway,
                    dataInvoice,
                    tableName,
                    strPerson,
                    custAddress,
                    printer,
                    orderType
                ) { success ->
                    if (success) {
                        printer.printer_connection_type_id = -2
                        PrintMaster.decidePrintFunctionality(
                            this,
                            kot,
                            kots,
                            kotTakeaway,
                            dataInvoice,
                            tableName,
                            strPerson,
                            custAddress,
                            printer,
                            orderType
                        ) { if (it) onDialogClick.onOk() else onDialogClick.onCancel() }
                    } else {
                        onDialogClick.onCancel()
                    }
                }
            }
        }
    }

    private fun showPrinterSelector(
        kot: KotInstance?,
        orders: Orders?,
        kotTakeaway: TakeawayOrder?,
        tableName: String,
        strPerson: Int,
        custAddress: String,
        orderType: Int,
        printerList: ArrayList<PrinterRespo>,
        onDialogClick: OnDialogClick
    ) {
        if (orders == null) {
            Utils.selectPrinter(
                this,
                kot,
                null,
                kotTakeaway,
                null,
                tableName,
                strPerson,
                custAddress,
                orderType,
                printerList
            ) { onDialogClick.onOk() }
        } else {
            Utils.selectPrinter(this, orders, tableName, printerList) { onDialogClick.onOk() }
        }
    }
}