package com.eresto.captain.ui
//
//import android.app.Activity
//import android.app.Dialog
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.content.res.ColorStateList
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.os.Build
//import android.os.Bundle
//import android.text.Editable
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.Menu
//import android.view.MenuInflater
//import android.view.MenuItem
//import android.view.View
//import android.view.ViewGroup
//import android.view.Window
//import android.view.WindowManager
//import android.widget.AutoCompleteTextView
//import android.widget.CheckBox
//import android.widget.EditText
//import android.widget.LinearLayout
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.AppCompatImageView
//import androidx.appcompat.widget.AppCompatTextView
//import androidx.cardview.widget.CardView
//import androidx.core.text.isDigitsOnly
//import androidx.core.widget.addTextChangedListener
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.bottomsheet.BottomSheetDialog
//import com.google.android.material.button.MaterialButton
//import com.google.android.material.chip.Chip
//import com.google.android.material.chip.ChipGroup
//import com.google.android.material.textfield.TextInputEditText
//import com.google.android.material.textfield.TextInputLayout
//import com.eresto.captain.R
//import com.eresto.captain.databinding.FragmentPendingKotTabBinding
//import com.eresto.captain.RetrofitClient
//import com.eresto.captain.WebAPI
//import com.eresto.captain.model.CartItemRow
//import com.eresto.captain.model.CommonKey
//import com.eresto.captain.model.CommonResult
//import com.eresto.captain.model.CommonResultMSG
//import com.eresto.captain.model.CommonResultWithData
//import com.eresto.captain.model.CustomerData
//import com.eresto.captain.model.DataInvoice
//import com.eresto.captain.model.GetInvoiceById
//import com.eresto.captain.model.GetTables
//import com.eresto.captain.model.GetTakeawayOrderQSR
//import com.eresto.captain.model.ItemQSR
//import com.eresto.captain.model.KotInstance
//import com.eresto.captain.model.MenuData
//import com.eresto.captain.model.OrderDetailQSR
//import com.eresto.captain.model.OrderQSR
//import com.eresto.captain.model.PrinterRespo
//import com.eresto.captain.model.QSRData
//import com.eresto.captain.model.RestoOrderKot
//import com.eresto.captain.model.SubmitNewOrderInvoice
//import com.eresto.captain.model.TakeawayOrder
//import com.eresto.captain.model.rcpt_channels
//import com.eresto.captain.ui.activity.EditOrderReceiptActivity
//import com.eresto.captain.ui.adapter.DineKOTAdapter
//import com.eresto.captain.ui.adapter.PendingTakeAwayAdapter
//import com.eresto.captain.ui.adapter.ReceiptItemAdapter
//import com.eresto.captain.ui.adapter.TableListAdapter
//import com.eresto.captain.ui.reports.ReceiptInfoActivity
//import com.eresto.captain.utils.A5Printer
//import com.eresto.captain.utils.CommonUtils
//import com.eresto.captain.utils.Helper
//import com.eresto.captain.utils.KeyUtils
//import com.eresto.captain.utils.KeyUtils.Companion.DINEIN
//import com.eresto.captain.utils.KeyUtils.Companion.ROOM
//import com.eresto.captain.utils.KeyUtils.Companion.TAKEAWAY
//import com.eresto.captain.utils.OrderInvoiceHelper
//import com.eresto.captain.utils.Utils
//import com.eresto.captain.utils.Utils.Companion.addLayout
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.json.JSONArray
//import org.json.JSONObject
//import retrofit2.await
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Locale
//import java.util.Objects
//import kotlin.math.roundToInt
//
//class KOTTabFragment : BaseFragment() {
//
//    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
//    lateinit var binding: FragmentPendingKotTabBinding
//    var sectionId: String = "0"
//    var type = 0
//    var tabStatus = 0
//    var tableName = ""
//    var tableSeating = ""
//    var orderId = 0
//    lateinit var order: OrderDetailQSR
//    private var tableId = 0
//    private var sessionId = 0
//    var custName = ""
//    var custNumber = ""
//    var custPerson = 0
//    var custAddress = ""
//    var custDiscount = "0"
//    var custGST = ""
//    var successMessage = ""
//    var isNewItemAdded = false
//    var isFirst = 1
//    lateinit var receiptJson: JSONArray
//    lateinit var listReceiptAmount: ArrayList<rcpt_channels>
//    lateinit var orderDetails: List<OrderDetailQSR>
//    var menu: Menu? = null
//    var mCallback: OnMenu? = null
//    var start_date = ""
//    var end_date = ""
//    var message = ""
//    val sdf = SimpleDateFormat(Helper.date_format_DMY, Locale.getDefault())
//
//    var isAlert = false
//    private var printerList = ArrayList<PrinterRespo>()
//    var kotBluetooth: KotInstance? = null
//    var kotBluetoothTakeaway: TakeawayOrder? = null
//    var invoiceBluetooth: DataInvoice? = null
//    var positionBluetooth: Int = 0
//    private var custCatId = 0
//    private var orderNcv = 0
//    private var invoiceTempId = 0
//    private var msgRespo = ""
//    var ncv: MenuItem? = null
//
//    interface OnMenu {
//        fun menuItemClick(orderId: Int)
//        fun onTimeChange(time: String)
//    }
//
//    override fun onAttach(activity: Activity) {
//        super.onAttach(activity)
//        mCallback = try {
//            activity as OnMenu
//        } catch (e: ClassCastException) {
//            throw ClassCastException("$activity must implement TextClicked")
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding =
//            FragmentPendingKotTabBinding.inflate(inflater)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        receiptJson = JSONArray()
//        tableId = requireArguments().getInt("table_id")
//        type = requireArguments().getInt("type")
//        tabStatus = requireArguments().getInt("tabStatus")
//        tableName = requireArguments().getString("table_name").toString()
//        tableSeating = requireArguments().getString("table_seating").toString()
//        orderId = requireArguments().getInt("order_id")
//        orderNcv = requireArguments().getInt("order_ncv")
//        custCatId = requireArguments().getInt("cust_cat_id", 0)
//        isAlert = requireArguments().getBoolean("is_alert", false)
//
//        binding.swipeContainer.setOnRefreshListener {
//            if (CommonUtils.internetAvailable(requireActivity())) {
//                makeDecisionToGetPending(false)
//            }
//        }
//        if (type == TAKEAWAY) {
//            binding.btnCloseOrder.visibility = View.GONE
//            binding.txtMenuPlus.text = resources.getString(R.string.lbl_order)
//        } else {
//            binding.txtMenuPlus.text = resources.getString(R.string.lbl_kot_caps)
//        }
//
//        binding.llMenu.setOnClickListener {
//            val classAct = MenuViewActivity::class.java
//            val intent = Intent(activity, classAct)
//            intent.putExtra("table_name", tableName)
//            intent.putExtra("order_ncv", orderNcv)
//            intent.putExtra("inv_id", orderId)
//            intent.putExtra("type", type)
//            intent.putExtra("table_id", tableId)
//            intent.putExtra("section_id", 0)
//            intent.putExtra("name", custName)
//            intent.putExtra("number", custNumber)
//            intent.putExtra("person", custPerson)
//            intent.putExtra("address", custAddress)
//            intent.putExtra("discount", custDiscount)
//            intent.putExtra("gst", custGST)
//            intent.putExtra("session_id", sessionId)
//            intent.putExtra("cust_cat_id", custCatId)
//            intent.putExtra("is_free_table", false)
//            ViewKOTsActivityResult.launch(intent)
//        }
//
//        // Configure the refreshing colors
//        binding.swipeContainer.setColorSchemeResources(
//            R.color.colorPrimary,
//            R.color.colorPrimary,
//            R.color.colorPrimary,
//            R.color.colorPrimary
//        )
//        binding.rvPendingKot.layoutManager =
//            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
//    }
//
//   /* private fun editItemDialog(item: ItemQSR) {
//        var count = item.qty
//        var ncv = item.kot_ncv
//        val dialog = BottomSheetDialog(requireActivity(), R.style.BottomSheetPostDialogTheme)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setCanceledOnTouchOutside(true)
//        dialog.setContentView(R.layout.dialog_add_menu_item)
//        val btnBack: ImageButton = dialog.findViewById(R.id.btn_back)!!
//        val btnAddInst: ImageView = dialog.findViewById(R.id.btn_add_inst)!!
//        val chipGroup = dialog.findViewById<ChipGroup>(R.id.chips)!!
//        btnBack.setOnClickListener {
//            dialog.cancel()
//        }
//        val edtSpecialInst = dialog.findViewById<EditText>(R.id.edt_special_inst)!!
//        edtSpecialInst.addTextChangedListener {
//            edtSpecialInst.error = null
//        }
//        btnAddInst.setOnClickListener {
//            if (edtSpecialInst.text.toString().isNullOrEmpty()) {
//                edtSpecialInst.error = resources.getString(R.string.enter_special_instruction_name)
//            } else {
//                val chip = Chip(activity)
//                chip.text = edtSpecialInst.text.toString()
//                chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
//                chip.isCheckedIconVisible = false
//                chip.isCheckable = true
//                chip.setTextAppearance(R.style.ChipTextAppearance)
//                chip.setOnCheckedChangeListener { _, _ ->
//                    edtSpecialInst.setText(
//                        Utils.getCheckedIns(
//                            edtSpecialInst.text.toString(),
//                            chipGroup
//                        )
//                    )
//                    edtSpecialInst.setSelection(edtSpecialInst.text.toString().length)
//                }
//                chipGroup.addView(chip)
//                chip.isChecked = true
//                edtSpecialInst.text = null
//            }
//        }
//        val name: AppCompatTextView = dialog.findViewById(R.id.tv_item_name)!!
//        val tvInstruction: TextInputEditText = dialog.findViewById(R.id.tv_name)!!
//
//        val tvQty: AppCompatEditText = dialog.findViewById(R.id.tv_qty)!!
//        tvQty.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                count = if (!tvQty.text.toString().isNullOrEmpty()) tvQty.text.toString()
//                    .toInt() else item.qty
//            }
//        })
//
//        tvQty.setOnFocusChangeListener { v, hasFocus ->
//            if (!hasFocus) {
//                count = if (!tvQty.text.toString().isNullOrEmpty()) {
//                    if (tvQty.text.toString().toInt() < 1) {
//                        tvQty.setText("1")
//                        1
//                    } else {
//                        tvQty.text.toString().toInt()
//                    }
//                } else {
//                    tvQty.setText("1")
//                    1
//                }
//            }
//        }
//        val tvItemCurrency: AppCompatTextView = dialog.findViewById(R.id.tv_item_currency)!!
//        val tvItemPrice: AppCompatEditText = dialog.findViewById(R.id.tv_item_price)!!
//        val linMin: LinearLayout = dialog.findViewById(R.id.lin_min)!!
//        val linAdd: LinearLayout = dialog.findViewById(R.id.lin_add)!!
//        val btnAdd = dialog.findViewById<MaterialButton>(R.id.btn_add)!!
//        val cancel = dialog.findViewById<AppCompatTextView>(R.id.tv_cancel)!!
//        val txtTitle = dialog.findViewById<AppCompatTextView>(R.id.txt_title)!!
//
//        var localInst: List<String>? = null
//        var notes: String? = null
//
//        var instruction = arrayListOf<String>()
//        var inst = ""
//        try {
//            val items = JSONArray(item.sp_inst)
//            for (i in 0 until items.length()) {
//                inst += items.getString(i) + ","
//                instruction.add(items.getString(i))
//            }
//            if (inst.isNotEmpty()) {
//                inst = inst.substring(0, inst.length - 1)
//            }
//            tvInstruction.setText(inst)
//
//        } catch (e: JSONException) {
//            tvInstruction.setText(item.sp_inst)
//            if (!item.sp_inst.isNullOrEmpty()) instruction =
//                item.sp_inst!!.split(",") as ArrayList<String>
//            e.printStackTrace()
//        }
//        val itemInst = db!!.GetItemsById(item.item_id)
//        if (itemInst != null) {
//            Utils.addChips(
//                requireActivity(),
//                itemInst.sp_inst ?: "",
//                instruction,
//                chipGroup,
//                txtTitle
//            ) { _, _ ->
//                tvInstruction.setText(Utils.getCheckedIns(tvInstruction.text.toString(), chipGroup))
//            }
//        } else {
//            txtTitle.visibility = View.GONE
//        }
//
//        tvInstruction.setOnKeyListener { v, keyCode, event ->
//            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                btnAdd.callOnClick()
//            }
//            true
//        }
//
//        name.text = item.item_name
//        tvItemPrice.setText("${item.price}")
//        tvItemCurrency.text = pref!!.getStr(mContext!!, KeyUtils.restoCurrency)
//        tvQty.setText("$count")
//
//        val linOrderNcv: LinearLayout = dialog.findViewById(R.id.lin_order_ncv)!!
//        val btnComplimentary: AppCompatTextView = dialog.findViewById(R.id.btn_complimentary)!!
//        val btnVoid: AppCompatTextView = dialog.findViewById(R.id.btn_void)!!
//        btnComplimentary.setOnClickListener {
//            if (ncv == 2) {
//                ncv = 0
//                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
//                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
//                if (tvItemPrice.text.toString().isEmpty() || tvItemPrice.text.toString() == "0") {
//                    tvItemPrice.setText("${item.price}")
//                    tvItemPrice.setSelection(tvItemPrice.text.toString().length)
//                }
//            } else {
//
//                tvItemPrice.setText("0")
//                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
//                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
//                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
//                btnComplimentary.backgroundTintList = GetColor(R.color.colorSecondary)
//                btnComplimentary.setTextColor(GetColor(R.color.color_white))
//                ncv = 2
//            }
//        }
//        btnVoid.setOnClickListener {
//            if (ncv == 3) {
//                ncv = 0
//                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
//                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
//                if (tvItemPrice.text.toString().isEmpty() || tvItemPrice.text.toString() == "0") {
//                    tvItemPrice.setText("${item.price}")
//                    tvItemPrice.setSelection(tvItemPrice.text.toString().length)
//                }
//            } else {
//                tvItemPrice.setText("0")
//                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
//                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
//                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
//                btnVoid.backgroundTintList = GetColor(R.color.colorSecondary)
//                btnVoid.setTextColor(GetColor(R.color.color_white))
//                ncv = 3
//            }
//        }
//        when (ncv) {
//            0 -> {
//                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
//                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
//                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
//                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
//            }
//
//            2 -> {
//                tvItemPrice.setText("0")
//                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
//                btnComplimentary.backgroundTintList = GetColor(R.color.colorSecondary)
//                btnComplimentary.setTextColor(GetColor(R.color.color_white))
//            }
//
//            3 -> {
//                tvItemPrice.setText("0")
//                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
//                btnVoid.backgroundTintList = GetColor(R.color.colorSecondary)
//                btnVoid.setTextColor(GetColor(R.color.color_white))
//            }
//        }
//        tvItemPrice.addTextChangedListener {
//            if (tvItemPrice.text.toString().isNotEmpty()) {
//                when (ncv) {
//                    2 -> {
//                        btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
//                        btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
//                    }
//
//                    3 -> {
//                        btnVoid.backgroundTintList = GetColor(R.color.light_grey)
//                        btnVoid.setTextColor(GetColor(R.color.colorSecondary))
//                    }
//                }
//                ncv = 0
//            }
//        }
//        if (orderId > 0) {
//            when (orderNcv) {
//                0 -> {
//                    linOrderNcv.visibility = View.VISIBLE
//                }
//
//                1 -> {
//                    linOrderNcv.visibility = View.GONE
//                    tvItemPrice.setText("0")
//                    tvItemPrice.isEnabled = false
//                }
//
//                2 -> {
//                    btnVoid.visibility = View.GONE
//                    tvItemPrice.setText("0")
//                    tvItemPrice.isEnabled = false
//                }
//
//                3 -> {
//                    btnComplimentary.visibility = View.GONE
//                    tvItemPrice.setText("0")
//                    tvItemPrice.isEnabled = false
//                }
//            }
//        }
//        linMin.setOnClickListener {
//            if (count > 1) {
//                count -= 1
//                tvQty.setText("$count")
//                tvQty.setSelection(tvQty.text.toString().length)
//            }
//        }
//        linAdd.setOnClickListener {
//            count += 1
//            tvQty.setText("$count")
//            tvQty.setSelection(tvQty.text.toString().length)
//        }
//
//        btnAdd.text = resources.getString(R.string.update)
//        btnAdd.setOnClickListener {
//            dialog.dismiss()
//            item.qty = count
//            val arrStr = ArrayList<String>()
//            for (chi in 0 until chipGroup.childCount) {
//                val chip = chipGroup.getChildAt(chi) as Chip
//                if (chip.isChecked) {
//                    arrStr.add(chip.text.toString())
//                }
//            }
//            val text = if (arrStr.isNotEmpty()) {
//                arrStr.joinToString(",")
//            } else {
//                ""
//            }
//            if (tvItemPrice.text.toString().isEmpty()) {
//                tvItemPrice.setText("0")
//            }
//            item.sp_inst = text
//            item.price = tvItemPrice.text.toString().toInt()
//            item.kot_ncv = ncv
//            editOrderItem(item)
//        }
//
//        cancel.setOnClickListener {
//            dialog.dismiss()
//        }
//        dialog.window!!.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        dialog.window!!.setGravity(Gravity.CENTER)
//        dialog.show()
//    }*/
//
//    override fun onResume() {
//        super.onResume()
//        printerList = db!!.GetPrinters()
//        var syncData = ""
//        if (db!!.GetKitCat().isEmpty()) {
//            syncData = "kitchen_cat"
//        }
//        if (printerList.isEmpty()) {
//            if (syncData.isNotBlank()) syncData += ","
//            syncData += "printers"
//        }
//        if (pref!!.getInt(mContext!!, "kot_copies") == 0 || pref!!.getInt(
//                mContext!!,
//                "invoice_copies"
//            ) == 0
//        ) {
//            if (syncData.isNotBlank()) syncData += ","
//            syncData += "prints"
//        }
//        if (syncData.isNotEmpty()) {
//            syncData(syncData, object : OnSync {
//                override fun onComplete() {
//                    printerList = db!!.GetPrinters()
//                    makeDecisionToGetPending(true)
//                }
//
//                override fun onCancel() {
//
//                }
//            })
//        } else {
//            makeDecisionToGetPending(true)
//        }
//    }
//
//    private val ViewKOTsActivityResult =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == AppCompatActivity.RESULT_OK) { // There are no request codes
//                successMessage = result.data!!.getStringExtra(KeyUtils.msg)!!
//                isNewItemAdded = true
//                if (CommonUtils.internetAvailable(requireActivity())) {
//                    makeDecisionToGetPending(true)
//                }
//            }
//        }
//
//    private fun makeDecisionToGetPending(dialog: Boolean) {
//        var sync = ""
//        if (db!!.GetPrinters().isEmpty()) {
//            sync = "resto_printers"
//        }
//        if (!pref!!.getBool(mContext!!, "prn_setting") || pref!!.getInt(
//                mContext!!,
//                "kot_copies"
//            ) == 0 || pref!!.getInt(mContext!!, "invoice_copies") == 0
//        ) {
//            if (sync.isNotBlank()) sync += ","
//            sync += "user_printer_settings"
//        }
//        if (pref!!.getStr(mContext!!, "gst_type")
//                .isNotEmpty() && pref!!.getStr(
//                mContext!!,
//                "gst_type"
//            ) != "Unregistered" && pref!!.getStr(mContext!!, "resto_gst_no")
//                .isEmpty()
//        ) {
//            if (sync.isNotEmpty()) sync += ","
//
//            sync = "invoice_settings"
//        }
//        if (db!!.GetInvList().isEmpty()) {
//            if (sync.isNotEmpty()) sync += ","
//
//            sync += "invoice_settings"
//        }
//        if (db!!.GetReceiptChannel().isEmpty()) {
//            if (sync.isNotEmpty()) sync += ","
//
//            sync += "rcpt_channels"
//        }
//
//        if (sync.isNotEmpty()) {
//            syncData(sync, object : OnSync {
//                override fun onComplete() {
//                    if (isAlert) {
//                        getOrderGetByIDData(dialog)
//                    } else {
//                        if (type == DINEIN || type == ROOM) {
//                            getDineInPendingKotData(dialog)
//                        } else {
//                            setDate(dialog)
//                        }
//                    }
//                }
//
//                override fun onCancel() {}
//            })
//        } else {
//            if (isAlert) {
//                getOrderGetByIDData(dialog)
//            } else {
//                if (type == DINEIN || type == ROOM) {
//                    getDineInPendingKotData(dialog)
//                } else {
//                    setDate(dialog)
//                }
//            }
//        }
//    }
//
//    private fun setDate(dialog: Boolean) {
//        val startCalendar = Calendar.getInstance()
//        startCalendar[Calendar.DAY_OF_MONTH] = 1
//        val date = sdf.format(startCalendar.time)
//        start_date = Editable.Factory.getInstance()
//            .newEditable(
//                Helper.changeDateFormat(
//                    Helper.date_format_DMY,
//                    Helper.date_format_YMD,
//                    date
//                )
//            )
//            .toString()
//        val endCalendar = Calendar.getInstance()
//        val dateEnd = sdf.format(endCalendar.time)
//        end_date = Editable.Factory.getInstance()
//            .newEditable(
//                Helper.changeDateFormat(
//                    Helper.date_format_DMY,
//                    Helper.date_format_YMD,
//                    dateEnd
//                )
//            )
//            .toString()
//        getTakeAwayPendingKotData(dialog)
//    }
//
//    private fun editOrderItem(item: ItemQSR) {
//        try {
//            showProgressDialog(requireContext())
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(requireContext())
//                        .editOrderQSR(
//                            Utils.getVirtual(
//                                db!!,
//                                WebAPI.u319693969f88929497808891808676868683
//                            ),
//                            item.id,
//                            item.qty,
//                            item.price,
//                            if (item.sp_inst == null) "" else item.sp_inst,
//                            item.kot_ncv
//                        )
//                        .await()
//
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleEditOrderItemResponse(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleEditOrderItemResponse(response: CommonResult) {
//        if (response.status == 1) {
//            makeDecisionToGetPending(true)
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    private fun getDineInPendingKotData(dialog: Boolean) {
//        try {
//            if (dialog) showProgressDialog(activity)
//            lifecycleScope.launch {
//                try {
//                    var flag = if (type == DINEIN || type == ROOM) 1 else 3
//                    if (tabStatus == 3) flag = 2
//                    if (tabStatus == 3 && isNewItemAdded) flag = 1
//
//                    val call = RetrofitClient.getInstance(mContext)
//                        .getPendingOrderDine(
//                            Utils.getVirtual(db!!, WebAPI.u409fb29d9ca49b93a1a292919c9b85959592),
//                            tableId,
//                            flag,
//                            when (type) {
//                                DINEIN -> {
//                                    1
//                                }
//
//                                ROOM -> {
//                                    3
//                                }
//
//                                else -> {
//                                    2
//                                }
//
//                            }, "DESC"
//                        )
//                    val response = call.await()
//                    withContext(Dispatchers.Main) {
//                        if (dialog) dismissProgressDialog()
//                        handlePendingKotResponse(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            Toast.makeText(mContext!!, ex.printStackTrace().toString(), Toast.LENGTH_LONG).show()
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            Toast.makeText(mContext!!, ex.printStackTrace().toString(), Toast.LENGTH_LONG).show()
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            Toast.makeText(mContext!!, ex.printStackTrace().toString(), Toast.LENGTH_LONG).show()
//            ex.printStackTrace()
//        }
//    }
//
//    private fun getOrderGetByIDData(dialog: Boolean) {
//        try {
//            if (dialog) showProgressDialog(activity)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .getOrderByIdDine(
//                            Utils.getVirtual(
//                                db!!,
//                                WebAPI.GET_ORDER_DETAILS_ON_INV_ID
//                            ), orderId
//                        )
//                        .await()
//                    withContext(Dispatchers.Main) {
//                        if (dialog) dismissProgressDialog()
//                        handlePendingKotResponse(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            Toast.makeText(mContext!!, ex.printStackTrace().toString(), Toast.LENGTH_LONG).show()
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            Toast.makeText(mContext!!, ex.printStackTrace().toString(), Toast.LENGTH_LONG).show()
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            Toast.makeText(mContext!!, ex.printStackTrace().toString(), Toast.LENGTH_LONG).show()
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handlePendingKotResponse(response: CommonKey<QSRData>) {
//        binding.swipeContainer.isRefreshing = false
//        if (response.status > 0) {
//            if (response.data != null) {
//                if (msgRespo.isNotEmpty()) {
//                    displayActionSnackbar(requireActivity(), msgRespo, 1)
//                    msgRespo = ""
//                }
//                if (response.data!!.order_details.isEmpty()) {
//                    binding.rvPendingKot.visibility = View.GONE
//                    binding.txtNo.visibility = View.VISIBLE
//                    binding.txtNo.text = response.message
//                    binding.btnCloseOrder.visibility = View.GONE
//                    return
//                }
//                val userRole = pref!!.getInt(mContext, KeyUtils.roleId)
//                if (userRole == 2) {
//                    binding.btnCloseOrder.visibility = View.VISIBLE
//                        binding.btnCreateInvoice.visibility = View.VISIBLE
//
//                } else if (userRole == 3) {
//                   binding.btnCloseOrder.visibility = View.VISIBLE
//                        binding.btnCreateInvoice.visibility = View.VISIBLE
//                } else {
//                  binding.btnCloseOrder.visibility = View.VISIBLE
//                    binding.btnCreateInvoice.visibility = View.GONE
//                }
//
//                if (type == DINEIN || type == ROOM) {
//                    order = response.data!!.order_details[0]
//                    order.order.table_name = tableName
//                    order.order.table_id = tableId
//                    sessionId = response.data!!.order_details[0].order.session_id
//                    orderId = response.data!!.order_details[0].order.id
//                    custName =
//                        if (response.data!!.order_details[0].order.cust_name != null && response.data!!.order_details[0].order.cust_name != "null") response.data!!.order_details[0].order.cust_name!! else ""
//                    custNumber =
//                        if (response.data!!.order_details[0].order.cust_mobile != null && response.data!!.order_details[0].order.cust_mobile != "null") response.data!!.order_details[0].order.cust_mobile!! else ""
//                    custPerson = response.data!!.order_details[0].order.no_of_person
//                    custAddress = response.data!!.order_details[0].order.cust_add ?: ""
//                    custDiscount = response.data!!.order_details[0].order.disc_percentage.toString()
//                    custGST = response.data!!.order_details[0].order.cust_gst_no ?: ""
//                    custCatId = response.data!!.order_details[0].order.cust_cat_id
//                    orderNcv = response.data!!.order_details[0].order.inv_ncv
//                    ncv!!.setIcon(
//                        when (orderNcv) {
//                            1 -> R.drawable.ic_no_charge
//                            2 -> R.drawable.ic_complementary
//                            3 -> R.drawable.ic_void
//                            else -> R.drawable.ic_regular
//                        }
//                    )
//
//                   binding.btnCloseOrder.visibility = View.VISIBLE
//                    if (mCallback != null) {
//                        mCallback!!.menuItemClick(orderId)
//                        mCallback!!.onTimeChange(Utils.getAgoTimeShort(response.data!!.order_details[0].order.inv_date))
//                    }
//                } else {
//                    binding.btnCloseOrder.visibility = View.GONE
//                }
//                binding.txtNo.visibility = View.GONE
//                binding.rvPendingKot.visibility = View.VISIBLE
//
//                if (tabStatus == 3 && isNewItemAdded) {
//                    tabStatus = 2
//                    isNewItemAdded = false
//                    setCloseEvent()
//                } else if (tabStatus == 3) {
//                    binding.btnCloseOrder.visibility = View.GONE
//                } else {
//                    setCloseEvent()
//                }
//                setAdapter(tabStatus == 3, response.data!!.order_details)
//
//            } else {
//                if (type == DINEIN || type == ROOM) {
//                    requireActivity().finish()
//                }
//                binding.btnCloseOrder.visibility = View.GONE
//                binding.btnCreateInvoice.visibility = View.GONE
//                binding.txtNo.visibility = View.VISIBLE
//                binding.txtNo.text = response.message
//            }
//        } else {
//            if (type == DINEIN || type == ROOM) {
//                requireActivity().finish()
//            }
//            binding.btnCloseOrder.visibility = View.GONE
//            binding.btnCreateInvoice.visibility = View.GONE
//            binding.txtNo.visibility = View.VISIBLE
//            binding.txtNo.text = response.message
//        }
//        setCloseOrder()
//    }
//
//    private fun setCloseEvent() {
//        if (isAlert) {
//            binding.btnCloseOrder.text = resources.getString(R.string.lbl_delete_order)
//            binding.btnCloseOrder.setOnClickListener {
//                getDeleteOrder(orderId)
//            }
//        } else {
//            binding.btnCloseOrder.setOnClickListener {
//                getCloseOrderAll(orderId, "close_table")
//            }
//        }
//    }
//
//    private fun setCloseOrder() {
//        binding.btnCreateInvoice.setOnClickListener {
//
//            listReceiptAmount = ArrayList()
//
//            viewInvoice(order, true)
//        }
//    }
//
//    private fun getTakeAwayPendingKotData(dialog: Boolean) {
//        try {
//            if (dialog) showProgressDialog(activity)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .getPendingOrderTakeaway(
//                            Utils.getVirtual(
//                                db!!,
//                                WebAPI.u409fb29da59d97ab93a98d9b9c8c8b96957f8f8f8c
//                            ), tableId, type, 1, "$start_date 00:00:00", "$end_date 23:59:59"
//                        )
//                        .await()
//                    withContext(Dispatchers.Main) {
//                        if (dialog) dismissProgressDialog()
//                        handlePendingKotResponse(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handlePendingKotResponse(response: GetTakeawayOrderQSR) {
//        binding.swipeContainer.isRefreshing = false
//        if (response.status > 0) {
//            if (response.data != null) {
//                Log.v("Response", "KOT: " + response.data.toString())
//
//                if (response.data.isEmpty()) {
//                    binding.swipeContainer.visibility = View.GONE
//                    binding.txtNo.visibility = View.VISIBLE
//                    binding.txtNo.text = response.message
//                    return
//                }
//                binding.txtNo.visibility = View.GONE
//                binding.swipeContainer.visibility = View.VISIBLE
//                val listKOT = ArrayList<TakeawayOrder>()
//
//                for (order in response.data) {
//                    val orders = order.copy()
//                    orders.isExpanded = true
//                    var isDelivered = 0
//                    val listRestoOrder = ArrayList<RestoOrderKot>()
//                    for (item in order.resto_order_kot) {
//                        val items = item.copy()
//                        orders.kot_instance_id = item.kot_instance_id
//                        if (items.is_delivered == 0) {
//                            isDelivered = 0
//                        }
//                        if (items.soft_delete == 0) {
//                            listRestoOrder.add(items)
//                        }
//                    }
//                    if (listRestoOrder.isNotEmpty()) {
//                        orders.resto_order_kot = listRestoOrder
//                        orders.is_delivered = isDelivered
//                        listKOT.add(orders)
//                    }
//                }
//                val adapter =
//                    PendingTakeAwayAdapter(
//                        this@KOTTabFragment.requireActivity(),
//                        false,
//                        0,
//                        listKOT,
//                        object :
//                            PendingTakeAwayAdapter.SetOnItemClick {
//                            override fun onItemChecked(position: Int, isChecked: Boolean, id: Int) {
//
//                            }
//
//                            override fun onKOTChecked(
//                                position: Int,
//                                isChecked: Boolean,
//                                row: TakeawayOrder
//                            ) {
//                                    getCloseOrderAll(row.id, "close_order")
//
//                            }
//
//                            override fun onItemClick(position: Int, row: RestoOrderKot) {
//
//                            }
//
//                            override fun onPrintKOT(position: Int, kot: TakeawayOrder) {
//
//                                    if (pref!!.getBool(mContext!!, "prn_setting")) {
//                                        printKOTInvoice(position, null, kot, null)
//                                    } else {
//                                        displayPrinterSettingDialog(
//                                            requireActivity(),
//                                            resources.getString(R.string.please_setup_the_kot_print_printer),
//                                            resources.getString(R.string.go_to_settings_printer_management),
//                                            false,
//                                            resources.getString(R.string.ok),
//                                            "",
//                                            object :
//                                                OnDialogClick {
//                                                override fun onOk() {
//
//                                                }
//
//                                                override fun onCancel() {
//
//                                                }
//                                            })
//                                    }
//                            }
//
//                            override fun onPrintInvoice(position: Int, kot: TakeawayOrder) {
//
//                            }
//
//                            override fun onLongPress(position: Int, row: TakeawayOrder) {
//                                    val listKotItem = ArrayList<ItemQSR>()
//                                    for (ki in row.resto_order_kot) {
//                                        listKotItem.add(
//                                            ItemQSR(
//                                                ki.kot_id,
//                                                ki.item_id,
//                                                ki.item_name,
//                                                "",
//                                                ki.price,
//                                                ki.qty,
//                                                "",
//                                                ki.is_delivered,
//                                                ki.sp_inst,
//                                                ki.soft_delete,
//                                                ki.is_edited,
//                                                ki.kitchen_cat_id,
//                                                ki.kot_ncv,
//                                                "",
//                                                "",
//                                                ""
//                                            )
//                                        )
//                                    }
//                                    val item =
//                                        KotInstance(
//                                            listKotItem,
//                                            row.kot_instance_id,
//                                            "",
//                                            row.id,
//                                            "",
//                                            row.is_delivered,
//                                            row.isExpanded,
//                                            "",
//                                            row.soft_delete,
//                                            ""
//                                        )
//                                    onKOTEdit(position, item, row)
//                            }
//
//                            override fun onKOTEdit(position: Int, row: TakeawayOrder) {
//                                val listKotItem = ArrayList<ItemQSR>()
//                                for (ki in row.resto_order_kot) {
//                                    listKotItem.add(
//                                        ItemQSR(
//                                            ki.kot_id,
//                                            ki.item_id,
//                                            ki.item_name,
//                                            "",
//                                            ki.price,
//                                            ki.qty,
//                                            "",
//                                            ki.is_delivered,
//                                            ki.sp_inst,
//                                            ki.soft_delete,
//                                            ki.is_edited,
//                                            ki.kitchen_cat_id,
//                                            ki.kot_ncv,
//                                            "",
//                                            "",
//                                            ""
//                                        )
//                                    )
//                                }
//                                val item =
//                                    KotInstance(
//                                        listKotItem,
//                                        row.kot_instance_id,
//                                        "",
//                                        row.id,
//                                        "",
//                                        row.is_delivered,
//                                        row.isExpanded,
//                                        "",
//                                        row.soft_delete,
//                                        ""
//                                    )
//
//                                custName =
//                                    if (row.cust_name != null && row.cust_name != "null") row.cust_name else ""
//                                custNumber =
//                                    if (row.cust_mobile != null && row.cust_mobile != "null") row.cust_mobile else ""
//                                custPerson = row.no_of_persons
//                                custAddress =
//                                    if (row.cust_add != null && row.cust_add != "null") row.cust_add else ""
//                                custCatId = row.cust_cat_id
//                                editKOT(item, row)
//                            }
//
//                            override fun onKOTDelete(position: Int, row: TakeawayOrder) {
//                                    val dialog = Dialog(activity!!, R.style.DialogTheme)
//                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//                                    dialog.setCancelable(false)
//                                    dialog.setContentView(R.layout.dialog_common_filee)
//                                    dialog.show()
//
//                                    val title =
//                                        dialog.findViewById(R.id.text_title) as AppCompatTextView
//                                    title.text = resources.getString(R.string.msg_delete_kot)
//
//                                    val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//                                    no.setOnClickListener {
//                                        dialog.dismiss()
//                                    }
//                                    val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//                                    yes.setOnClickListener {
//                                        dialog.dismiss()
//                                        deletePendingKOT(row.kot_instance_id, position, row)
//                                    }
//                            }
//
//                            override fun onKotItemDelete(
//                                position: Int,
//                                orderId: Int,
//                                tableId: Int,
//                                id: Int,
//                                row: TakeawayOrder?
//                            ) {
//                                    val dialog = Dialog(activity!!, R.style.DialogTheme)
//                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//                                    dialog.setCancelable(false)
//                                    dialog.setContentView(R.layout.dialog_common_filee)
//                                    dialog.show()
//
//                                    val title =
//                                        dialog.findViewById(R.id.text_title) as AppCompatTextView
//                                    title.text = resources.getString(R.string.msg_delete_item)
//
//                                    val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//                                    no.setOnClickListener {
//                                        dialog.dismiss()
//                                    }
//                                    val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//                                    yes.setOnClickListener {
//                                        dialog.dismiss()
//                                        deletePendingItem(id, position, row!!)
//                                    }
//                            }
//                        })
//                binding.rvPendingKot.adapter = adapter
//            } else {
//                binding.txtNo.visibility = View.VISIBLE
//                binding.txtNo.text = response.message
//                binding.swipeContainer.visibility =
//                    View.GONE
//            }
//        } else {
//            binding.txtNo.visibility = View.VISIBLE
//            binding.txtNo.text = response.message
//            binding.swipeContainer.visibility =
//                View.GONE
//        }
//    }
//
//    private fun moveKOTProcess(
//        kot: KotInstance,
//        table: ArrayList<GetTables>
//    ) {
//        val dialogMove = BottomSheetDialog(mContext!!, R.style.BottomSheetPostDialogTheme)
//        dialogMove.setContentView(R.layout.bottomsheet_movekot)
//        val btnClose = dialogMove.findViewById<AppCompatImageView>(R.id.cancel)
//        val recyclerView = dialogMove.findViewById<RecyclerView>(R.id.recyclerviewTables)
//        recyclerView!!.layoutManager = GridLayoutManager(mContext, 3)
//        btnClose!!.setOnClickListener {
//            dialogMove.dismiss()
//        }
//
//        val itr = table.iterator()
//        while (itr.hasNext()) {
//            val t: GetTables = itr.next()
//            if (t.id == tableId) {
//                itr.remove()
//            } else if (t.tab_status == 3) {
//                itr.remove()
//            } else if (t.tab_status == 5) {
//                itr.remove()
//            }
//        }
//        val adapter = TableListAdapter(this@KOTTabFragment.requireActivity(), table, type, object :
//            TableListAdapter.SetOnItemClick {
//            override fun onItemClick(position: Int, data: GetTables) {
//
//                    dialogMove.cancel()
//                    moveKOT(data.id, orderId, kot.instance)
//            }
//
//            override fun onEdit(position: Int, data: GetTables) {
//
//            }
//        })
//        recyclerView.adapter = adapter
//        dialogMove.setCancelable(false)
//        dialogMove.show()
//    }
//
//    private fun onKOTEdit(position: Int, kot: KotInstance, row: TakeawayOrder?) {
//        val dialog = BottomSheetDialog(requireActivity(), R.style.DialogTheme)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setContentView(R.layout.bottomsheet_kot_edit)
//        val txtName = dialog.findViewById<AppCompatTextView>(R.id.txt_name)
//        val txtSeating = dialog.findViewById<AppCompatTextView>(R.id.txt_seating)
//        val reserve = dialog.findViewById<CardView>(R.id.card_reserve)
//        val print = dialog.findViewById<CardView>(R.id.card_print)
//        val edit = dialog.findViewById<CardView>(R.id.card_edit)
//        val move = dialog.findViewById<CardView>(R.id.card_move)
//        val delete = dialog.findViewById<CardView>(R.id.card_delete)
//        txtName!!.text = "#${kot.instance}-${kot.short_name ?: "--"}"
//        txtSeating!!.text = ""
//        Objects.requireNonNull(dialog.window)
//            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.window!!.setLayout(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.WRAP_CONTENT
//        )
//        reserve!!.visibility = View.GONE
//        if (type == TAKEAWAY) {
//            move!!.visibility = View.GONE
//        }
//        print!!.setOnClickListener {
//             printKOTInvoice(0, kot, null, null)
//
//        }
//        edit!!.setOnClickListener {
//            editKOT(kot, row)
//            dialog.cancel()
//        }
//        move!!.setOnClickListener {
//            dialog.cancel()
//            getTable(kot)
//        }
//
//        delete!!.setOnClickListener {
//            val dialogDelete = Dialog(requireActivity(), R.style.DialogTheme)
//            dialogDelete.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            dialogDelete.setCancelable(false)
//            dialogDelete.setContentView(R.layout.dialog_common_filee)
//            dialogDelete.show()
//
//            val title = dialogDelete.findViewById(R.id.text_title) as AppCompatTextView
//            title.text = resources.getString(R.string.msg_delete_kot)
//
//            val no = dialogDelete.findViewById(R.id.tv_no) as AppCompatTextView
//            no.setOnClickListener {
//                dialogDelete.dismiss()
//            }
//            val yes = dialogDelete.findViewById(R.id.tv_yes) as AppCompatTextView
//            yes.setOnClickListener {
//                dialogDelete.dismiss()
//                deletePendingKOT(kot.kot_instance_id, position, row ?: kot)
//            }
//            dialog.cancel()
//        }
//        dialog.show()
//    }
//
//    private fun moveKOT(movetable_id: Int, order_id: Int, instance: String) {
//        try {
//            showProgressDialog(mContext!!)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext!!)
//                        .moveKot(
//                            Utils.getVirtual(db!!, WebAPI.u319e9ea39088929497),
//                            tableId,
//                            movetable_id,
//                            order_id,
//                            instance
//                        )
//                        .await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleMoveKotResponse(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleMoveKotResponse(response: CommonKey<List<GetTables>>) {
//        if (response.status == 1) {
//            msgRespo = response.message!!
//            db!!.InsertTable(response.data)
//            makeDecisionToGetPending(true)
//        } else {
//            displayActionSnackbar(requireActivity(), response.message!!, 2)
//        }
//    }
//
//    private fun onKOTDelete(kot_id: Int, position: Int, kot: KotInstance) {
//        val dialog = Dialog(requireActivity(), R.style.DialogTheme)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(false)
//        dialog.setContentView(R.layout.dialog_common_filee)
//        dialog.show()
//
//        val title = dialog.findViewById(R.id.text_title) as AppCompatTextView
//        title.text = resources.getString(R.string.msg_delete_item)
//
//        val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//        no.setOnClickListener {
//            dialog.dismiss()
//        }
//        val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//        yes.setOnClickListener {
//            dialog.dismiss()
//            deletePendingItem(kot_id, position, kot)
//        }
//    }
//
//    private fun setAdapter(isEdit: Boolean, menuList: List<OrderDetailQSR>) {
//        val listKOT = ArrayList<KotInstance>()
//        val menuListSec = ArrayList<OrderDetailQSR>()
//        for (it in menuList) {
//            menuListSec.add(it)
//        }
//        for (it in menuListSec) {
//            val itr = it.order.kot_instance!!.iterator()
//            while (itr.hasNext()) {
//                val t: KotInstance = itr.next()
//                var isDelete = true
//                for (item in t.item) {
//                    if (item.soft_delete == 0) {
//                        isDelete = false
//                    }
//                }
//                if (isDelete) {
//                    itr.remove()
//                }
//            }
//        }
//        for (order in menuList) {
//            for (kot in order.order.kot_instance!!) {
//                val kos = kot.copy()
//                kos.isExpanded = true
//                val listKOTItem = ArrayList<ItemQSR>()
//                for (item in kos.item) {
//                    if (item.soft_delete == 0) {
//                        listKOTItem.add(item)
//                    }
//                }
//                kos.item = listKOTItem
//                if (listKOTItem.size > 0) listKOT.add(kos)
//            }
//        }
//        val adapter =
//            DineKOTAdapter(
//                this@KOTTabFragment.requireActivity(),
//                isEdit,
//                true,
//                true,
//                true,
//                listKOT,
//                object :
//                    DineKOTAdapter.SetOnItemClick {
//                    override fun onItemChecked(position: Int, isChecked: Boolean, id: Int) {
//
//                    }
//
//                    override fun onPrintKOT(position: Int, kot: KotInstance) {
//                        //kot_default_printer
//                            if (pref!!.getBool(mContext!!, "prn_setting")) {
//                                printKOTInvoice(
//                                    position,
//                                    menuListSec[0].order.kot_instance!![position],
//                                    null,
//                                    null
//                                )
//                            } else {
//                                displayPrinterSettingDialog(
//                                    requireActivity(),
//                                    resources.getString(R.string.please_setup_the_kot_print_printer),
//                                    resources.getString(R.string.go_to_settings_printer_management),
//                                    false,
//                                    resources.getString(R.string.ok),
//                                    "",
//                                    object :
//                                        OnDialogClick {
//                                        override fun onOk() {
//
//                                        }
//
//                                        override fun onCancel() {
//
//                                        }
//                                    })
//                        }
//                    }
//
//                    override fun onKOTChecked(
//                        position: Int,
//                        isChecked: Boolean,
//                        kot: KotInstance
//                    ) { //                    onKOTChecked(isChecked, orderKotId.kot_instance_id)
//
//                    }
//
//                    override fun onItemClick(position: Int, item: ItemQSR) {
//                        //  if (isUpdateRights) editItemDialog(item)
//                    }
//
//                    override fun onKOTEdit(position: Int, kot: KotInstance) {
//                        editKOT(kot, null)
//                    }
//
//                    override fun onLongPress(position: Int, kot: KotInstance) {
//                       onKOTEdit(position, kot, null)
//                    }
//
//                    override fun onKOTDelete(position: Int, instanceId: String) {
//
//                            val dialog = Dialog(activity!!, R.style.DialogTheme)
//                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//                            dialog.setCancelable(false)
//                            dialog.setContentView(R.layout.dialog_common_filee)
//                            dialog.show()
//
//                            val title = dialog.findViewById(R.id.text_title) as AppCompatTextView
//                            title.text = resources.getString(R.string.msg_delete_kot)
//
//                            val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//                            no.setOnClickListener {
//                                dialog.dismiss()
//                            }
//                            val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//                            yes.setOnClickListener {
//                                dialog.dismiss()
//                                deletePendingKOT(
//                                    instanceId,
//                                    position,
//                                    menuListSec[0].order.kot_instance!![position]
//                                )
//                            }
//
//                    }
//
//                    override fun onKotItemDelete(
//                        position: Int,
//                        orderKotId: Int,
//                        kot: KotInstance?
//                    ) {
//                            val dialog = Dialog(activity!!, R.style.DialogTheme)
//                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//                            dialog.setCancelable(false)
//                            dialog.setContentView(R.layout.dialog_common_filee)
//                            dialog.show()
//
//                            val title = dialog.findViewById(R.id.text_title) as AppCompatTextView
//                            title.text = resources.getString(R.string.msg_delete_item)
//
//                            val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
//                            no.setOnClickListener {
//                                dialog.dismiss()
//                            }
//                            val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
//                            yes.setOnClickListener {
//                                dialog.dismiss()
//                                deletePendingItem(orderKotId, position, kot!!)
//                            }
//                    }
//                })
//
//        binding.rvPendingKot.adapter = adapter
//    }
//
//    private fun printKOTInvoice(
//        position: Int,
//        kot: KotInstance?,
//        kotTakeaway: TakeawayOrder?,
//        invoice: DataInvoice?
//    ) {
//        printerList = db!!.GetPrinters()
//        kotBluetooth = kot
//        kotBluetoothTakeaway = kotTakeaway
//        invoiceBluetooth = invoice
//        positionBluetooth = position
//            printKOTMain(
//                kot,
//                null,
//                kotTakeaway,
//                null,
//                null,
//                tableName,
//                custPerson,
//                custAddress,
//                type,
//                object :
//                    OnDialogClick {
//                    override fun onOk() {
//                        if (isAlert) {
//                            val intent = Intent()
//                            intent.putExtra(KeyUtils.msg, "")
//                            requireActivity().setResult(Activity.RESULT_OK, intent)
//                            requireActivity().finish()
//                        } else {
//                            makeDecisionToGetPending(true)
//                        }
//                    }
//
//                    override fun onCancel() {
//                        if (isAlert) {
//                            val intent = Intent()
//                            intent.putExtra(KeyUtils.msg, "")
//                            requireActivity().setResult(Activity.RESULT_OK, intent)
//                            requireActivity().finish()
//                        } else {
//                            makeDecisionToGetPending(true)
//                        }
//                    }
//                })
//
//    }
//
//    val PERMISSION_BLUETOOTH = 1
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String?>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            when (requestCode) {
//                PERMISSION_BLUETOOTH -> {
//                    if (kotBluetooth != null || kotBluetoothTakeaway != null || invoiceBluetooth != null) printKOTInvoice(
//                        positionBluetooth,
//                        kotBluetooth,
//                        kotBluetoothTakeaway,
//                        invoiceBluetooth
//                    )
//                }
//            }
//        }
//    }
//
//    private fun editKOT(orderInstance: KotInstance, row: TakeawayOrder?) {
//        val localListMenu: List<MenuData> = db!!.GetWaiterCategories()
//        if (localListMenu.isEmpty()) {
//            getMenu(orderInstance, row)
//        } else {
//            val cartList = ArrayList<CartItemRow>()
//            for (cat in localListMenu) {
//                for (kotItem in orderInstance.item) {
//                    for (item in cat.items) {
//                        if (item.id == kotItem.item_id) {
//                            cartList.add(
//                                CartItemRow(
//                                    kotItem.item_id,
//                                    kotItem.id,
//                                    item.item_name,
//                                    kotItem.short_name
//                                        ?: "",
//                                    kotItem.price,
//                                    item.sp_inst,
//                                    cat.item_cat_id,
//                                    tableId,
//                                    if (type == DINEIN || type == ROOM) "0" else "-1",
//                                    kotItem.qty,
//                                    kotItem.kot_ncv,
//                                    kotItem.sp_inst,
//                                    kotItem.kitchen_cat_id,
//                                    kotItem.item_tax,
//                                    kotItem.item_tax_amt,
//                                    kotItem.item_amt, 1
//                                )
//                            )
//                        }
//                    }
//                }
//            }
//            if (type == TAKEAWAY) {
//                if (row != null) {
//                    getInvoice(row.inv_id)
//                }
//            } else {
//                db!!.InsertTableItems(cartList)
//                val intent = Intent(activity, MenuViewActivity::class.java)
//                intent.putExtra("table_name", tableName)
//                intent.putExtra("order_id", if (row != null) row.id else orderId)
//                intent.putExtra("table_id", tableId)
//                intent.putExtra("type", type)
//                intent.putExtra("section_id", 0)
//                intent.putExtra("session_id", sessionId)
//                intent.putExtra("is_free_table", false)
//                intent.putExtra("kot_instance_id", orderInstance.kot_instance_id)
//                intent.putExtra("is_edit", true)
//                intent.putExtra("name", custName)
//                intent.putExtra("number", custNumber)
//                intent.putExtra("person", custPerson)
//                intent.putExtra("gst", "")
//                intent.putExtra("discount", "0")
//                intent.putExtra("cust_cat_id", custCatId)
//                ViewKOTsActivityResult.launch(intent)
//            }
//        }
//    }
//
//    private fun getMenu(orderKotId: KotInstance, row: TakeawayOrder?) {
//        try {
//            showProgressDialog(mContext)
//            isProcessing = true
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .getMenu(Utils.getVirtual(db!!, WebAPI.u409faba1a8ad), 0).await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleMenuResponse(response, orderKotId, row)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleMenuResponse(
//        response: CommonKey<ArrayList<MenuData>>,
//        orderKotId: KotInstance,
//        row: TakeawayOrder?
//    ) {
//        if (response.status > 0) {
//            val itr = response.data!!.iterator()
//            while (itr.hasNext()) {
//                val t: MenuData = itr.next()
//                if (t.item_cat_id == -1) {
//                    itr.remove()
//                }
//            }
//            db!!.InsertWaiterCategory(response.data!!)
//            editKOT(orderKotId, row)
//        }
//        isProcessing = false
//    }
//
//    /** delete pending item **/
//    private fun deletePendingItem(kot_id: Int, position: Int, kot: Any) {
//        try {
//            showProgressDialog(activity)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .deleteItemFromCloseOrderQSR(
//                            Utils.getVirtual(db!!, WebAPI.delete_kot_item),
//                            kot_id.toString()
//                        )
//                        .await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleDeletePendingItemResponse(response, position, kot, kot_id)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    /** handle delete pending kot response **/
//    private fun handleDeletePendingItemResponse(
//        response: CommonResultWithData,
//        position: Int,
//        kot: Any,
//        ids: Int
//    ) {
//        if (response.status == 1) {
//            if (CommonUtils.internetAvailable(requireActivity())) {
//
//                    if (pref!!.getBool(mContext!!, "prn_setting")) {
//                        if (kot is TakeawayOrder) {
//                            for (it in kot.resto_order_kot) {
//                                if (it.kot_id == ids) {
//                                    it.soft_delete = 2
//                                    it.is_edited = 3
//                                }
//                            }
//
//                            printKOTInvoice(position, null, kot, null)
//                        } else if (kot is KotInstance) {
//                            for (it in kot.item) {
//                                if (it.id == ids) {
//                                    it.soft_delete = 2
//                                    it.is_edited = 3
//                                }
//                            }
//                            printKOTInvoice(position, kot, null, null)
//                        }
//                    } else {
//                        displayPrinterSettingDialog(
//                            requireActivity(),
//                            resources.getString(R.string.please_setup_the_kot_print_printer),
//                            resources.getString(R.string.go_to_settings_printer_management),
//                            false,
//                            resources.getString(R.string.ok),
//                            "",
//                            object :
//                                OnDialogClick {
//                                override fun onOk() {
//                                    makeDecisionToGetPending(true)
//                                }
//
//                                override fun onCancel() {
//
//                                }
//                            })
//                    }
//            }
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    /** delete pending kot **/
//    private fun deletePendingKOT(orderId: String, position: Int, kot: Any) {
//        try {
//            showProgressDialog(activity)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .deleteKOTQSR(Utils.getVirtual(db!!, WebAPI.delete_kot_instance), orderId)
//                        .await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleDeletePendingKotResponse(response, position, kot)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    /** handle delete pending kot response **/
//    private fun handleDeletePendingKotResponse(
//        response: CommonResultMSG,
//        position: Int,
//        kot: Any
//    ) {
//        if (response.status == 1) {
//            msgRespo = response.message
//            if (CommonUtils.internetAvailable(requireActivity())) {
//                    if (pref!!.getBool(mContext!!, "prn_setting")) {
//                        if (kot is TakeawayOrder) {
//                            kot.soft_delete = 1
//                            printKOTInvoice(position, null, kot, null)
//                        } else if (kot is KotInstance) {
//                            kot.soft_delete = 1
//                            printKOTInvoice(position, kot, null, null)
//                        }
//                    } else {
//                        displayPrinterSettingDialog(
//                            requireActivity(),
//                            resources.getString(R.string.please_setup_the_kot_print_printer),
//                            resources.getString(R.string.go_to_settings_printer_management),
//                            false,
//                            resources.getString(R.string.ok),
//                            "",
//                            object :
//                                OnDialogClick {
//                                override fun onOk() {
//                                    makeDecisionToGetPending(true)
//                                }
//
//                                override fun onCancel() {
//
//                                }
//                            })
//                    }
//
//            }
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.pre_order -> {
//                if (db!!.GetCustCat().isEmpty()) {
//                    syncData("cust_cat", object : OnSync {
//                        override fun onComplete() {
//                            customerDialog()
//                        }
//
//                        override fun onCancel() {
//
//                        }
//                    })
//                } else {
//                    customerDialog()
//                }
//                true
//            }
//
//            R.id.ncv -> {
//                viewNCV(orderNcv, object : OnNcvClick {
//                    override fun onNcvClick(value: Int) {
//                        orderNcv = value
//                        order.order.inv_ncv = value
//                        ncv!!.setIcon(
//                            when (orderNcv) {
//                                1 -> R.drawable.ic_no_charge
//                                2 -> R.drawable.ic_complementary
//                                3 -> R.drawable.ic_void
//                                else -> R.drawable.ic_regular
//                            }
//                        )
//                        saveCustomerDetails(
//                            order.order.cust_name
//                                ?: resources.getString(R.string.dash),
//                            order.order.cust_mobile ?: "-",
//                            order.order.no_of_person.toString(),
//                            order.order.cust_add.toString(),
//                            order.order.cust_gst_no.toString(), "ncv"
//                        )
//                    }
//                })
//                true
//            }
//
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        menu.clear()
//        inflater.inflate(R.menu.pre_order, menu)
//        menu.findItem(R.id.pre_order_receipt).isVisible = false
//        menu.findItem(R.id.pre_order).isVisible = type == 1
//        menu.findItem(R.id.pre_order_filter).isVisible = false
//        menu.findItem(R.id.pending_filter).isVisible = false
//        menu.findItem(R.id.grid).isVisible = false
//        ncv = menu.findItem(R.id.ncv)
//        ncv!!.isVisible = true
//        ncv!!.setIcon(
//            when (orderNcv) {
//                1 -> R.drawable.ic_no_charge
//                2 -> R.drawable.ic_complementary
//                3 -> R.drawable.ic_void
//                else -> R.drawable.ic_regular
//            }
//        )
//    }
//
//    private fun getCloseOrderAll(id: Int, type: String) {
//        try {
//            showProgressDialog(mContext)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .closeOrders(
//                            Utils.getVirtual(db!!, WebAPI.u31949b9c9e8e86949585848f), id
//                        ).await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleAllCloseOrderResponse(response, type)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleAllCloseOrderResponse(response: CommonResultMSG, type: String) {
//        if (response.status == 1) {
//            if (tabStatus == 3) {
//                binding.btnCreateInvoice.callOnClick()
//            } else {
//                if (type == "close_table") {
//                    requireActivity().finish()
//                } else {
//                    tabStatus = 3
//                    binding.btnCloseOrder.visibility = View.GONE
//                    makeDecisionToGetPending(true)
//                }
//            }
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    private fun getDeleteOrder(id: Int) {
//        try {
//            showProgressDialog(mContext)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .deleteOrders(Utils.getVirtual(db!!, WebAPI.u3594a2a393929d), id).await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleDeleteOrderResponse(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleDeleteOrderResponse(response: CommonResultMSG) {
//        if (response.status == 1) {
//            val intent = Intent()
//            intent.putExtra("success", 1)
//            intent.putExtra(KeyUtils.msg, response.message)
//            requireActivity().setResult(AppCompatActivity.RESULT_OK, intent)
//            requireActivity().finish()
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    private fun createInvoice(
//        bottomSheetDialog: BottomSheetDialog,
//        order: OrderQSR,
//        createType: String,
//        invoiceTempId: Int
//    ) {
//        try {
//            showProgressDialog(mContext)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .createInvoice(
//                            Utils.getVirtual(db!!, WebAPI.u3998a0a3a9a0989090),
//                            order.id,
//                            if (order.cust_name != null) order.cust_name!! else "",
//                            if (order.cust_mobile != null) order.cust_mobile!! else "",
//                            if (order.cust_gst_no != null) order.cust_gst_no!! else null,
//                            if (order.cust_add != null) order.cust_add!! else "",
//                            custCatId,
//                            order.no_of_person.toString(),
//                            order.disc_percentage.toString(),
//                            order.disc_type,
//                            order.disc_code_id,
//                            receiptJson.toString(),
//                            invoiceTempId,
//                            order.inv_ncv)
//                        .await()
//
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleCreateInvoice(bottomSheetDialog, response, createType)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleCreateInvoice(
//        bottomSheetDialog: BottomSheetDialog,
//        response: SubmitNewOrderInvoice,
//        createType: String
//    ) {
//        if (response.status == 3) {
//            bottomSheetDialog.cancel()
//            val intent = Intent()
//            intent.putExtra("success", 2)
//            intent.putExtra(KeyUtils.msg, response.message)
//            requireActivity().setResult(AppCompatActivity.RESULT_OK, intent)
//            requireActivity().finish()
//        } else if (response.status == 2) {
//            bottomSheetDialog.cancel()
//            displayActionSnackbarBottom(
//                requireActivity(),
//                response.message,
//                3,
//                true,
//                resources.getString(R.string.view_invoice),
//                resources.getString(R.string.exit),
//                object :
//                    OnDialogClick {
//                    override fun onOk() {
//                        val intent = Intent(mContext, ReceiptInfoActivity::class.java)
//                        intent.putExtra("invoice_id", response.data.inv_details.id)
//                        startActivity(intent)
//                    }
//
//                    override fun onCancel() {
//                        requireActivity().finish()
//                    }
//                })
//        } else if (response.status == 1) {
//            bottomSheetDialog.cancel()
//            if (createType == "print") {
//                message = response.message
//                    if (pref!!.getBool(mContext!!, "prn_setting")) {
//                        printInvoiceMain(
//                            response.data,
//                            response.data.inv_details.tab_label,
//                            response.data.inv_details.no_of_ppl,
//                            response.data.inv_details.cust_add ?: "",
//                            response.data.inv_details.order_type,
//                            object :
//                                OnDialogClick {
//                                override fun onOk() {
//                                    requireActivity().finish()
//                                }
//
//                                override fun onCancel() {
//                                    requireActivity().finish()
//                                }
//                            })
//                    } else {
//                        displayPrinterSettingDialog(
//                            requireActivity(),
//                            resources.getString(R.string.please_setup_the_kot_print_printer),
//                            resources.getString(R.string.go_to_settings_printer_management),
//                            false,
//                            resources.getString(R.string.ok),
//                            "",
//                            object :
//                                OnDialogClick {
//                                override fun onOk() {
//                                    requireActivity().finish()
//                                }
//
//                                override fun onCancel() {
//
//                                }
//                            })
//
//                }
//            } else {
//                A5Printer.showThermalInvoice(
//                    requireActivity(),
//                    response.data,
//                    true,
//                    pref!!.getStr(mContext!!, "resto_gst_no"),
//                    pref!!.getStr(mContext!!, "fssi_lic_no"),
//                    pref!!.getStr(mContext!!, "sac_code"),
//                    pref!!.getStr(mContext!!, "gst_type"),
//                    object :
//                        A5Printer.OnSuccess {
//                        override fun onSuccess(isSuccess: Boolean?) {
//                            val intent = Intent()
//                            intent.putExtra(KeyUtils.msg, response.message)
//                            requireActivity().setResult(Activity.RESULT_OK, intent)
//                            requireActivity().finish()
//                        }
//                    })
//            }
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    fun viewInvoice(orders: OrderDetailQSR, bool: Boolean) {
//        if (bool) {
//            Log.d("responseTag", "invoiceApi")
//            if (db!!.GetCustCat().isEmpty()) {
//                syncData("cust_cat", object : OnSync {
//                    override fun onComplete() {
//                        viewInvoice(orders, false)
//                    }
//
//                    override fun onCancel() {
//
//                    }
//                })
//            } else {
//                viewInvoice(orders, false)
//            }
//        } else {
//            val order = OrderInvoiceHelper.makeOrderInvoiceList(orders)
//            val table = db!!.GetTablesById(tableId)
//            if (table == null) {
//                getTable(orders)
//            } else {
//                OrderInvoiceHelper.viewInvoice(
//                    requireActivity(),
//                    type,
//                    order,
//                    listReceiptAmount,
//                    receiptJson,
//                    object :
//                        OrderInvoiceHelper.SetOnItemClick {
//                        override fun onSubmitInvoice(
//                            bottomSheetDialog: BottomSheetDialog,
//                            receiptJsonFromDialog: JSONArray,
//                            listReceipt: ArrayList<rcpt_channels>,
//                            order: OrderQSR,
//                            invoiceTemplateIds: Int,
//                            createType: String,
//                            custCatIds: Int
//                        ) {
//                            custCatId = custCatIds
//                            receiptJson = receiptJsonFromDialog
//                            listReceiptAmount = listReceipt
//                            invoiceTempId = invoiceTemplateIds
//                            createInvoice(bottomSheetDialog, order, createType, invoiceTempId)
//                        }
//
//                        override fun onUserDetailsUpdate(
//                            bottomSheetDialog: BottomSheetDialog,
//                            view: View,
//                            order: OrderQSR
//                        ) {
//
//                        }
//                    })
//            }
//        }
//    }
//
//    private fun setPaymentMethod(
//        recyclerView: RecyclerView,
//        bottomSheetDialog: BottomSheetDialog,
//        view: View,
//        order: OrderDetailQSR,
//        total: Float
//    ) {
//        val listPayment = db!!.GetReceiptChannel()
//        if (receiptJson != null) {
//            for (i in 0 until receiptJson.length()) {
//                for (j in listPayment) {
//                    val json = receiptJson.getJSONObject(i)
//                    if (json.getString("acc_id") == j.id.toString()) {
//                        if (json.has("amt") && json.getString("amt").isNotEmpty()) {
//                            j.is_checked = true
//                            j.amount = json.getString("amt").toFloat()
//                        }
//                    }
//                }
//            }
//        }
//
//        listPayment.sortByDescending { it.amount }
//        val itr = listPayment.iterator()
//        while (itr.hasNext()) {
//            val t: rcpt_channels = itr.next()
//            if (t.is_show == 0) {
//                itr.remove()
//            }
//        }
//        if (listPayment.size > 1) listPayment.add(
//            0,
//            rcpt_channels(-1, "Multi Payment", false, 1, 0f)
//        )
//
//        val adapterReceipt =
//            ReceiptItemAdapter(mContext!!, listPayment, false, total, object :
//                ReceiptItemAdapter.SetOnItemClick {
//                override fun onSelection(position: Int, check: Boolean) {
//                    if (listPayment[position].id == -1) {
//                        receiptDialog(recyclerView, bottomSheetDialog, view, order, total)
//                    } else {
//                        receiptJson = JSONArray()
//                        if (check) {
//                            val obj = JSONObject()
//                            obj.put("acc_id", listPayment[position].id)
//                            obj.put("amt", total)
//                            receiptJson.put(obj)
//                        }
//                    }
//                }
//
//                override fun onChangeAmount(position: Int, tvAmount: AppCompatTextView) {
//                    tvAmount.text = String.format("₹%.2f", total)
//                }
//            })
//        recyclerView.adapter = adapterReceipt
//    }
//
//    private fun customerDialog() {
//        val dialog = Dialog(requireActivity())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setContentView(R.layout.dialog_customer_details)
//        val tvContactNumber: AutoCompleteTextView =
//            dialog.findViewById(R.id.tv_contact_number) as AutoCompleteTextView
//        val tvName: TextInputEditText = dialog.findViewById(R.id.tv_name) as TextInputEditText
//        val tvPerson: TextInputEditText = dialog.findViewById(R.id.tv_ppl) as TextInputEditText
//        val tvAddress: TextInputEditText =
//            dialog.findViewById(R.id.edt_address) as TextInputEditText
//        val tvGst: TextInputEditText = dialog.findViewById(R.id.tv_gst) as TextInputEditText
//        val tiContactNumber: TextInputLayout =
//            dialog.findViewById(R.id.ti_contact_number) as TextInputLayout
//        val tiAddress: TextInputLayout = dialog.findViewById(R.id.ti_address) as TextInputLayout
//        val tiGst: TextInputLayout = dialog.findViewById(R.id.ti_gst) as TextInputLayout
//        val btnOk = dialog.findViewById(R.id.btn_ok) as MaterialButton
//        val cancel = dialog.findViewById(R.id.tv_cancel) as AppCompatTextView
//        val txtTitle = dialog.findViewById(R.id.txt_title_cust) as AppCompatTextView
//        val chipGroup = dialog.findViewById(R.id.chips) as ChipGroup
//        val viewHistory: AppCompatImageView =
//            dialog.findViewById(R.id.view_history) as AppCompatImageView
//
//        if (db!!.GetCustCat().isNotEmpty()) {
//            Utils.addCustChips(
//                requireActivity(),
//                custCatId,
//                true,
//                db!!.GetCustCat(),
//                chipGroup,
//                txtTitle
//            ) { v, b ->
//                val chip = v as Chip
//                if (b) {
//                    custCatId = v.getTag(R.id.v).toString().toInt()
//                    chip.setTextAppearance(R.style.ChipTextAppearanceWhite)
//                    chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst_checked)
//                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        resources.getColor(R.color.colorPrimary, null)
//                    } else {
//                        resources.getColor(R.color.colorPrimary)
//                    }
//                    chip.chipBackgroundColor = ColorStateList.valueOf(color)
//                } else {
//                    chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
//                    chip.setTextAppearance(R.style.ChipTextAppearance)
//                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        resources.getColor(R.color.login_bg, null)
//                    } else {
//                        resources.getColor(R.color.login_bg)
//                    }
//                    chip.chipBackgroundColor = ColorStateList.valueOf(color)
//                }
//            }
//        } else {
//            txtTitle.visibility = View.GONE
//            chipGroup.visibility = View.GONE
//        }
//        var phoneNumber = order.order.cust_mobile
//        if (phoneNumber.isNullOrEmpty() || phoneNumber == "xxxxx-xxxxx" || phoneNumber == "null"|| phoneNumber == "-") {
//            phoneNumber = ""
//        }
//        if (order.order.cust_name.isNullOrEmpty() ||
//            (order.order.cust_name ?: "").lowercase() == "walk-in customer" ||
//            order.order.cust_name == "null"||
//            order.order.cust_name == "-"
//        ) {
//            order.order.cust_name = ""
//        }
//        tvContactNumber.setText(phoneNumber)
//        tvAddress.setText(order.order.cust_add)
//        tvContactNumber.tag = "0"
//        tvContactNumber.addTextChangedListener {
//            if (tvContactNumber.text.toString().length == 10 && tvContactNumber.text.toString()
//                    .isDigitsOnly()
//            ) {
//                if (tvContactNumber.tag.toString() == "0") {
//                    if (CommonUtils.internetAvailable(requireActivity())) {
//                        Utils.getNameByNumber(
//                            requireActivity(),
//                            tvContactNumber.text.toString(),
//                            tvContactNumber,
//                            tvName,
//                            viewHistory,
//                            object :
//                                Utils.Companion.SetOnCustomerListener {
//                                override fun onCustomerClick(data: CustomerData) {
//                                    tvName.setText(data.name)
//                                    tvContactNumber.tag = "1"
//                                    tvContactNumber.setText(data.mobile)
//                                    tvName.setSelection((data.name ?: "").length)
//                                    custCatId = data.cust_cat_id
//                                    Utils.addCustChips(
//                                        requireActivity(),
//                                        data.cust_cat_id,
//                                        true,
//                                        db!!.GetCustCat(),
//                                        chipGroup,
//                                        txtTitle
//                                    ) { v, b ->
//                                        val chip = v as Chip
//                                        if (b) {
//                                            custCatId = v.getTag(R.id.v).toString().toInt()
//                                            chip.setTextAppearance(R.style.ChipTextAppearanceWhite)
//                                            chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst_checked)
//                                            val color =
//                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                                    resources.getColor(R.color.colorPrimary, null)
//                                                } else {
//                                                    resources.getColor(R.color.colorPrimary)
//                                                }
//                                            chip.chipBackgroundColor = ColorStateList.valueOf(color)
//                                        } else {
//                                            chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
//                                            chip.setTextAppearance(R.style.ChipTextAppearance)
//                                            val color =
//                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                                    resources.getColor(R.color.login_bg, null)
//                                                } else {
//                                                    resources.getColor(R.color.login_bg)
//                                                }
//                                            chip.chipBackgroundColor = ColorStateList.valueOf(color)
//                                        }
//                                    }
//                                }
//                            })
//                    }
//                } else {
//                    tvContactNumber.tag = "0"
//                }
//            }
//        }
//        tvName.setText(order.order.cust_name)
//        tvPerson.setText("${order.order.no_of_person}")
//        tvGst.setText(if (order.order.cust_gst_no != "null") order.order.cust_gst_no else "")
//        tvPerson.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus) {
//                tvPerson.text!!.clear()
//            }
//        }
//        btnOk.setOnClickListener {
//            var flag = 0
//            if (!tvContactNumber.text.isNullOrEmpty() && tvContactNumber.text!!.length < 10) {
//                tiContactNumber.error = resources.getString(R.string.enter_valid_phone_number)
//                flag = 1
//            }
//            if (flag == 0) {
//                saveCustomerDetails(
//                    tvName.text.toString(),
//                    tvContactNumber.text.toString(),
//                    tvPerson.text.toString(),
//                    tvAddress.text.toString(),
//                    tvGst.text.toString(), "customer"
//                )
//                dialog.dismiss()
//            }
//        }
//
//        cancel.setOnClickListener {
//            dialog.dismiss()
//        }
//        Objects.requireNonNull(dialog.window)
//            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.window!!.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        dialog.show()
//    }
//
//    private fun receiptDialog(
//        recyclerView: RecyclerView,
//        bottomSheetDialog: BottomSheetDialog,
//        view: View,
//        order: OrderDetailQSR,
//        total: Float
//    ) {
//        val dialog = Dialog(requireActivity(), R.style.DialogTheme)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setCanceledOnTouchOutside(true)
//        dialog.setContentView(R.layout.dialog_receipt)
//        Objects.requireNonNull(dialog.window)
//            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        val btnClose = dialog.findViewById<AppCompatImageView>(R.id.btn_close)
//        btnClose.setOnClickListener {
//            dialog.cancel()
//        }
//        val tvViewInvoice = dialog.findViewById<AppCompatTextView>(R.id.tv_view_invoice)
//        val checkMulti = dialog.findViewById<CheckBox>(R.id.check_multi)
//        tvViewInvoice.visibility = View.GONE
//        checkMulti.visibility = View.GONE
//        val btnOk = dialog.findViewById<MaterialButton>(R.id.btn_ok)
//        val tvCancel = dialog.findViewById<AppCompatTextView>(R.id.tv_cancel)
//        val linPayment = dialog.findViewById<LinearLayout>(R.id.lin_payment)
//        val txtTotal = dialog.findViewById<AppCompatTextView>(R.id.txt_total)
//        val txtReceipt = dialog.findViewById<AppCompatTextView>(R.id.txt_receipt)
//        val txtRemain = dialog.findViewById<AppCompatTextView>(R.id.txt_remain)
//        val linDetails = dialog.findViewById<View>(R.id.lin_details)
//        val vDetails = dialog.findViewById<View>(R.id.v_details)
//
//        linDetails.visibility = View.GONE
//        vDetails.visibility = View.GONE
//        tvViewInvoice.visibility = View.GONE
//
//        txtTotal.text = String.format("₹%.2f", total)
//        val recReceipt = dialog.findViewById<RecyclerView>(R.id.rec_receipt)
//        recReceipt!!.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
//        listReceiptAmount = ArrayList()
//        val listReceipt = db!!.GetReceiptChannel()
//        val itr = listReceipt.iterator()
//        while (itr.hasNext()) {
//            val t: rcpt_channels = itr.next()
//            if (t.is_show == 0) {
//                itr.remove()
//            }
//        }
//
//        val adapterReceipt = ReceiptItemAdapter(mContext!!, listReceipt, true, 0f, object :
//            ReceiptItemAdapter.SetOnItemClick {
//            override fun onSelection(position: Int, check: Boolean) {
//                val dif = Utils.getEnteredAmount(linPayment, total.roundToInt().toFloat())
//                listReceipt[position].amount = dif
//                addLayout(
//                    recReceipt,
//                    linPayment,
//                    listReceipt[position],
//                    txtReceipt,
//                    txtRemain,
//                    true,
//                    total.roundToInt()
//                        .toFloat(),
//                    mContext!!,
//                    db!!,
//                    listReceiptAmount
//                )
//            }
//
//            override fun onChangeAmount(position: Int, tvAmount: AppCompatTextView) {
//            }
//        })
//        recReceipt.adapter = adapterReceipt
//
//        if (listReceipt.size > 1) {
//            for (listPay in 0 until listReceipt.size) {
//                if (receiptJson.length() > 0) {
//                    for (item in 0 until receiptJson.length()) {
//                        if (listReceipt[listPay].id == receiptJson.getJSONObject(item)
//                                .getInt("acc_id")
//                        ) {
//                            listReceipt[listPay].is_checked = true
//                            if (receiptJson.getJSONObject(item).getString("amt").isNotEmpty()) {
//                                listReceipt[listPay].amount =
//                                    receiptJson.getJSONObject(item).getInt("amt").toFloat()
//                            }
//                            addLayout(
//                                recReceipt,
//                                linPayment,
//                                listReceipt[listPay],
//                                txtReceipt,
//                                txtRemain,
//                                true,
//                                total.roundToInt()
//                                    .toFloat(),
//                                mContext!!,
//                                db!!,
//                                listReceiptAmount
//                            )
//                        }
//                    }
//                }
//            }
//        }
//
//        btnOk.text = resources.getString(R.string.ok)
//        btnOk.setOnClickListener {
//            receiptJson = JSONArray()
//            for (i in 0 until linPayment.childCount) {
//                val id = linPayment.getChildAt(i).tag.toString()
//                val tvPrice = linPayment.getChildAt(i).findViewById<EditText>(R.id.tv_price)
//                val obj = JSONObject()
//                obj.put("acc_id", id)
//                obj.put("amt", tvPrice.text.toString().ifEmpty { "0" })
//                receiptJson.put(obj)
//            }
//            setPaymentMethod(recyclerView, bottomSheetDialog, view, order, total)
//            dialog.cancel()
//        }
//
//        tvCancel.setOnClickListener {
//            dialog.dismiss()
//            setPaymentMethod(recyclerView, bottomSheetDialog, view, order, total)
//        }
//        dialog.window!!.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        dialog.show()
//    }
//
//    private fun getTable(kot: KotInstance) {
//        try {
//            showProgressDialog(mContext!!)
//            isProcessing = true
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext!!)
//                        .getTables(Utils.getVirtual(db!!, WebAPI.u409fb29d9ca49ba7), 0).await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleGetTableResponseOrder(response, kot)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleGetTableResponseOrder(
//        response: CommonKey<List<GetTables>>,
//        kot: KotInstance
//    ) {
//        if (response.status > 0) {
//            moveKOTProcess(kot, response.data as ArrayList<GetTables>)
//        }
//        isProcessing = false
//    }
//
//    private fun getTable(orders: OrderDetailQSR) {
//        try {
//            showProgressDialog(mContext!!)
//            isProcessing = true
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext!!)
//                        .getTables(Utils.getVirtual(db!!, WebAPI.u409fb29d9ca49ba7), type).await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleGetTableResponse(response, orders)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleGetTableResponse(
//        response: CommonKey<List<GetTables>>,
//        orders: OrderDetailQSR
//    ) {
//        db!!.InsertTable(response.data)
//        viewInvoice(orders, true)
//        isProcessing = false
//    }
//
//    private fun saveCustomerDetails(
//        tvName: String, tvContactNumber: String, tvPerson: String,
//        tvAdd: String, tvGst: String, from: String
//    ) {
//        try {
//            showProgressDialog(mContext)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext)
//                        .saveCustomerDetails(
//                            Utils.getVirtual(
//                                db!!,
//                                WebAPI.EDIT_ORDER_CUST_DETAILS
//                            ),
//                            orderId,
//                            getTextFromInput(tvName),
//                            getTextFromInput(tvContactNumber),
//                            getTextFromInput(tvGst),
//                            "",
//                            getTextFromInput(tvPerson),
//                            getTextFromInput(tvAdd),
//                            custCatId,
//                            orderNcv
//                        )
//                        .await()
//
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleSaveCustomerInvoice(
//                            response, tvName, tvContactNumber,
//                            tvPerson, tvAdd, tvGst, from
//                        )
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun getTextFromInput(tv: String): String {
//        return if (tv.isNullOrEmpty()) "" else tv
//    }
//
//    private fun handleSaveCustomerInvoice(
//        response: CommonResultMSG,
//        tvName: String,
//        tvContactNumber: String,
//        tvPerson: String,
//        tvAdd: String,
//        tvGst: String,
//        from: String
//    ) {
//        if (response.status == 1) {
//            order.order.cust_mobile = tvContactNumber
//            order.order.cust_name = tvName
//            order.order.no_of_person = tvPerson.toInt()
//            order.order.cust_add = tvAdd
//            order.order.cust_gst_no = tvGst
//            order.order.cust_cat_id = custCatId
//            if (from == "ncv") {
//                displayActionSnackbar(requireActivity(), "Order Updated", 1)
//            } else {
//                displayActionSnackbar(requireActivity(), response.message, 1)
//            }
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    private fun getInvoice(invID: Int) {
//        try {
//            showProgressDialog(mContext!!)
//            lifecycleScope.launch {
//                try {
//                    val response = RetrofitClient.getInstance(mContext!!).getInvoice(
//                        Utils.getVirtual(db!!, WebAPI.u409fa7aab0a79f9797), invID
//                    ).await()
//                    withContext(Dispatchers.Main) {
//                        dismissProgressDialog()
//                        handleInvoiceRespo(response)
//                    }
//                } catch (e: Exception) {
//                    dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            }
//        } catch (ex: IllegalArgumentException) {
//            ex.printStackTrace()
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        } catch (ex: java.lang.Exception) {
//            ex.printStackTrace()
//        }
//    }
//
//    private fun handleInvoiceRespo(response: GetInvoiceById) {
//        binding.swipeContainer.isRefreshing = false
//        if (response.status > 0) {
//            if (response.data != null) {
//                EditOrderReceiptActivity.order = response.data
//                val intent = Intent(mContext!!, EditOrderReceiptActivity::class.java)
//                intent.putExtra("editReceipt", false)
//                intent.putExtra("from_takeaway", true)
//                resultLauncher.launch(intent)
//            } else {
//                displayActionSnackbar(requireActivity(), response.message, 2)
//            }
//        } else {
//            displayActionSnackbar(requireActivity(), response.message, 2)
//        }
//    }
//
//    var resultLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) { // There are no request codes
//                isNewItemAdded = true
//                if (CommonUtils.internetAvailable(requireActivity())) {
//                    makeDecisionToGetPending(true)
//                }
//            }
//        }
//}