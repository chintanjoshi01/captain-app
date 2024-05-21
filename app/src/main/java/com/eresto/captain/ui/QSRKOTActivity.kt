package com.eresto.captain.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.adapter.DineKOTAdapter
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityQsrBinding
import com.eresto.captain.model.DataInvoice
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.KotInstance
import com.eresto.captain.model.OrderDetailQSR
import com.eresto.captain.model.TakeawayOrder
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.SocketForegroundService
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject

class QSRKOTActivity : BaseActivity() {

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQsrBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        tableName = intent.getStringExtra("table_name").toString()
        binding!!.toolbar.title = tableName
        setSupportActionBar(binding!!.toolbar)
        type = intent.getIntExtra("type", 0)
        val main = intent.getStringExtra("message")!!
        tableId = intent.getIntExtra("table_id", 0)

        binding!!.rvPendingKot.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        handlePendingKotResponse(
            gson.fromJson(
                JSONObject(main)
                    .getJSONObject("order_details").toString(),
                OrderDetailQSR::class.java
            )
        )
        binding!!.swipeContainer.setOnRefreshListener {
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
            intent.putExtra("name", custName)
            intent.putExtra("number", custNumber)
            intent.putExtra("person", custPerson)
            intent.putExtra("address", custAddress)
            intent.putExtra("discount", custDiscount)
            intent.putExtra("gst", custGST)
            intent.putExtra("cust_cat_id", custCatId)
            intent.putExtra("is_free_table", false)
            ViewKOTsActivityResult.launch(intent)
        }
    }

    private val ViewKOTsActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) { // There are no request codes
                if (result.data != null && result.data!!.getBooleanExtra("exit", false)) {
                    val mIntent =
                        Intent(this@QSRKOTActivity, LoginActivity::class.java)
                    startActivity(mIntent)
                    finish()
                } else {
                    getTableDetails()
                }
            }else {
                getTableDetails()
            }
        }

    private fun getTableDetails() {
        val jsonObj = JSONObject()
        jsonObj.put("ca", GET_TABLE_ORDER)
        val cv = JSONObject()
        cv.put("tab_id", tableId)
        cv.put("sid", pref!!.getInt(this@QSRKOTActivity, "sid"))
        jsonObj.put("cv", cv)
        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_TAB)
        setCallBack(object : OnResponseFromServerPOS {
            override fun onResponse(json: String) {
                try {
                    binding!!.swipeContainer.isRefreshing = false
                    val jsonObjData = JSONObject(json)
                    if (jsonObjData.getJSONObject("order_details")
                            .getJSONObject("order")
                            .getInt("tab_status") != 1
                    ) {
                        handlePendingKotResponse(
                            gson.fromJson(
                                JSONObject(json)
                                    .getJSONObject("order_details").toString(),
                                OrderDetailQSR::class.java
                            )
                        )
                    } else {
                        finish()
                    }
                } catch (e: JSONException) {
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
            if (userRole == 2) {
                binding!!.btnCloseOrder.visibility = View.VISIBLE
                binding!!.btnCreateInvoice.visibility = View.VISIBLE

            } else if (userRole == 3) {
                binding!!.btnCloseOrder.visibility = View.VISIBLE
                binding!!.btnCreateInvoice.visibility = View.VISIBLE
            } else {
                binding!!.btnCloseOrder.visibility = View.VISIBLE
                binding!!.btnCreateInvoice.visibility = View.GONE
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
            setAdapter(tabStatus == 3, response)

        } else {
            if (type == KeyUtils.DINEIN || type == KeyUtils.ROOM) {
                finish()
            }
            binding!!.btnCloseOrder.visibility = View.GONE
            binding!!.btnCreateInvoice.visibility = View.GONE
            binding!!.txtNo.visibility = View.VISIBLE
            binding!!.txtNo.text = "NO Kots Available"
        }

        binding!!.btnCloseOrder.setOnClickListener {
            val jsonObj = JSONObject()
            val cv = JSONObject()
            jsonObj.put("ca", CLOSE_ORDER)
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
                        //  if (isUpdateRights) editItemDialog(item)
                    }

                    override fun onKOTEdit(position: Int, kot: KotInstance) {
                        // editKOT(kot, null)
                    }

                    override fun onLongPress(position: Int, kot: KotInstance) {
                        //onKOTEdit(position, kot, null)
                    }

                    override fun onKOTDelete(position: Int, instanceId: String) {

//                        val dialog = Dialog(activity!!, R.style.DialogTheme)
//                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//                        dialog.setCancelable(false)
//                        dialog.setContentView(R.layout.dialog_common_filee)
//                        dialog.show()
//
//                        val title = dialog.findViewById(R.id.text_title) as AppCompatTextView
//                        title.text = resources.getString(R.string.msg_delete_kot)
//
//                        val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//                        no.setOnClickListener {
//                            dialog.dismiss()
//                        }
//                        val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//                        yes.setOnClickListener {
//                            dialog.dismiss()
//                            deletePendingKOT(
//                                instanceId,
//                                position,
//                                menuListSec[0].order.kot_instance!![position]
//                            )
//                        }

                    }

                    override fun onKotItemDelete(
                        position: Int,
                        orderKotId: Int,
                        kot: KotInstance?
                    ) {
//                        val dialog = Dialog(activity!!, R.style.DialogTheme)
//                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//                        dialog.setCancelable(false)
//                        dialog.setContentView(R.layout.dialog_common_filee)
//                        dialog.show()
//
//                        val title = dialog.findViewById(R.id.text_title) as AppCompatTextView
//                        title.text = resources.getString(R.string.msg_delete_item)
//
//                        val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//                        no.setOnClickListener {
//                            dialog.dismiss()
//                        }
//                        val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//                        yes.setOnClickListener {
//                            dialog.dismiss()
//                            deletePendingItem(orderKotId, position, kot!!)
//                        }
                    }
                })

        binding!!.rvPendingKot.adapter = adapter
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
            custPerson,
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
        ncv!!.isVisible = true
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

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun viewNCV(ncv: Int) {
        val bottomSheetDialog =
            BottomSheetDialog(this@QSRKOTActivity, R.style.BottomSheetPostDialogTheme)
        val bottomSheet: View = LayoutInflater.from(this@QSRKOTActivity)
            .inflate(
                R.layout.bottomsheet_view_ncv,
                bottomSheetDialog.findViewById(R.id.bottomSheet)
            )
        bottomSheetDialog.setContentView(bottomSheet)
        bottomSheetDialog.show()
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.peekHeight = metrics.heightPixels

        val regular = bottomSheetDialog.findViewById<TextView>(R.id.txt_regular)!!
        val noCharge = bottomSheetDialog.findViewById<TextView>(R.id.txt_no_charge)!!
        val complimentary = bottomSheetDialog.findViewById<TextView>(R.id.txt_complimentary)!!
        val void = bottomSheetDialog.findViewById<TextView>(R.id.txt_void)!!
        val pad = resources.getDimension(com.intuit.sdp.R.dimen._12sdp).toInt()
        when (ncv) {
            0 -> {
                regular.setBackgroundResource(R.drawable.shape_red_border_round)
                regular.setPadding(pad, pad, pad, pad)
            }

            1 -> {
                noCharge.setBackgroundResource(R.drawable.shape_red_border_round)
                noCharge.setPadding(pad, pad, pad, pad)
            }

            2 -> {
                complimentary.setBackgroundResource(R.drawable.shape_red_border_round)
                complimentary.setPadding(pad, pad, pad, pad)
            }

            3 -> {
                void.setBackgroundResource(R.drawable.shape_red_border_round)
                void.setPadding(pad, pad, pad, pad)
            }
        }
        regular.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_red_border_round)
            noCharge.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setBackgroundResource(R.drawable.shape_grey_border_white)
            void.setBackgroundResource(R.drawable.shape_grey_border_white)
            regular.setPadding(pad, pad, pad, pad)
            setNCV(0)
            bottomSheetDialog.cancel()
        }
        noCharge.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setBackgroundResource(R.drawable.shape_red_border_round)
            complimentary.setBackgroundResource(R.drawable.shape_grey_border_white)
            void.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setPadding(pad, pad, pad, pad)
            setNCV(1)
            bottomSheetDialog.cancel()
        }
        complimentary.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setBackgroundResource(R.drawable.shape_red_border_round)
            void.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setPadding(pad, pad, pad, pad)
            setNCV(2)
            bottomSheetDialog.cancel()
        }
        void.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setBackgroundResource(R.drawable.shape_grey_border_white)
            void.setBackgroundResource(R.drawable.shape_red_border_round)
            void.setPadding(pad, pad, pad, pad)
            setNCV(3)
            bottomSheetDialog.cancel()
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
}