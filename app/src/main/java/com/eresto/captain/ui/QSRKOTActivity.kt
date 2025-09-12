package com.eresto.captain.ui

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.eresto.captain.R
import com.eresto.captain.adapter.DineKOTAdapter
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityQsrBinding
import com.eresto.captain.databinding.DialogItemStatusBinding
import com.eresto.captain.model.CartItemRow
import com.eresto.captain.model.DataInvoice
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.KotInstance
import com.eresto.captain.model.MenuData
import com.eresto.captain.model.OrderDetailQSR
import com.eresto.captain.model.TakeawayOrder
import com.eresto.captain.utils.ItemServingStatus
import com.eresto.captain.utils.ItemStatusDetails
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.SocketForegroundService
import com.eresto.captain.utils.SocketForegroundService.Companion.ACTION_CUSTOMER
import com.eresto.captain.utils.Utils.Companion.determineItemStatus
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

class QSRKOTActivity : BaseActivity() {

    private var address: String = ""
    private var strGST: String = ""
    private var strAdd: String = ""
    private var strPerson: Int = 1
    private var strName: String = ""
    private var strContactNumber: String = ""
    private var binding: ActivityQsrBinding? = null
    private var tableId = 0
    private var orderId = 0
    private var type = 0
    private var noc = 0
    private var to = ""
    private var tabStatus = 0
    private var tableName = ""
    private var rights = ""
    private var isAlert = false
    private var invoiceId = ""
    var custName = ""
    var custNumber = ""
    var custPerson = 0
    var custAddress = ""
    var custDiscount = "0"
    var custGST = ""
    lateinit var order: OrderDetailQSR
    private var custCatId = 0
    private var orderNcv = 0
    private var invoiceTempId = 0
    var ncv: MenuItem? = null
    var kotBluetooth: KotInstance? = null
    var kotBluetoothTakeaway: TakeawayOrder? = null
    var invoiceBluetooth: DataInvoice? = null
    var positionBluetooth: Int = 0
    val gson = Gson()

    private val broadcastReceiverCustomerDetails = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CUSTOMER) {
                // Safely get the extras from the intent
                val ua = intent.getStringExtra("ua") ?: "{}"
                val pv = intent.getStringExtra("pv") ?: "{}"

                Log.d("BroadcastReceiver", "Received ua: $ua")
                Log.d("BroadcastReceiver", "Received pv: $pv")

                try {
                    // Parse the "pv" JSON
                    val pvObject = JSONObject(pv)  // Parse the outer "pv" JSON

                    // Extract the nested "pv" object first
                    if (pvObject.has("pv")) {
                        val nestedPvObject = pvObject.getJSONObject("pv")

                        // Check if the "cd" object exists inside the nested "pv" object
                        if (nestedPvObject.has("cd")) {
                            val customer = nestedPvObject.getJSONObject("cd") // Extract "cd" object

                            // Assign values to the variables
                            strName = customer.optString("cn", "") // Customer Name
                            strContactNumber =
                                customer.optString("cm", "") // Customer Contact (as String)
                            strPerson = customer.optInt("nop", 0) // Number of Persons
                            strAdd = customer.optString("ca", "") // Customer Address
                            strGST = customer.optString("cgn", "") // GST Number

                            // Log the extracted customer details
                            Log.e("CustomerDetails", "Name: $strName")
                            Log.e("CustomerDetails", "Number: $strPerson")
                            Log.e("CustomerDetails", "Persons: $custPerson")
                            Log.e("CustomerDetails", "Address: $strAdd")
                            Log.e("CustomerDetails", "GST: $strGST")
                        } else {
                            Log.e("CustomerDetails", "Customer data (cd) is missing in nested pv")
                        }
                    } else {
                        Log.e("CustomerDetails", "Nested 'pv' object is missing")
                    }
                } catch (e: JSONException) {
                    Log.e("BroadcastReceiver", "Error parsing JSON: ${e.message}", e)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQsrBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        tableName = intent.getStringExtra("table_name").toString()
        binding!!.toolbar.title = tableName
        setSupportActionBar(binding!!.toolbar)
        type = intent.getIntExtra("type", 0)
        val main = intent.getStringExtra("message")!!
        Log.e(
            "QSRKOTActivity",
            "onCreate: $main"
        )
        val jsonObjData = JSONObject(main)
        val customer = jsonObjData.getJSONObject("cd")
        Log.e("jlsjdlfdjl", "CD :: $customer")
        strName = customer.getString("cn")
        strContactNumber = customer.getString("cm")
        strPerson = customer.getInt("nop")
        strAdd = customer.getString("ca")
        strGST = customer.getString("cgn")


        tableId = intent.getIntExtra("table_id", 0)
        val spanCount = resources.getInteger(R.integer.kot_grid_columns)
        val layoutManager = if (resources.getBoolean(R.bool.isTablet)) {
            StaggeredGridLayoutManager(
                spanCount,  // number of columns
                StaggeredGridLayoutManager.VERTICAL
            )
        } else {
            GridLayoutManager(this, spanCount)
        }
        binding!!.rvPendingKot.layoutManager = layoutManager

        handlePendingKotResponse(
            gson.fromJson(
                JSONObject(main)
                    .getJSONObject("od").toString(),
                OrderDetailQSR::class.java
            )
        )
        val filter = IntentFilter(ACTION_CUSTOMER)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiverCustomerDetails, filter)

        binding!!.swipeContainer.setOnRefreshListener {
            Log.e("jjladljja", "setOnRefreshListener ")
            getTableDetails()
        }
        binding!!.llMenu.setOnClickListener {
            val classAct = MenuViewActivity::class.java
            val intent = Intent(activity, classAct)
            intent.putExtra("table_name", tableName)
            intent.putExtra("order_ncv", orderNcv)
            intent.putExtra("inv_id", orderId)
            intent.putExtra("type", type)
            intent.putExtra("table_id", tableId)
            intent.putExtra("section_id", 0)
            intent.putExtra("name", strName)
            intent.putExtra("number", strContactNumber)
            intent.putExtra("person", strPerson)
            intent.putExtra("address", strAdd)
            intent.putExtra("discount", custDiscount)
            intent.putExtra("gst", strGST)
            intent.putExtra("cust_cat_id", custCatId)
            intent.putExtra("is_free_table", false)
            ViewKOTsActivityResult.launch(intent)
        }


    }

    private val ViewKOTsActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            /*if (result.resultCode == AppCompatActivity.RESULT_OK) { // There are no request codes
                if (result.data != null && result.data!!.getBooleanExtra("exit", false)) {
                    val mIntent =
                        Intent(this@QSRKOTActivity, LoginActivity::class.java)
                    startActivity(mIntent)
                    finish()
                } else {
                    finish()
                }
            } else {
                finish()
            }*/
            getTableDetails()
        }

    private fun getCustomerDetails() {
        val jsonObj = JSONObject()
        jsonObj.put("ca", CUSTOMER_DETAILS)
        val cv = JSONObject()
        cv.put("tab_id", tableId)
        cv.put("sid", pref!!.getInt(this@QSRKOTActivity, "sid"))
        jsonObj.put("cv", cv)
        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_TAB)
        setCallBack(object : OnResponseFromServerPOS {
            override fun onResponse(json: String) {
                try {
                    val jsonObjData = JSONObject(json)
                    Log.e("jjladljja", "json $json")
                    val customer = jsonObjData.getJSONObject("pv")
                    custName = customer.getString("cn")
                    custNumber = customer.getString("cm")
                    custPerson = customer.getInt("nop")
                    custAddress = customer.getString("ca")
                    custGST = customer.getString("cgn")
                    Log.e("jjladljja", "Name $custName")
                    Log.e("jjladljja", "custNumber $custNumber")
                    Log.e("jjladljja", "custPerson $custPerson")
                    Log.e("jjladljja", "custAddress $custAddress")
                    Log.e("jjladljja", "custDiscount $custDiscount")
                    Log.e("jjladljja", "custGST $custGST")
                    Log.e("jjladljja", "custCatId $custCatId")
                } catch (e: JSONException) {
                    Log.e("jjladljja", "custCatId $e")
                    e.printStackTrace()
                }
            }
        })
    }

    private fun getTableDetails() {
        val jsonObj = JSONObject()
        jsonObj.put("ca", GET_TABLE_ORDER)
        val cv = JSONObject()
        cv.put("tab_id", tableId)
        cv.put("sid", pref!!.getInt(this@QSRKOTActivity, "sid"))
        jsonObj.put("cv", cv)
        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_ORDER)
        setCallBack(object : OnResponseFromServerPOS {
            override fun onResponse(json: String) {
                try {
                    Log.e("jjladljja", "json GET Table $json")
                    binding!!.swipeContainer.isRefreshing = false
                    val jsonObjData = JSONObject(json)
                    val customer = jsonObjData.getJSONObject("cd")
                    custName = customer.getString("cn")
                    custNumber = customer.getString("cm")
                    custPerson = customer.getInt("nop")
                    custAddress = customer.getString("ca")
                    custGST = customer.getString("cgn")
                    if (jsonObjData.getJSONObject("od")
                            .getJSONObject("order")
                            .getInt("tab_status") != 1
                    ) {
                        handlePendingKotResponse(
                            gson.fromJson(
                                JSONObject(json)
                                    .getJSONObject("od").toString(),
                                OrderDetailQSR::class.java
                            )
                        )
                    } else {
                        finish()
                    }
                } catch (e: JSONException) {
                    Log.e("jjladljja", "JSONException ${e.printStackTrace()}")
                    finish()
                    e.printStackTrace()
                }
            }
        })
    }

    private fun handlePendingKotResponse(response: OrderDetailQSR) {
        binding!!.swipeContainer.isRefreshing = false
        if (response != null) {
            val userRole = pref!!.getInt(this@QSRKOTActivity, KeyUtils.roleId)
//            invoiceId = response.order.inv_id
            if (userRole == 2) {
                binding!!.btnCloseOrder.visibility = View.VISIBLE
                binding!!.btnCreateInvoice?.visibility = View.VISIBLE

            } else if (userRole == 3) {
                binding!!.btnCloseOrder.visibility = View.VISIBLE
                binding!!.btnCreateInvoice?.visibility = View.VISIBLE
            } else {
                binding!!.btnCloseOrder.visibility = View.VISIBLE
                binding!!.btnCreateInvoice?.visibility = View.GONE
            }

            if (type == KeyUtils.DINEIN || type == KeyUtils.ROOM) {
                order = response
                order.order.table_name = tableName
                order.order.table_id = tableId
                orderId = order.order.id
                custName =
                    if (order.order.cust_name != null && order.order.cust_name != "null") order.order.cust_name!! else ""
                custNumber =
                    if (order.order.cust_mobile != null && order.order.cust_mobile != "null") order.order.cust_mobile!! else ""
                custPerson = order.order.no_of_person
                custAddress = order.order.cust_add ?: ""
                custDiscount = order.order.disc_percentage.toString()
                custGST = order.order.cust_gst_no ?: ""
                custCatId = order.order.cust_cat_id
                orderNcv = order.order.inv_ncv
                ncv?.setIcon(
                    when (orderNcv) {
                        1 -> R.drawable.ic_no_charge
                        2 -> R.drawable.ic_complementary
                        3 -> R.drawable.ic_void
                        else -> R.drawable.ic_regular
                    }
                )

                binding!!.btnCloseOrder.visibility = View.VISIBLE
            } else {
                binding!!.btnCloseOrder.visibility = View.GONE
            }
            binding!!.txtNo.visibility = View.GONE
            binding!!.rvPendingKot.visibility = View.VISIBLE
            Log.e("jjladljja", "response $response")
            setAdapter(tabStatus == 3, response)

        } else {
            if (type == KeyUtils.DINEIN || type == KeyUtils.ROOM) {
                finish()
            }
            binding!!.btnCloseOrder.visibility = View.GONE
            binding!!.btnCreateInvoice?.visibility = View.GONE
            binding!!.txtNo.visibility = View.VISIBLE
            binding!!.txtNo.text = "NO Kots Available"
        }

        binding!!.btnCloseOrder.setOnClickListener {
            val jsonObj = JSONObject()
            val cv = JSONObject()
            jsonObj.put("ca", CLOSE_ORDER)
            val cd = JSONObject()
            cd.put("cn", strName)
            cd.put("cm", strContactNumber)
            cd.put("cgn", strGST)
            cd.put("ca", address)
            cd.put("nop", strPerson)
            Log.e(
                "dhdshfkh",
                "CD Data :: ${cd.toString()}"
            )
            cv.put("cd", cd)
            cv.put("sid", pref!!.getInt(this, "sid"))
            cv.put("tab_id", tableId)
            jsonObj.put("cv", cv)
            sendMessageToServer(jsonObj, SocketForegroundService.ACTION_ORDER)
            setCallBack(object : OnResponseFromServerPOS {
                override fun onResponse(json: String) {
                    dismissOfflineProgressDialog()
                    finish()
                }
            })
        }
    }


    private fun setAdapter(isEdit: Boolean, menuList: OrderDetailQSR) {
        val listKOT = ArrayList<KotInstance>()
//        val menuListSec = ArrayList<OrderDetailQSR>()
//        for (it in menuList) {
//            menuListSec.add(it)
//        }
//        for (it in menuListSec) {
        val itr = menuList.order.kot_instance!!.iterator()
        while (itr.hasNext()) {
            val t: KotInstance = itr.next()
            var isDelete = true
            for (item in t.item) {
                if (item.soft_delete == 0) {
                    isDelete = false
                }
            }
            if (isDelete) {
                itr.remove()
            }
        }
//        }
        for (kot in menuList.order.kot_instance!!) {
            val kos = kot.copy()
            kos.isExpanded = true
            val listKOTItem = ArrayList<ItemQSR>()
            for (item in kos.item) {
                if (item.soft_delete == 0) {
                    listKOTItem.add(item)
                }
            }
            kos.item = listKOTItem
            if (listKOTItem.size > 0) listKOT.add(kos)
        }
        val adapter =
            DineKOTAdapter(
                this@QSRKOTActivity,
                isEdit,
                true,
                true,
                true,
                listKOT,
                db!!,
                object :
                    DineKOTAdapter.SetOnItemClick {
                    override fun onItemChecked(position: Int, isChecked: Boolean, id: Int) {

                    }

                    override fun onPrintKOT(position: Int, kot: KotInstance) {
                        //kot_default_printer
                        if (pref!!.getBool(this@QSRKOTActivity, "prn_setting")) {
                            printKOTInvoice(
                                position,
                                menuList.order.kot_instance!![position],
                                null,
                                null
                            )
                        } else {
                            displayActionSnackbarBottom(
                                this@QSRKOTActivity,
                                "Printer Setup is Not Available, Contact POS", 2, false,
                                resources.getString(R.string.ok),
                                object :
                                    OnDialogClick {
                                    override fun onOk() {

                                    }

                                    override fun onCancel() {

                                    }
                                })
                        }
                    }

                    override fun onKOTChecked(
                        position: Int,
                        isChecked: Boolean,
                        kot: KotInstance
                    ) { //                    onKOTChecked(isChecked, orderKotId.kot_instance_id)

                    }

                    override fun onItemClick(position: Int, item: ItemQSR) {
// 3. Create the data object.

                        // 1. Get the service time.
                        val serviceTime = db!!.getDeliveryTimeById(item.item_id) ?: 15

                        // 2. Use the updated helper function. It now correctly handles the `created_at` string.
                        val (status, duration) = determineItemStatus(item, serviceTime)
                        val itemDetails = ItemStatusDetails(
                            itemName = item.item_name,
                            quantity = item.qty,
                            status = status,
                            specialInstructions = item.sp_inst,
                            serviceTimeMinutes = serviceTime,
                            durationMinutes = duration
                        )

                        // 4. Show the dialog. This part remains unchanged.
                        showItemStatusDialog(this@QSRKOTActivity, itemDetails)
                    }

                    override fun onKOTEdit(position: Int, kot: KotInstance) {
                        Log.e("jjladljja", "onKOTEdit $kot")
                        editKOT(kot)
                    }

                    override fun onLongPress(position: Int, kot: KotInstance) {
                        //onKOTEdit(position, kot, null)
                    }

                    override fun onKOTDelete(position: Int, instanceId: String) {


                    }

                    override fun onKotItemDelete(
                        position: Int,
                        orderKotId: Int,
                        kot: KotInstance?
                    ) {
                    }
                })

        binding!!.rvPendingKot.adapter = adapter
    }


    private fun editKOT(orderInstance: KotInstance) {
        // 1. Gracefully handle the case where the database is not available.
        val database = db ?: run {
            Toast.makeText(activity, "Database not available.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Create a lookup map for high-performance access. This is much faster
        // than nested loops, especially for large menus.
        val localListMenu: List<MenuData> = database.getCategoriesWithItems()
        val itemLookupMap = localListMenu.flatMap { category ->
            category.items.map { item -> item.id to Pair(category, item) }
        }.toMap()

        val itemsNotFound = mutableListOf<Int>()

        // 3. Use `mapNotNull` to cleanly transform KOT items into CartItemRows.
        // It automatically filters out any items that couldn't be found in the local menu.
        val cartList: List<CartItemRow> = orderInstance.item.mapNotNull { kotItem ->
            val lookupResult = itemLookupMap[kotItem.item_id]

            if (lookupResult != null) {
                val (category, menuItem) = lookupResult

                // 4. Use named arguments to create the CartItemRow. This maps your KOT
                // data to the exact structure your DbHelper's insert method expects.
                Log.e("jjladljja", "kotItem $kotItem")
                CartItemRow(
                    id = kotItem.item_id,
                    kot_id = kotItem.id,
                    item_name = menuItem.item_name,
                    item_short_name = kotItem.short_name ?: "",
                    item_price = kotItem.price,
                    sp_inst = menuItem.sp_inst ?: "[\"\"]", // Default instruction from the menu
                    item_cat_id = category.item_cat_id,
                    table_id = tableId,
                    pre_order_id = "0", // Default value from original code
                    qty = kotItem.qty,
                    kot_ncv = kotItem.kot_ncv,
                    notes = kotItem.sp_inst
                        ?: "[\"\"]", // The KOT-specific instruction goes into the 'notes' field
                    kitchen_cat_id = kotItem.kitchen_cat_id,
                    item_tax = kotItem.item_tax
                        ?: "[]", // Use a valid JSON default like "[]" or "{}"
                    item_tax_amt = kotItem.item_tax_amt ?: "0.0",
                    item_amt = kotItem.item_amt ?: "0.0",
                    sorting = 0, // Provide a default sorting value
                    softDelete = 0, // Default to not deleted
                    isEdit = 1 // Default to not editable
                )
            } else {
                // Log items from the order that were not found in the local DB
                itemsNotFound.add(kotItem.item_id)
                null // This null will be filtered out by mapNotNull
            }
        }

        // --- DEBUGGING LOGS ---
        Log.d(
            "EditKOT",
            "Attempting to replace cart with ${cartList.size} items for tableId $tableId."
        )
        if (itemsNotFound.isNotEmpty()) {
            Log.w("EditKOT", "Could not find these item IDs in local DB: $itemsNotFound")
        }
        // ----------------------

        // Handle the case where the KOT had items, but none exist in the current menu
        if (cartList.isEmpty() && orderInstance.item.isNotEmpty()) {
            Toast.makeText(
                activity,
                "None of the order items are currently available in the menu.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            // 5. THE FIX: Call the new transactional Java method to safely replace the cart items.
            // This deletes old items and inserts new ones, preventing duplicates.
            val success = database.replaceCartItemsForTable(tableId, cartList)

            if (success) {
                // If the database operation was successful, proceed to the next screen.
                launchMenuViewActivity(orderInstance)
            } else {
                Toast.makeText(
                    activity,
                    "Failed to update order in the database.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (ex: Exception) {
            Log.e("EditKOT", "An error occurred while saving the order.", ex)
            Toast.makeText(activity, "A critical error occurred while saving.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Helper function to create the Intent and launch the MenuViewActivity.
     * This improves code organization and reduces clutter in the main function.
     */
    private fun launchMenuViewActivity(orderInstance: KotInstance) {
        val intent = Intent(activity, MenuViewActivity::class.java).apply {
            putExtra("table_name", tableName)
            putExtra("order_ncv", orderNcv)
            putExtra("inv_id", orderId)
            putExtra("type", type)
            putExtra("table_id", tableId)
            putExtra("section_id", 0)
            putExtra("name", strName)
            putExtra("number", strContactNumber)
            putExtra("person", strPerson)
            putExtra("address", strAdd)
            putExtra("discount", custDiscount)
            putExtra("gst", strGST)
            putExtra("cust_cat_id", custCatId)
            putExtra("is_free_table", false)
            putExtra("is_edit_order", true)
            putExtra("kot_id", orderInstance.kot_id)
            putExtra(
                "kot_id_short_name",
                "KOT ${orderInstance.kot_id}-(${orderInstance.short_name}"
            )
            putExtra("kot_order_date", orderInstance.kot_order_date)
        }
        ViewKOTsActivityResult.launch(intent)
    }


    private fun printKOTInvoice(
        position: Int,
        kot: KotInstance?,
        kotTakeaway: TakeawayOrder?,
        invoice: DataInvoice?
    ) {
        kotBluetooth = kot
        kotBluetoothTakeaway = kotTakeaway
        invoiceBluetooth = invoice
        positionBluetooth = position
        printKOTMain(
            kot,
            null,
            kotTakeaway,
            null,
            null,
            tableName,
            strPerson = strPerson,
            custAddress,
            type,
            object :
                OnDialogClick {
                override fun onOk() {

                }

                override fun onCancel() {

                }
            })

    }

    val PERMISSION_BLUETOOTH = 1

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_BLUETOOTH -> {
                    if (kotBluetooth != null || kotBluetoothTakeaway != null || invoiceBluetooth != null) printKOTInvoice(
                        positionBluetooth,
                        kotBluetooth,
                        kotBluetoothTakeaway,
                        invoiceBluetooth
                    )
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.clear()
        menuInflater.inflate(R.menu.top_main_menu, menu)
        ncv = menu.findItem(R.id.ncv)
        ncv!!.isVisible = false
        ncv!!.setIcon(
            when (orderNcv) {
                1 -> R.drawable.ic_no_charge
                2 -> R.drawable.ic_complementary
                3 -> R.drawable.ic_void
                else -> R.drawable.ic_regular
            }
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
//            R.id.ncv -> {
//                viewNCV(orderNcv)
//                true
//            }
            R.id.pre_order -> {
                customerDialog(false)
//                getCustomerDetails()
                val handler = Handler(Looper.getMainLooper())
// Post a task with a delay of 1 second (1000 milliseconds)
                handler.postDelayed({

                    Log.d("HandlerExample", "Task executed after 1 second")
                }, 1000)

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    fun setNCV(value: Int) {
        orderNcv = value
        order.order.inv_ncv = value
        ncv!!.setIcon(
            when (orderNcv) {
                1 -> R.drawable.ic_no_charge
                2 -> R.drawable.ic_complementary
                3 -> R.drawable.ic_void
                else -> R.drawable.ic_regular
            }
        )
        saveCustomerDetails(
            order.order.cust_name ?: "-",
            order.order.cust_mobile ?: "-",
            order.order.no_of_person.toString(),
            order.order.cust_add.toString(),
            order.order.cust_gst_no.toString(), "ncv"
        )
    }

    private fun saveCustomerDetails(
        tvName: String, tvContactNumber: String, tvPerson: String,
        tvAdd: String, tvGst: String, from: String
    ) {
        showProgressDialog(this@QSRKOTActivity)
//            lifecycleScope.launch {
//
//            }
    }

    private fun customerDialog(isEdit: Boolean) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_customer_details)
        val tvContactNumber: TextInputEditText =
            dialog.findViewById(R.id.tv_contact_number) as TextInputEditText
        val tvName: TextInputEditText = dialog.findViewById(R.id.tv_name) as TextInputEditText
        val tvPerson: TextInputEditText = dialog.findViewById(R.id.tv_ppl) as TextInputEditText
        val tvGst: TextInputEditText = dialog.findViewById(R.id.tv_gst) as TextInputEditText
        val tvAddress: TextInputEditText =
            dialog.findViewById(R.id.edt_address) as TextInputEditText

        tvName.setText(strName)
        tvContactNumber.setText(strContactNumber)
        tvPerson.setText(strPerson.toString())
        tvAddress.setText(strAdd)
        tvGst.setText(strGST)

        val btnOk: AppCompatButton = dialog.findViewById(R.id.btn_ok)
        val cancel: AppCompatButton = dialog.findViewById(R.id.tv_cancel)
        tvPerson.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tvPerson.text!!.clear()
            }
        }
        tvContactNumber.tag = "0"
        tvContactNumber.addTextChangedListener {
            if (tvContactNumber.text.toString().length == 10 && tvContactNumber.text.toString()
                    .isDigitsOnly()
            ) {
                if (tvContactNumber.tag.toString() == "0") {

                } else {
                    tvContactNumber.tag = "0"
                }
            }
        }
        btnOk.setOnClickListener {
            strContactNumber =
                if (!tvContactNumber.text.isNullOrBlank()) tvContactNumber.text.toString() else ""
            strName =
                if (!tvName.text.isNullOrBlank()) tvName.text.toString() else "-"
            strPerson = if (!tvPerson.text.isNullOrBlank()) tvPerson.text.toString().toInt() else 1
            strAdd = if (!tvAddress.text.isNullOrBlank()) tvAddress.text.toString() else ""
            strGST = if (!tvGst.text.isNullOrBlank()) tvGst.text.toString() else ""
            dialog.dismiss()
        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }
        Objects.requireNonNull(dialog.window)
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        if (resources.getBoolean(R.bool.isTablet)) {
            val window = dialog.window
            if (window != null) {
                // 1. Get the screen's width
                val displayMetrics = activity!!.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels

                // 2. Calculate 40% of the screen width
                val dialogWidth = (screenWidth * 0.40).toInt()

                // 3. Get the current layout params and update them
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window.attributes)
                layoutParams.width = dialogWidth
                layoutParams.height =
                    WindowManager.LayoutParams.WRAP_CONTENT // Keep height adaptive

                // 4. Apply the new layout params to the dialog's window
                window.attributes = layoutParams
            }
        }

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }


    /**
     * Main entry point. Shows an item's status in a modal (tablet) or bottom sheet (phone).
     * This version is fully refactored to use View Binding.
     *
     * @param activity The context.
     * @param details The data object containing all information to display.
     */
    fun showItemStatusDialog(activity: Activity, details: ItemStatusDetails) {
        if (activity.isFinishing) return

        // 1. Inflate the layout using the generated binding class.
        // This is the core change.
        val binding = DialogItemStatusBinding.inflate(activity.layoutInflater)

        val isTablet = activity.resources.getBoolean(R.bool.isTablet)
        val dialog: DialogInterface

        if (isTablet) {
            val modalDialog = Dialog(activity, R.style.App_Dialog_Alert)
            modalDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            // 2. Set the content view using the binding's root.
            modalDialog.setContentView(binding.root)
            dialog = modalDialog
        } else {
            val bottomSheet = BottomSheetDialog(activity, R.style.BottomSheetPostDialogTheme)
            // 2. Set the content view using the binding's root.
            bottomSheet.setContentView(binding.root)
            dialog = bottomSheet
        }

        // 3. Pass the binding object directly to the worker function.
        setupItemStatusViews(activity, dialog, binding, details)

        // Show the created dialog
        when (dialog) {
            is Dialog -> dialog.show()
            is BottomSheetDialog -> dialog.show()
        }
    }

    /**
     * Worker function that populates the views using the binding object.
     * It is completely type-safe and avoids all findViewById calls.
     */
    private fun setupItemStatusViews(
        activity: Activity,
        dialog: DialogInterface,
        binding: DialogItemStatusBinding, // The binding object is passed in
        details: ItemStatusDetails
    ) {
        // Use the binding object to access all views directly and safely.
        binding.apply {
            // --- Populate Static Data ---
            tvItemName.text = details.itemName
            tvQuantity.text = activity.getString(R.string.item_quantity_format, details.quantity)
            tvServiceTime.text =
                activity.getString(R.string.time_minutes_format, details.serviceTimeMinutes)
            tvDuration.text =
                activity.getString(R.string.time_minutes_format, details.durationMinutes)

            /*if (details.specialInstructions.isNullOrBlank()
                || details.specialInstructions.equals("null")
                || details.specialInstructions.equals("[]")
                || details.specialInstructions.equals("[\"\"]")
            ) {
                tvInstructionsContent.text = activity.getString(R.string.no_special_instructions)
            } else {
                tvInstructionsContent.text = details.specialInstructions
            }*/
            if (details.specialInstructions != null && details.specialInstructions != "" && details.specialInstructions != "[\"\"]") {
                try {
                    val items = JSONArray(details.specialInstructions)
                    var inst = ""
                    for (i in 0 until items.length()) {
                        inst += items.getString(i) + ","
                    }
                    if (inst.isNotEmpty()) {
                        inst = inst.substring(0, inst.length - 1)
                    }
                    tvInstructionsContent.text = inst
                } catch (e: JSONException) {
                    tvInstructionsContent.text = details.specialInstructions
                    e.printStackTrace()
                }
            } else {
                tvInstructionsContent.text = activity.getString(R.string.no_special_instructions)
            }

            // --- Populate Dynamic Data based on Status ---
            val statusColor: Int
            val cardBgColor: Int
            when (details.status) {
                ItemServingStatus.DELIVERED -> {
                    statusColor =
                        ContextCompat.getColor(activity, R.color.color_green_running_border)
                    cardBgColor = ContextCompat.getColor(activity, R.color.color_delivered_bg)
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    tvStatusText.text = activity.getString(R.string.status_delivered)
//                    cardDuration.setCardBackgroundColor(statusColor.withAlpha(40))
                }

                ItemServingStatus.WITHIN_SERVICE_TIME -> {
                    statusColor = ContextCompat.getColor(activity, R.color.yellowText)
                    ivStatusIcon.setImageResource(R.drawable.ic_wthin_time)
                    cardBgColor = ContextCompat.getColor(activity, R.color.color_within_time_bg)
                    tvStatusText.text = activity.getString(R.string.status_within_service_time)
//                    cardDuration.setCardBackgroundColor(statusColor.withAlpha(40))
                }

                ItemServingStatus.BEYOND_SERVICE_TIME -> {
                    statusColor = ContextCompat.getColor(activity, R.color.colorPrimary)
                    cardBgColor = ContextCompat.getColor(activity, R.color.color_beyond_time_bg)
                    ivStatusIcon.setImageResource(R.drawable.ic_service_time)
                    tvStatusText.text = activity.getString(R.string.status_beyond_service_time)
//                    cardDuration.setCardBackgroundColor(statusColor.withAlpha(40))
                }
            }

            // Apply the determined status color to all relevant views
            val statusColorStateList = ColorStateList.valueOf(statusColor)
//            ivStatusIcon.imageTintList = statusColorStateList
            cardDuration.setCardBackgroundColor(cardBgColor)
            tvStatusText.setTextColor(statusColor)
            ivDurationIcon.imageTintList = statusColorStateList
            tvDuration.setTextColor(statusColor)

            // --- Set Listeners ---
            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }
    }
}