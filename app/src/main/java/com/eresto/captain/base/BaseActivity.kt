package com.eresto.captain.base

import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.eresto.captain.R
import com.eresto.captain.model.*
import com.eresto.captain.ui.LoginActivity
import com.eresto.captain.ui.MenuViewActivity
import com.eresto.captain.ui.TabActivity
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.PrintMaster
import com.eresto.captain.utils.PrintMaster.Companion.BOTH
import com.eresto.captain.utils.PrintMaster.Companion.KITCHENWISEPRINT
import com.eresto.captain.utils.PrintMaster.Companion.SEPARATE
import com.eresto.captain.utils.PrintMaster.Companion.SINGLE
import com.eresto.captain.utils.SocketForegroundService
import com.eresto.captain.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Objects
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

open class BaseActivity : AppCompatActivity() {

    companion object {
        val ERROR = 0
        val CONNECT = 1
        val GET_TABLE = 2
        val KOT = 3
        val GET_TABLE_ORDER = 4
        val CLOSE_ORDER = 5
        val SHOW_PROGRESS = 6
        val HIDE_PROGRESS = 7
    }

    var db: DBHelper? = null
    var pref: Preferences? = null
    var dialog: Dialog? = null
    var isProcessing = false
    var activity: Activity? = null
    var isTablet = false
    var restoCurrency = ""

    var lastReceiver=""
    var currentActivity: AppCompatActivity? = null
    fun EditText.onlyUppercase() {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        filters = arrayOf(InputFilter.AllCaps())
    }

    var serviceIntent: Intent? = null
    var service: SocketForegroundService? = null

    val states =
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_pressed)
        )
    var mOnDialogClick: OnResponseFromServerPOS? = null
var isBroadcastSet=false
    interface OnResponseFromServerPOS {
        fun onResponse(json: String)
    }

    interface OnDialogClick {
        fun onOk()
        fun onCancel()
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
        isTablet = resources.getBoolean(R.bool.isTablets)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (isTablet) {
            displayActionSnackbarBottom(
                this,
                resources.getString(R.string.this_is_mobile_app_you_cant_use_in_tablet),
                3,
                false,
                resources.getString(R.string.download),
                object :
                    BaseActivity.OnDialogClick {
                    override fun onOk() {
                        exitProcess(0)
                    }

                    override fun onCancel() {
                    }
                })
        }

        restoCurrency = pref!!.getStr(applicationContext, KeyUtils.restoCurrency)
        db = DBHelper(this)

        setStatusBarColor()


    }

    override fun onDestroy() {
        // Unregister the broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

     val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == lastReceiver) {
                if (!intent.getStringExtra("error").isNullOrEmpty()) {
                    val message = intent.getStringExtra("error")!!
                    displayActionSnackbarBottom(
                        this@BaseActivity,
                        message,
                        2,
                        false,
                        "Reconnect",
                        object : OnDialogClick {
                            override fun onOk() {
                                pref!!.clearSharedPreference(this@BaseActivity)
                                val bool = db!!.DeleteDB()
                                if (currentActivity is MenuViewActivity) {
                                    val resultIntent = Intent()
                                    resultIntent.putExtra("exit", true)
                                    setResult(Activity.RESULT_OK, resultIntent)
                                } else {
                                    val mIntent =
                                        Intent(this@BaseActivity, LoginActivity::class.java)
                                    startActivity(mIntent)
                                }
                                finish()
                            }

                            override fun onCancel() {
                                finish()
                            }
                        })
                } else {
                    val action = JSONObject(intent.getStringExtra("ua")!!)
                    val sendMessage = action.getInt("aa")
                    when (sendMessage) {
                        SHOW_PROGRESS -> {
                            showOfflineProgressDialog(this@BaseActivity)
                        }

                        HIDE_PROGRESS -> {
                            dismissOfflineProgressDialog()
                        }

                        CONNECT -> {
                            /*App Setting*/
                            dismissOfflineProgressDialog()
                            pref!!.setBool(this@BaseActivity, true, "is_login")
                            val mIntent = Intent(this@BaseActivity, TabActivity::class.java)
                            startActivity(mIntent)
                            finish()
                        }

                        GET_TABLE -> {
                            dismissOfflineProgressDialog()
                            mOnDialogClick!!.onResponse("")
                        }
                        CLOSE_ORDER -> {
                            dismissOfflineProgressDialog()
                            mOnDialogClick!!.onResponse("")
                        }

                        GET_TABLE_ORDER -> {
                            dismissOfflineProgressDialog()
                            mOnDialogClick!!.onResponse(intent.getStringExtra("pv")!!)
                        }

                        KOT -> {

                            val message = JSONObject(intent.getStringExtra("pv")!!).getJSONObject("pv")
                            if (message.has("kot") && message.getJSONObject("kot").length() != 0) {
                                val kot = message.getJSONObject("kot")
                                pref!!.setBool(
                                    this@BaseActivity,
                                    kot.getBoolean("ck"),
                                    "cb_kot"
                                )
                                pref!!.setBool(
                                    this@BaseActivity,
                                    kot.getBoolean("kps"),
                                    "kot_print_submission"
                                )
                                pref!!.setInt(
                                    this@BaseActivity,
                                    kot.getInt("ktp"),
                                    "kot_type_printer"
                                )
                                pref!!.setInt(
                                    this@BaseActivity,
                                    kot.getInt("kdp"),
                                    "kot_default_printer"
                                )
                                pref!!.setInt(
                                    this@BaseActivity,
                                    kot.getInt("kp"),
                                    "kot_copies"
                                )
                                pref!!.setInt(
                                    this@BaseActivity,
                                    kot.getInt("kit_print"),
                                    "kitchen_print"
                                )
                                pref!!.setStr(
                                    this@BaseActivity,
                                    kot.getString("kit_cat_print"),
                                    "kit_cat_print"
                                )
                            }
//                            val gson = Gson()
//                            val response = gson.fromJson(
//                                message.getJSONObject("kot_response").toString(),
//                                SubmitNewOrderKOT::class.java
//                            )

                            printKOT(message.getString("kot_instance_id"), action.getJSONObject("cv").getInt("table_id"), action.getJSONObject("cv").getInt("type"))
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
            val flag = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            window.decorView.systemUiVisibility = 0
        }
    }


    fun showOfflineProgressDialog(context: Context) {
        try {
            if (dialog != null && dialog!!.isShowing) try {
                dialog!!.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                dialog = Dialog(context)
                dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog!!.setContentView(R.layout.dialog_progress)

                val img = dialog!!.findViewById<ImageView>(R.id.img)
                Glide.with(context).asGif().load(R.raw.sync_eresto_gif).into(img)

                Objects.requireNonNull(dialog!!.window)
                    ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog!!.setCancelable(false)
                dialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
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
            if (dialog != null && dialog!!.isShowing) try {
                dialog!!.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                dialog = Dialog(context)
                dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog!!.setContentView(R.layout.dialog_progress)

                val img = dialog!!.findViewById<ImageView>(R.id.img)
                Glide.with(context).asGif().load(R.raw.loading).into(img)

                Objects.requireNonNull(dialog!!.window)
                    ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog!!.setCancelable(false)
                dialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
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

    fun displayActionSnackbarBottom(
        activity: Activity?,
        message: String,
        type: Int,
        showCancel: Boolean,
        okButtonText: String,
        onDialogClick: OnDialogClick
    ) {
        try {
            val dialog = BottomSheetDialog(activity!!)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_snack_bottom)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            val img = dialog.findViewById<ImageView>(R.id.img)

            val txtStandard = dialog.findViewById<TextView>(R.id.txt_standard)
            txtStandard!!.text = message

            val btnAddSession = dialog.findViewById<AppCompatButton>(R.id.btn_add_session)
            val btnCancel = dialog.findViewById<AppCompatButton>(R.id.btn_cancel)
            btnAddSession!!.text = okButtonText
            if (showCancel) {
                btnCancel!!.visibility = View.VISIBLE
            } else {
                btnCancel!!.visibility = View.GONE
            }
            btnAddSession.setOnClickListener {
                onDialogClick.onOk()
                dialog.cancel()
            }
            btnCancel.setOnClickListener {
                onDialogClick.onCancel()
                dialog.cancel()
            }
            when (type) {
                1 -> {
                    img!!.setImageResource(R.drawable.ic_check_circle_fill_green)
                    txtStandard.setTextColor(activity.resources.getColor(R.color.greenText))
                }

                2 -> {
                    img!!.setImageResource(R.drawable.ic_cancel_red_fill)
                    txtStandard.setTextColor(activity.resources.getColor(R.color.colorPrimary))
                }

                3 -> {
                    img!!.setImageResource(R.drawable.ic_warning_yellow_fill)
                    txtStandard.setTextColor(activity.resources.getColor(R.color.yellowText))
                }
            }

            Objects.requireNonNull(dialog.window)
                ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SocketForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun sendMessageToServer(jsonObj: JSONObject,action:String) {
        if (service == null) {
            serviceIntent = Intent(this, SocketForegroundService::class.java)
            serviceIntent!!.putExtra("json", jsonObj.toString())
            ContextCompat.startForegroundService(this, serviceIntent!!)

            service = SocketForegroundService()
        } else {
            service!!.makeConnection(this, jsonObj)
        }
        lastReceiver=action
        SocketForegroundService.CURRENT_ACTION =action

        isBroadcastSet=true
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                broadcastReceiver,
                IntentFilter(action)
            )
    }

    override fun onResume() {
        super.onResume()
        val startTime = Calendar.getInstance().timeInMillis
        try {
            val pauseTime = pref!!.getLng(this, "pause_time")
            if (pauseTime > 0) {
                val difference = startTime - pauseTime
                val min =
                    java.lang.Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(difference))
                if (min >= 60 && pref!!.getBool(this, "is_login")
                ) {
                    displayActionSnackbarBottom(
                        this,
                        "POS not Connected, Connect Again",
                        3,
                        false,
                        "Login",
                        object :
                            OnDialogClick {
                            override fun onOk() {
                                pref!!.clearSharedPreference(this@BaseActivity)
                                val bool = db!!.DeleteDB()
                                val intent =
                                    Intent(this@BaseActivity, LoginActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }

                            override fun onCancel() {
                            }
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
        val cal2 = Calendar.getInstance().timeInMillis
        pref!!.setLng(this, cal2, "pause_time")
    }

    private fun printKOT(kotInstanceId: String, tableId: Int, type: Int) {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val strDate: String = dateFormat.format(date)
        val listItem = ArrayList<ItemQSR>()
        val table = db!!.GetTablesById(tableId)
        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        for (item in list) {
            listItem.add(
                ItemQSR(
                    0,
                    item.id,
                    item.item_name,
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


        val kot =
            KotInstance(
                listItem,
                kotInstanceId,
                "",
                0,
                strDate,
                0,
                false,
                pref!!.getStr(this, KeyUtils.shortName),
                0,
                ""
            )
        db!!.deleteItemOfTable(tableId)
        if (pref!!.getBool(this, "prn_setting")) {

            val printKOTSubmission =
                pref!!.getBool(this, "kot_print_submission")
            val printInvoiceSubmission =
                pref!!.getBool(this, "invoice_print_submission")

            if (printKOTSubmission) {
                val printerList = db!!.GetPrinters()
                if (printerList.isNotEmpty()) {
                    printKOTMain(
                        kot,
                        null,
                        null,
                        null,
                        null,
                        table.tab_label,
                        1, "" ?: "",
                        type,
                        object :
                            OnDialogClick {
                            override fun onOk() {
                                dismissProgressDialog()
                                finish()
                            }

                            override fun onCancel() {
                                dismissProgressDialog()
                                finish()
                            }
                        })
                }
            } else {
                dismissProgressDialog()
                finish()
            }

        } else {
            dismissProgressDialog()
            finish()
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
            if (dataInvoice != null) pref!!.getInt(
                this,
                "invoice_type_printer"
            ) else pref!!.getInt(this, "kot_type_printer")
        if (pref!!.getBool(this, "prn_setting")) {
            val printerList = db!!.GetPrinters()

            if (type == 0) {
                val printSubmission = pref!!.getBool(this, "kot_print_submission")
                if (printSubmission) {
                    val kotType = pref!!.getInt(this, "kot_type_printer")
                    val defaultPrinter = pref!!.getInt(this, "kot_default_printer")
                    val kitchenPrint = pref!!.getInt(this, "kitchen_print")

                    if (kotType == 0) {
                        if (defaultPrinter != 0) {
                            val printer = findPrinter(defaultPrinter, printerList)
                            var flag = 0
                            if (printer != null) {
                                first@ for (print in printerList) {
                                    if (print.printer_name == printer.printer_name && print.printer_connection_type_id == printer.printer_connection_type_id && print.printer_type == printer.printer_type) {
                                        if (orders == null) {
                                            when (kitchenPrint) {
                                                0 -> {
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
                                                    ) {
                                                        if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                                    }
                                                }

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
                                                    ) {
                                                        if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                                    }
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
                                                    ) {
                                                        if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                                    }
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
                                                    ) {
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
                                                        ) {
                                                            if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            PrintMaster.printAllKOTFunction(
                                                this,
                                                orders,
                                                tableName,
                                                print.ip_add,
                                                print.port_add
                                            ) {
                                                if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                            }
                                        }
                                        flag = 1
                                        break@first
                                    }
                                }
                            }
                            if (flag == 0) {
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
                                    ) {
                                        onDialogClick.onOk()
                                    }
                                } else {
                                    Utils.selectPrinter(
                                        this,
                                        orders,
                                        tableName,
                                        printerList
                                    ) {
                                        onDialogClick.onOk()
                                    }
                                }
                            }
                        } else {
                            if (kitchenPrint == KITCHENWISEPRINT) {
                                val printer = PrinterRespo(
                                    -1,
                                    "",
                                    -1,
                                    0,
                                    "",
                                    "",
                                    "",
                                    "",
                                )
                                PrintMaster.decidePrintFunctionality(
                                    this,
                                    kot,
                                    null,
                                    kotTakeaway,
                                    dataInvoice,
                                    tableName,
                                    strPerson,
                                    custAddress,
                                    printer,
                                    orderType
                                ) {
                                    if (it) onDialogClick.onOk() else onDialogClick.onCancel()
                                }
                            } else {
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
                                    ) {
                                        onDialogClick.onOk()
                                    }
                                } else {
                                    Utils.selectPrinter(
                                        this,
                                        orders,
                                        tableName,
                                        printerList
                                    ) {
                                        onDialogClick.onOk()
                                    }
                                }
                            }
                        }
                    } else {

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
                            ) {
                                onDialogClick.onOk()
                            }
                        } else {
                            Utils.selectPrinter(this, orders, tableName, printerList) {
                                onDialogClick.onOk()
                            }
                        }
                    }
                } else {
                    onDialogClick.onOk()
                }
            } else {
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
                    ) {
                        onDialogClick.onOk()
                    }
                } else {
                    Utils.selectPrinter(this, orders, tableName, printerList) {
                        onDialogClick.onOk()
                    }
                }
            }
        }
    }

}