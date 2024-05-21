package com.eresto.captain.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.adapter.TableKotAdapter
import com.eresto.captain.adapter.TableKotChildAdapter
import com.eresto.captain.adapter.ViewKotAdapter
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityViewMenuBinding
import com.eresto.captain.model.CartItemRow
import com.eresto.captain.model.Item
import com.eresto.captain.model.MenuData
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.SocketForegroundService
import com.eresto.captain.utils.Utils
import com.eresto.captain.utils.Utils.Companion.displayActionSnackbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.Objects

class MenuViewActivity : BaseActivity() {

    private var strGST: String = ""
    private var strAdd: String = ""
    private var strPerson: Int = 1
    private var strName: String = ""
    private var strContactNumber: String = ""
    private var binding: ActivityViewMenuBinding? = null
    private var type = 0
    private var tableName = "0"
    private var name = ""
    private var number = ""
    private var address = ""
    private var from: String? = null
    private var noc = 0
    private var to = ""
    private var orderNcr = 0
    private var invId = 0
    private var tableId = 0
    private var isEditOrder = false
    private var isFromInvoice = false
    private var isAlert = false

    var listItem: ArrayList<MenuData>? = null

    private var dialogConst: Dialog? = null
    private var dialogSnack: Dialog? = null

    var adapterChild: TableKotChildAdapter? = null
    var adapterParent: TableKotAdapter? = null
    var adapterSearch: TableKotChildAdapter? = null
    var oldSelection = ""
    var custGST = ""
    var custDiscount = ""


    var kotNcv = 0
    var ncv: MenuItem? = null
    var isFilterOn = false
    var pageForItems = 0
    var selectedPriceTemplate = 0
    var isLoading: Boolean = false
    var limit: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMenuBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        tableName = intent.getStringExtra("table_name") ?: ""
        tableId = intent.getIntExtra("table_id", 0)
        type = intent.getIntExtra("type", 0)
        isEditOrder = intent.getBooleanExtra("is_edit_order", false)
        isFromInvoice = intent.getBooleanExtra("is_from_invoice", false)
        isAlert = intent.getBooleanExtra("is_alert", false)
        name = intent.getStringExtra("name") ?: ""
        number = intent.getStringExtra("number") ?: ""
        address = intent.getStringExtra("address") ?: ""
        from = intent.getStringExtra("from")
        noc = intent.getIntExtra("noc", 0)
        to = intent.getStringExtra("to") ?: ""
        orderNcr = intent.getIntExtra("order_ncr", 0)
        invId = intent.getIntExtra("inv_id", 0)
        strName = intent.getStringExtra("name").toString()
        strContactNumber = intent.getStringExtra("number").toString()
        strPerson = intent.getIntExtra("person", 1)
        intent.putExtra("address", strAdd)
        custDiscount = intent.getStringExtra("discount").toString()
        custGST = intent.getStringExtra("gst").toString()
        binding!!.toolbar.title = tableName
        setSupportActionBar(binding!!.toolbar)
        db = DBHelper(this@MenuViewActivity)
        pref = Preferences()

        selectedPriceTemplate = pref!!.getInt(this@MenuViewActivity, "def_pt")
        binding!!.recyclerview.layoutManager =
            LinearLayoutManager(this@MenuViewActivity, RecyclerView.VERTICAL, false)
        binding!!.recyclerview.setHasFixedSize(true)

        binding!!.recyclerview.layoutManager =
            LinearLayoutManager(this@MenuViewActivity, RecyclerView.VERTICAL, false)
        binding!!.imgFilter.setOnClickListener {
            val wrapper: Context = ContextThemeWrapper(this@MenuViewActivity, R.style.MyPopupMenu)
            val menu = PopupMenu(wrapper, binding!!.imgFilter)
            menu.menu.add(resources.getString(R.string.by_item_name))
            menu.menu.add(resources.getString(R.string.by_item_group))
            menu.menu.getItem(0).isCheckable = true
            menu.menu.getItem(1).isCheckable = true

            if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_name") {
                menu.menu.getItem(0).isChecked = true
                binding!!.recyclerview.visibility = View.VISIBLE
            } else if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_group") {
                menu.menu.getItem(1).isChecked = true
                binding!!.recyclerview.visibility = View.VISIBLE
            }

            menu.setOnMenuItemClickListener { item ->

                oldSelection = pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)
                if (item.title == resources.getString(R.string.by_item_name)) {
                    pref!!.setStr(this@MenuViewActivity, "item_name", KeyUtils.sortingKey)
                    binding!!.recyclerview.visibility = View.VISIBLE
                    if (!binding!!.edtSearch.query.isNullOrEmpty()) {
                        binding!!.edtSearch.setQuery("", false)
                    } else {
                        getWaiterMenuList("")
                    }
                } else if (item.title == resources.getString(R.string.by_item_group)) {
                    pref!!.setStr(this@MenuViewActivity, "item_group", KeyUtils.sortingKey)
                    binding!!.recyclerview.visibility = View.VISIBLE
                    if (!binding!!.edtSearch.query.isNullOrEmpty()) {
                        binding!!.edtSearch.setQuery("", false)
                    } else {
                        getWaiterMenuList("")
                    }
                }
                true
            }
            menu.show()
            listItem?.let { it1 -> setAdapterParent(it1) }
        }

        binding!!.edtSearch.setOnCloseListener {
            binding!!.recyclerview.adapter = adapterParent
            true
        }
        binding!!.edtSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                when {
                    s.isEmpty() -> {
                        if (pref!!.getStr(
                                this@MenuViewActivity,
                                KeyUtils.sortingKey
                            ) == "item_group"
                        ) {
                            binding!!.recyclerview.adapter = adapterParent
//                            if (fsi != null && !isFilterOn) {
//                                binding!!.linFsi.visibility = View.VISIBLE
//                            }

                        } else if (pref!!.getStr(
                                this@MenuViewActivity,
                                KeyUtils.sortingKey
                            ) == "item_name"
                        ) {
                            getWaiterMenuList("")

                        }
                        binding!!.llMenu.visibility = View.VISIBLE
                    }

                    s.length > 1 -> {
                        setSearchAdapter(s)
                    }

                    s.isNotEmpty() -> {
                        binding!!.llMenu.visibility = View.GONE
                        binding!!.recyclerview.adapter = adapterChild
                    }
                }
                return true
            }
        })
        binding!!.llMenu.setOnClickListener {
            val wrapper: Context = ContextThemeWrapper(this@MenuViewActivity, R.style.popup_bg)
            val menu =
                PopupMenu(
                    wrapper,
                    binding!!.llMenu
                )
            val list = db!!.GetSyncCategories()
            list.add(
                0, MenuData(
                    0,
                    "All Categories",
                    0, 0, 0, 0, false, false, ArrayList<Item>()
                )
            )
            for (i in list.indices) {
                menu.menu.add(i, i, i, list[i].category_name)
            }
            menu.gravity = Gravity.TOP
            menu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                if (item.title.toString().lowercase() == "all categories") {
                    isFilterOn = false
//                    if (fsi != null) {
//                        binding!!.linFsi.visibility = View.VISIBLE
//                    }
                    setAdapterList(list!!, "", true)
                } else {
                    isFilterOn = true
//                    binding!!.linFsi.visibility = View.GONE
                    val itemsLocal = db!!.GetSyncItemsByCat(list[item.itemId].item_cat_id)
                    if (!item.title.isNullOrEmpty()) {
                        val listItemFilter = ArrayList<MenuData>()
                        catLoop@ for (i in itemsLocal!!.indices) {
                            if (itemsLocal[i].category_name == (item.title)) {
                                itemsLocal[i].isExpanded = true
                                listItemFilter.add(itemsLocal[i])
                                break@catLoop
                            }
                        }
                        setAdapterList(listItemFilter, "", true)
                    }
                }
                true
            })
            setFAB(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                menu.menu.setGroupDividerEnabled(true)  // This adds the divider between groups 0 and 1, but only supported on Android 9.0 and up.
            }
            menu.show()
            menu.setOnDismissListener {
                setFAB(false)
            }
        }

        binding!!.btnView.setOnClickListener {
            val isDefaultSetting = pref!!.getBool(this, "isDefaultPrinterSetting")
            if (!isDefaultSetting) {
                dialogDefaultPrinterSetting(this)
                displayActionSnackbar(this, "Please set printer settings first", 2)
                return@setOnClickListener
            }
            if (db!!.GetCartItems(tableId).isNotEmpty()) {
                submitKOT()
            } else {
                displayActionSnackbar(this, "Please add something", 2)
            }
        }
        binding!!.btnCancel.setOnClickListener {
            if (!db!!.GetCartItems(tableId).isEmpty()) {
                viewKOT()
            }
        }
        if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey).isNullOrEmpty()) {
            pref!!.setStr(this@MenuViewActivity, "item_group", KeyUtils.sortingKey)
        }
        pageForItems = 0
        listItem = java.util.ArrayList()
        FetchLocalData().execute()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                db!!.deleteItemOfTable(tableId)
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun getWaiterMenuList(expandItem: String) {
        isLoading = false

        if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_name") {
            binding!!.recyclerview.visibility = View.VISIBLE
            binding!!.llMenu.visibility = View.GONE
        } else if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_group") {
            binding!!.recyclerview.visibility = View.VISIBLE
            binding!!.llMenu.visibility = View.VISIBLE
        }
        /** normal listing **/
        setAdapterList(listItem!!, expandItem, false)
    }

    private fun setFAB(isOpen: Boolean) {
        if (isOpen) {
            binding!!.txtMenu.text = "CLOSE"
            binding!!.imgIcon.setImageResource(R.drawable.ic_vector_close)
        } else {
            binding!!.txtMenu.text = "MENU"
            binding!!.imgIcon.setImageResource(R.drawable.ic_restaurant_spoon)
        }
    }

    private fun setAdapterList(
        list: ArrayList<MenuData>,
        expandItem: String,
        isHardChange: Boolean
    ) {

        val tableItemList: List<CartItemRow> = db!!.GetCartItems(tableId)
        val listChild = mutableListOf<Item>()
        var isDifferentNCV = false
        var allItemNcv = 0
        for (cat in list) {
            for (table in tableItemList) {
                for (item in cat.items) {
                    if (table.id == item.id) {
                        item.isChecked = true
                        item.count = table.qty
                        item.localId = table.id
                        item.item_price = table.item_price
                        item.local_sp_inst = table.notes
                        item.kot_ncv = table.kot_ncv
                        if (expandItem == "update") {
                            db!!.UpdateTableItem(table)
                            showAnim()
                        }
                    }
                }
            }
            if (cat.item_cat_id > -1)
                listChild.addAll(cat.items)
        }
        listChild.sortBy { it.item_name }


        adapterChild = TableKotChildAdapter(this@MenuViewActivity, listChild, object :
            TableKotChildAdapter.SetOnItemClick {
            override fun onItemClicked(position: Int, item: Item) {
                addItemDialog(
                    position,
                    item.isChecked,
                    item,
                    item.item_cat_id
                )
            }

            override fun onItemDelete(position: Int, item: Item, values: Int) {
                db!!.deleteItemOfTable(tableId, item.id)
                showAnim()
            }

            override fun onItemUpdate(position: Int, item: Item, values: Int) {
                val data = Utils.countTaxDiscount(
                    if (item.kot_ncv == 0) {
                        item.item_price
                    } else 0.0,
                    item.count,
                    item.item_tax, 0f, 0, 0.0, 1
                ).split("|")
                val ncv = if (kotNcv == 0) item.kot_ncv else kotNcv
                val tableItem = CartItemRow(
                    item.id,
                    0,
                    item.item_name,
                    item.item_short_name
                        ?: "",
                    item.item_price,
                    item.sp_inst
                        ?: "",
                    item.item_cat_id,
                    tableId,
                    "-1",
                    item.count,
                    ncv,
                    item.local_sp_inst,
                    item.kitchen_cat_id,
                    item.item_tax,
                    data[0],
                    data[1], 0
                )

                if (item.isChecked) {
                    db!!.UpdateTableItemQty(tableItem)
                    setAdapterParent(list)
                    adapterParent?.refreshList(tableId, position)
                    showAnim()
                } else {
                    db!!.InsertTableItems(tableItem)
                    showAnim()
                    setAdapterParent(list)
                    if (adapterParent != null) {
                        adapterParent?.refreshList(tableId, position)
                    }
                }
            }
        })
        if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_name") {
//            if (adapterChild == null) {

//                if (adapterChild == null) {
//                    binding!!.recyclerview.isNestedScrollingEnabled = false
//                    binding!!.recyclerview.adapter = adapterChild
//                } else {
//                    binding!!.recyclerview.adapter = adapterChild
//                }
//            } else {

            adapterChild!!.setItemList(listChild)
            if (oldSelection != pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)) {
                binding!!.recyclerview.adapter = adapterChild
            }
            binding!!.llMenu.visibility = View.GONE
//            }
        } else {
            binding!!.llMenu.visibility = View.VISIBLE
        }
        if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_group") {
            if (adapterParent == null || isHardChange) {
                setAdapterParent(list)
                if (adapterParent == null) {
                    binding!!.recyclerview.isNestedScrollingEnabled = false
                    binding!!.recyclerview.adapter = adapterParent
                } else {
                    binding!!.recyclerview.adapter = adapterParent
                }
                binding!!.llMenu.visibility = View.VISIBLE

            } else {
                adapterParent!!.setItemList(list)
                if (oldSelection != pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)) {
                    binding!!.recyclerview.adapter = adapterParent
                }
//            if (lastIndex > 0) {
//                linearLayoutManager!!.scrollToPositionWithOffset(lastIndex, 0)
//            }
            }
        }
        oldSelection = pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)
        dismissProgressDialog()
        binding!!.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isLoading && !limit) {
                        pageForItems++
                        isLoading = true
                        FetchLocalData().execute()
                    }
                }
            }
        })
        binding!!.recyclerviewChild.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isLoading && !limit) {
                        pageForItems++
                        isLoading = true
                        FetchLocalData().execute()
                    }
                }
            }
        })

    }

    private fun  setAdapterParent( list: ArrayList<MenuData>){
        adapterParent = TableKotAdapter(this@MenuViewActivity, list, object :
            TableKotAdapter.SetOnItemClick {
            override fun onItemScroll(position: Int) {
                binding!!.recyclerview.scrollToPosition(position)
            }

            override fun onItemClicked(position: Int, item: Item, parent: MenuData) {
                addItemDialog(
                    position,
                    item.isChecked,
                    item,
                    item.item_cat_id
                )
            }

            override fun onItemDelete(
                position: Int,
                item: Item,
                parent: MenuData,
                values: Int
            ) {
                db!!.deleteItemOfTable(tableId, item.id)
                if (item.item_cat_id == -1) {
                    adapterParent!!.refreshList(tableId, -1)
                    adapterParent!!.refreshList(tableId, position)
                } else {
                    adapterParent!!.refreshList(tableId, position)
                    adapterParent!!.refreshList(tableId, -1)
                }
//                        val mPosition = if (db!!.isItemFromFSI(item.id)) -1 else position
//                        adapterParent!!.refreshList(tableId, mPosition)
                showAnim()
            }

            override fun onItemUpdate(
                position: Int,
                item: Item,
                parent: MenuData,
                values: Int
            ) {
                val ncv = if (kotNcv == 0) item.kot_ncv else kotNcv
                val data = Utils.countTaxDiscount(
                    if (item.kot_ncv == 0) {
                        item.item_price
                    } else 0.0,
                    item.count,
                    item.item_tax, 0f, 0, 0.0, 1
                ).split("|")
                val tableItem = CartItemRow(
                    item.id,
                    0,
                    item.item_name,
                    item.item_short_name
                        ?: "",
                    item.item_price,
                    item.sp_inst
                        ?: "",
                    item.item_cat_id,
                    tableId,
                    "-1",
                    item.count,
                    ncv,
                    item.local_sp_inst,
                    item.kitchen_cat_id,
                    item.item_tax,
                    data[0],
                    data[1], 0
                )

                if (item.isChecked) {
                    db!!.UpdateTableItemQty(tableItem)
                    adapterParent!!.refreshList(tableId, position)
                    showAnim()
                } else {
                    db!!.InsertTableItems(tableItem)
                    showAnim()
                    adapterParent!!.refreshList(tableId, position)
                }
            }
        })
    }

    private fun showAnim() {
        val list = db!!.GetCartItems(tableId)
        if (list.isEmpty()) {
            binding!!.viewAnim.clearAnimation()
            binding!!.relAnim.visibility = View.GONE
        } else {
            binding!!.relAnim.visibility = View.VISIBLE
            binding!!.viewAnim.clearAnimation()
            val animBlink = AnimationUtils.loadAnimation(this@MenuViewActivity, R.anim.ripple)
            animBlink.repeatCount = Animation.INFINITE
            binding!!.viewAnim.startAnimation(animBlink)
            binding!!.txtCount.text = "${list.size}"
        }
    }


    inner class FetchLocalData : AsyncTask<Void, Int, Void>() {

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(this@MenuViewActivity)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            // Perform your background task here
            val list = db!!.GetSyncItems(pageForItems)
            if (list.isEmpty()) {
                displayActionSnackbarBottom(
                    this@MenuViewActivity,
                    "Resources missing to Manage Order, Login Again",
                    3,
                    false,
                    "Login",
                    object :
                        BaseActivity.OnDialogClick {
                        override fun onOk() {
                            val bool = db!!.DeleteDB()
                            val resultIntent = Intent()
                            resultIntent.putExtra("exit", true)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }

                        override fun onCancel() {
                        }
                    })
                return null
            }
            if (list.isNullOrEmpty()) limit = true
            if (listItem.isNullOrEmpty()) {
                listItem = ArrayList()
                listItem!!.addAll(list)
            } else {
                for (it in list) {
                    var found = 0
                    for (its in listItem!!) {
                        if (it.item_cat_id == its.item_cat_id) {
                            its.items.addAll(it.items)
                            found = 1
                        }
                    }
                    if (found == 0) {
                        listItem!!.add(it)
                    }
                }
            }
            return null
        }


        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            getWaiterMenuList("update")
            dismissProgressDialog()
            // Task completed, update UI or perform any other required actions
        }
    }

    private fun setSearchAdapter(q: String) {
        val listChild = db!!.GetSyncSearchItems(q)
        val tableItemList: List<CartItemRow> = db!!.GetCartItems(tableId)

        for (table in tableItemList) {
            for (item in listChild) {
                if (table.id == item.id) {
                    item.isChecked = true
                    item.count = table.qty
                    item.kot_ncv = table.kot_ncv
                    item.localId = table.id
                    item.local_sp_inst = table.notes
                }
            }
        }

        adapterSearch = TableKotChildAdapter(this@MenuViewActivity, listChild, object :
            TableKotChildAdapter.SetOnItemClick {
            override fun onItemClicked(position: Int, item: Item) {
                addItemDialog(
                    position,
                    item.isChecked,
                    item,
                    item.item_cat_id
                )
            }

            override fun onItemDelete(position: Int, item: Item, values: Int) {
                db!!.deleteItemOfTable(tableId, item.id)
//                val mPosition = if (db!!.isItemFromFSI(item.id)) -1 else position
//                adapterSearch!!.refreshList(tableId, mPosition)
//                adapterParent!!.refreshList(tableId, mPosition)
                showAnim()
            }

            override fun onItemUpdate(position: Int, item: Item, values: Int) {
                val data = Utils.countTaxDiscount(
                    if (item.kot_ncv == 0) {
                        item.item_price
                    } else 0.0,
                    item.count,
                    item.item_tax, 0f, 0, 0.0, 1
                ).split("|")
                val ncv = if (kotNcv == 0) item.kot_ncv else kotNcv
                val tableItem = CartItemRow(
                    item.id,
                    0,
                    item.item_name,
                    item.item_short_name
                        ?: "",
                    item.item_price,
                    item.sp_inst
                        ?: "",
                    item.item_cat_id,
                    tableId,
                    "-1",
                    item.count,
                    ncv,
                    item.local_sp_inst,
                    item.kitchen_cat_id,
                    item.item_tax,
                    data[0],
                    data[1], 0
                )

                if (item.isChecked) {
                    db!!.UpdateTableItem(tableItem)
                    adapterSearch!!.refreshList(tableId, position)
                    adapterParent!!.refreshList(tableId, position)
                    showAnim()
                } else {
                    db!!.InsertTableItems(tableItem)
                    showAnim()
                    adapterSearch!!.refreshList(tableId, position)
                    adapterParent!!.refreshList(tableId, position)
                }
            }
        })
        binding!!.recyclerview.adapter = adapterSearch
    }


    private fun submitKOT() {
        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        showProgressDialog(this@MenuViewActivity)
        val arrOrder = JSONArray()
        Log.e("dkadaldj", "KOT_NCV : $kotNcv")
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("item_name", item.item_name)
            obj.put("item_price", item.item_price)
            obj.put("qty", item.qty)
            obj.put("item_short_name", item.item_short_name)
            obj.put("kitchen_cat_id", item.kitchen_cat_id)
            obj.put("item_tax", item.item_tax)
            val ncv = if (this.kotNcv == 0) item.kot_ncv else kotNcv
            Log.e("dkadaldj", "KOT_NCV item : $ncv")
            obj.put("kot_ncv", ncv)
            if (!item.notes.isNullOrEmpty()) obj.put(
                "notes", item.notes!!.replace("[", "")
                    .replace("]", "").replace("\"", "").replace("null", "")
            )
            else obj.put("notes", "")
            arrOrder.put(obj)
        }
        val jsonObj = JSONObject()
        Log.e(
            "dhdshfkh",
            "CD Data :: $strName , $strContactNumber , $strGST , $strPerson , $address"
        )
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
        val cv = JSONObject()
        jsonObj.put("ca", KOT)
        cv.put("sid", pref!!.getInt(this, "sid"))
        cv.put("last_sync_print", pref!!.getLng(this, "last_sync_print"))
        cv.put("table_id", tableId)
        cv.put("type", type)
        cv.put("kot", arrOrder)
        cv.put("cd", cd)
        jsonObj.put("cv", cv)
        persons = strPerson
        currentJsonData = jsonObj
        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_KOT)
    }

    fun addChipsFromInput(edtSpecialInst: EditText, chipGroup: ChipGroup, context: Context) {
        val inputText = edtSpecialInst.text.toString()
        val items = inputText.split(",").map { it.trim() }.filter { it.isNotBlank() } // Clean input

        for (item in items) {
            var chipExists = false

            // Check if chip already exists
            for (i in 0 until chipGroup.childCount) {
                val existingChip = chipGroup.getChildAt(i) as Chip
                if (existingChip.text.toString().equals(item, ignoreCase = true)) {
                    chipExists = true
                    break
                }
            }

            if (!chipExists) { // Only add new chips
                val chip = Chip(context)
                chip.text = item
                chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
                chip.isCheckedIconVisible = false
                chip.isCheckable = true
                chip.setTextAppearance(R.style.ChipTextAppearance)
                chip.isCloseIconVisible = true // Allow chip removal

                // Handle chip removal
                chip.setOnCloseIconClickListener {
                    chipGroup.removeView(chip) // Remove chip from ChipGroup
                    updateEditText(edtSpecialInst, chipGroup) // Update EditText
                }

                // Handle checked state change
                chip.setOnCheckedChangeListener { _, _ ->
                    updateEditText(edtSpecialInst, chipGroup)
                }

                chipGroup.addView(chip)
                chip.isChecked = true
            }
        }

        // Clear input after adding chips
        edtSpecialInst.text = null
    }

    // Function to update EditText based on existing chips
    fun updateEditText(edtSpecialInst: EditText, chipGroup: ChipGroup) {
        val remainingChips = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            remainingChips.add(chip.text.toString())
        }
        edtSpecialInst.setText(remainingChips.joinToString(", "))
        edtSpecialInst.setSelection(edtSpecialInst.text.toString().length)
    }


    private fun addItemDialog(position: Int, isEdit: Boolean, item: Item, parent_id: Int) {
        var count = 1
        var ncv = item.kot_ncv
        val dialog = BottomSheetDialog(this@MenuViewActivity, R.style.BottomSheetPostDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_add_menu_item)
        val btnBack: ImageButton = dialog.findViewById(R.id.btn_back)!!
        val btnAddInst: ImageView = dialog.findViewById(R.id.btn_add_inst)!!
        val chipGroup = dialog.findViewById<ChipGroup>(R.id.chips)!!
        btnBack.setOnClickListener {
            dialog.cancel()
        }

        val edtSpecialInst = dialog.findViewById<EditText>(R.id.edt_special_inst)!!
        edtSpecialInst.addTextChangedListener {
            edtSpecialInst.error = null
        }
        btnAddInst.setOnClickListener {
            if (edtSpecialInst.text.toString().isNullOrEmpty()) {
                edtSpecialInst.error = resources.getString(R.string.enter_special_instruction_name)
            } else {
                val inputText = edtSpecialInst.text.toString()
                val items =
                    inputText.split(",").map { it.trim() } // Split input by comma and trim spaces
                for (item in items) {
                    if (item.isNotEmpty()) { // Avoid adding empty chips
                        val chip = Chip(this@MenuViewActivity)
                        chip.text = item
                        chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
                        chip.isCheckedIconVisible = false
                        chip.isCheckable = true
                        chip.setTextAppearance(R.style.ChipTextAppearance)
                        chip.setOnCheckedChangeListener { c, isChecked ->
                            edtSpecialInst.setText(
                                chip.text.toString()
                            )
                            if (isChecked) {
                                c.setTextAppearance(R.style.CheckedChipTextAppearance)
                                c.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst_checked)
                            } else {
                                edtSpecialInst.text = null
                                c.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
                                c.setTextAppearance(R.style.ChipTextAppearance)
                            }
                            edtSpecialInst.setSelection(edtSpecialInst.text.toString().length)
                        }

                        chip.isChecked = true
                        if (chip.isChecked) {
                            chip.setTextAppearance(R.style.CheckedChipTextAppearance)
                            chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst_checked)
                        }
                        chipGroup.addView(chip)
                    }
                }
                edtSpecialInst.text = null // Clear input after adding chips
            }
        }
        val name: AppCompatTextView = dialog.findViewById(R.id.tv_item_name)!!
        val tvInstruction: TextInputEditText = dialog.findViewById(R.id.tv_name)!!
        val tvItemCurrency: AppCompatTextView = dialog.findViewById(R.id.tv_item_currency)!!
        val tvItemPrice: AppCompatEditText = dialog.findViewById(R.id.tv_item_price)!!
        val linMin: LinearLayout = dialog.findViewById(R.id.lin_min)!!
        val linAdd: LinearLayout = dialog.findViewById(R.id.lin_add)!!
        val btnAdd = dialog.findViewById<MaterialButton>(R.id.btn_add)!!
        val txtTitle = dialog.findViewById<AppCompatTextView>(R.id.txt_title)!!

        val tvQty: AppCompatEditText = dialog.findViewById(R.id.tv_qty)!!
        tvQty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                count = if (!tvQty.text.toString().isNullOrEmpty()) tvQty.text.toString()
                    .toInt() else item.count
            }
        })
        tvQty.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                count = if (!tvQty.text.toString().isNullOrEmpty()) {
                    if (tvQty.text.toString().toInt() < 1) {
                        tvQty.setText("1")
                        1
                    } else {
                        tvQty.text.toString().toInt()
                    }
                } else {
                    tvQty.setText("1")
                    1
                }
            } else {
                tvQty.setText("")
            }
        }

        var localInst: List<String>? = null
        var notes: String? = null
        tvInstruction.setOnKeyListener { v, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                dialog.cancel()
            }
            true
        }
        tvInstruction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                Utils.checkChips(tvInstruction.text.toString(), chipGroup)
            }
        })
        var sorting = 0
        if (isEdit) {
            val cart = db!!.GetCartItems(tableId, item.id)
            sorting = 1
            notes = cart.notes
            localInst = if (notes.isNullOrEmpty()) emptyList() else notes.split(",")
            count = item.count
            tvItemPrice.setText("${cart.item_price}")
            var inst = ""
            if (!notes.isNullOrEmpty()) {
                localInst = try {
                    val gson = Gson()
                    val type: Type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson(notes, type)

                } catch (e: java.lang.Exception) {
                    notes.split(",")
                }
                if (localInst != null) {
                    for (i in localInst) {
                        inst += "$i,"
                    }
                }
            }
            if (inst.isNotEmpty()) {
                inst = inst.substring(0, inst.length - 1)
            }
            tvInstruction.setText(inst)
        }
        tvItemPrice.setText("${item.item_price}")

        Utils.addChips(
            this@MenuViewActivity,
            item.sp_inst,
            localInst,
            chipGroup,
            txtTitle
        ) { _, _ ->
            tvInstruction.setText(Utils.getCheckedIns(tvInstruction.text.toString(), chipGroup))
            tvInstruction.setSelection(tvInstruction.text.toString().length)
        }

        name.text = item.item_name
        tvItemCurrency.text = pref!!.getStr(this@MenuViewActivity, KeyUtils.restoCurrency)
        tvQty.setText("$count")

        val linOrderNcv: LinearLayout = dialog.findViewById(R.id.lin_order_ncv)!!
        val layoutNVC: LinearLayout = dialog.findViewById(R.id.layoutNCV)!!
        val btnComplimentary: AppCompatTextView = dialog.findViewById(R.id.btn_complimentary)!!
        val btnVoid: AppCompatTextView = dialog.findViewById(R.id.btn_void)!!
        btnComplimentary.setOnClickListener {
            if (ncv == 2) {
                ncv = 0
                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                if (tvItemPrice.text.toString().isEmpty() || tvItemPrice.text.toString() == "0") {
                    tvItemPrice.setText("${item.item_price}")
                    tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                }
            } else {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
                btnComplimentary.backgroundTintList = GetColor(R.color.colorSecondary)
                btnComplimentary.setTextColor(GetColor(R.color.color_white))
                ncv = 2
            }
        }
        btnVoid.setOnClickListener {
            if (ncv == 3) {
                ncv = 0
                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
                if (tvItemPrice.text.toString().isEmpty() || tvItemPrice.text.toString() == "0") {
                    tvItemPrice.setText("${item.item_price}")
                    tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                }
            } else {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                btnVoid.backgroundTintList = GetColor(R.color.colorSecondary)
                btnVoid.setTextColor(GetColor(R.color.color_white))
                ncv = 3
            }
        }
        when (ncv) {
            0 -> {
                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
            }

            2 -> {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnComplimentary.backgroundTintList = GetColor(R.color.colorSecondary)
                btnComplimentary.setTextColor(GetColor(R.color.color_white))
            }

            3 -> {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnVoid.backgroundTintList = GetColor(R.color.colorSecondary)
                btnVoid.setTextColor(GetColor(R.color.color_white))
            }
        }
        tvItemPrice.addTextChangedListener {
            if (tvItemPrice.text.toString().isNotEmpty()) {
                when (ncv) {
                    2 -> {
                        btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                        btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                    }

                    3 -> {
                        btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                        btnVoid.setTextColor(GetColor(R.color.colorSecondary))
                    }
                }
                ncv = 0
            }
        }
        when (kotNcv) {
            0 -> {
                linOrderNcv.visibility = View.VISIBLE
                layoutNVC.visibility = View.VISIBLE
            }

            1 -> {
                linOrderNcv.visibility = View.GONE
                layoutNVC.visibility = View.GONE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }

            2 -> {
                linOrderNcv.visibility = View.GONE
                btnVoid.visibility = View.GONE
                layoutNVC.visibility = View.GONE
//                layoutNVC.visibility = View.VISIBLE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }

            3 -> {
                linOrderNcv.visibility = View.GONE
                btnComplimentary.visibility = View.GONE
                layoutNVC.visibility = View.GONE
//                layoutNVC.visibility = View.VISIBLE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }
        }

        linMin.setOnClickListener {
            if (count > 1) {
                count -= 1
                tvQty.setText("$count")
                tvQty.setSelection(tvQty.text.toString().length)
            }
        }
        linAdd.setOnClickListener {
            count += 1
            tvQty.setText("$count")
            tvQty.setSelection(tvQty.text.toString().length)
        }
        if (isEdit) btnAdd.text = "Update"
        btnAdd.setOnClickListener {
            dialog.dismiss()
            val arrStr = ArrayList<String>()
            for (chi in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(chi) as Chip
                if (chip.isChecked) {
                    arrStr.add(chip.text.toString())
                }
            }
            val text = if (arrStr.isNotEmpty()) {
                arrStr.joinToString(",")
            } else {
                ""
            }
            if (tvItemPrice.text.toString().isEmpty()) {
                tvItemPrice.setText("0")
            }
            val data = Utils.countTaxDiscount(
                if (item.kot_ncv == 0) {
                    tvItemPrice.text.toString().toDouble()
                } else item.item_price, count, item.item_tax, 0f, 0, 0.0, 1
            ).split("|")

            val tableItem = CartItemRow(
                item.id,
                0,
                item.item_name ?: "",
                item.item_short_name
                    ?: "", tvItemPrice.text.toString().toDouble(),
                item.sp_inst
                    ?: "",
                parent_id,
                tableId,
                "-1",
                count,
                ncv,
                text,
                item.kitchen_cat_id,
                item.item_tax,
                data[0],
                data[1], sorting
            )
            if (isEdit) {
                db!!.UpdateTableItem(tableItem)
                showAnim()
            } else {
                db!!.InsertTableItems(tableItem)
                showAnim()
            }
            showAnim()
            binding!!.edtSearch.setQuery("", false)
            adapterParent!!.refreshList(tableId, position)
        }

        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun updateItemDialog(
        item: CartItemRow,
        recyclerView: RecyclerView,
        bottomSheetDialog: BottomSheetDialog?
    ) {
        var count = item.qty
        var ncv = item.kot_ncv
        val dialog = BottomSheetDialog(this@MenuViewActivity, R.style.BottomSheetPostDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_add_menu_item)
        val btnBack: ImageButton = dialog.findViewById(R.id.btn_back)!!
        val btnAddInst: ImageView = dialog.findViewById(R.id.btn_add_inst)!!
        val chipGroup = dialog.findViewById<ChipGroup>(R.id.chips)!!

        btnBack.setOnClickListener {
            dialog.cancel()
        }
        val edtSpecialInst = dialog.findViewById<EditText>(R.id.edt_special_inst)!!
        edtSpecialInst.addTextChangedListener {
            edtSpecialInst.error = null
        }

        btnAddInst.setOnClickListener {
            if (edtSpecialInst.text.toString().isNullOrEmpty()) {
                edtSpecialInst.error = resources.getString(R.string.enter_special_instruction_name)
            } else {
                val inputText = edtSpecialInst.text.toString()
                val items =
                    inputText.split(",").map { it.trim() } // Split input by comma and trim spaces
                Log.e("flkjfldjl", " ITEMS :: $items")
                for (item in items) {
                    if (item.isNotEmpty()) { // Avoid adding empty chips
                        val chip = Chip(this@MenuViewActivity)
                        chip.text = item
                        chip.setBackgroundResource(R.drawable.layout_rounded_corner_sp_inst)
                        chip.isCheckedIconVisible = false
                        chip.isCheckable = true
                        chip.setTextAppearance(R.style.ChipTextAppearance)

                        chip.setOnCheckedChangeListener { _, _ ->
                            edtSpecialInst.setText(
                                Utils.getCheckedIns(
                                    edtSpecialInst.text.toString(),
                                    chipGroup
                                )
                            )
                            edtSpecialInst.setSelection(edtSpecialInst.text.toString().length)
                        }
                        chipGroup.addView(chip)
                        chip.isChecked = true
                    }
                }
                edtSpecialInst.text = null // Clear input after adding chips
            }
        }
        val name: AppCompatTextView = dialog.findViewById(R.id.tv_item_name)!!
        val tvInstruction: TextInputEditText = dialog.findViewById(R.id.tv_name)!!

        val tvQty: AppCompatEditText = dialog.findViewById(R.id.tv_qty)!!
        tvQty.setText("$count")
        tvQty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                count = if (!tvQty.text.toString().isNullOrEmpty()) tvQty.text.toString()
                    .toInt() else item.qty
            }
        })
        tvQty.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                count = if (!tvQty.text.toString().isNullOrEmpty()) {
                    if (tvQty.text.toString().toInt() < 1) {
                        tvQty.setText("1")
                        1
                    } else {
                        tvQty.text.toString().toInt()
                    }
                } else {
                    tvQty.setText("1")
                    1
                }
            }
        }
        val tvItemPrice: AppCompatEditText = dialog.findViewById(R.id.tv_item_price)!!
        tvItemPrice.setText("${item.item_price}")
        val linMin: LinearLayout = dialog.findViewById(R.id.lin_min)!!
        val linAdd: LinearLayout = dialog.findViewById(R.id.lin_add)!!
        val btnAdd = dialog.findViewById<MaterialButton>(R.id.btn_add)!!
        val txtTitle = dialog.findViewById<AppCompatTextView>(R.id.txt_title)!!
        item.notes =
            if (!item.notes.isNullOrEmpty()) item.notes!!.replace(",null", "").replace("null,", "")
                .replace("null", "") else item.notes
        val localInst = if (item.notes.isNullOrEmpty() || item.notes == "null") {
            ArrayList()
        } else {
            try {
                val gson = Gson()
                val type: Type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(item.notes, type)
            } catch (e: Exception) {
                item.notes!!.split(",")
            }
        }
        Utils.addChips(
            this@MenuViewActivity,
            item.sp_inst,
            localInst,
            chipGroup,
            txtTitle
        ) { _, _ ->
            tvInstruction.setText(Utils.getCheckedIns(tvInstruction.text.toString(), chipGroup))
            tvInstruction.setSelection(tvInstruction.text.toString().length)
        }

        tvInstruction.setOnKeyListener { v, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                btnAdd.callOnClick()
            }
            true
        }
        tvInstruction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                Utils.checkChips(tvInstruction.text.toString(), chipGroup)
            }
        })
        if (item.notes != null || item.notes != "null") {
            try {
                val items = JSONArray(item.notes)
                if (items.length() > 0) {
                    txtTitle.text = resources.getString(R.string.select_special_instruction)
                }
                var inst = ""
                for (i in 0 until items.length()) {
                    inst += items.getString(i) + ","
                }
                if (inst.isNotEmpty()) {
                    inst = inst.substring(0, inst.length - 1)
                }
                tvInstruction.setText(inst)
            } catch (e: JSONException) {
                tvInstruction.setText(item.notes)
                e.printStackTrace()
            }
        }
        val linOrderNcv: LinearLayout = dialog.findViewById(R.id.lin_order_ncv)!!
        val layoutNCV: LinearLayout = dialog.findViewById(R.id.layoutNCV)!!
        val btnComplimentary: AppCompatTextView = dialog.findViewById(R.id.btn_complimentary)!!
        val btnVoid: AppCompatTextView = dialog.findViewById(R.id.btn_void)!!
        btnComplimentary.setOnClickListener {
            if (ncv == 2) {
                ncv = 0
                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                if (tvItemPrice.text.toString().isEmpty() || tvItemPrice.text.toString() == "0") {
                    tvItemPrice.setText("${item.item_price}")
                    tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                }
            } else {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
                btnComplimentary.backgroundTintList = GetColor(R.color.colorSecondary)
                btnComplimentary.setTextColor(GetColor(R.color.color_white))
                ncv = 2
            }
        }
        btnVoid.setOnClickListener {
            if (ncv == 3) {
                ncv = 0
                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
                if (tvItemPrice.text.toString().isEmpty() || tvItemPrice.text.toString() == "0") {
                    tvItemPrice.setText("${item.item_price}")
                    tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                }
            } else {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                btnVoid.backgroundTintList = GetColor(R.color.colorSecondary)
                btnVoid.setTextColor(GetColor(R.color.color_white))
                ncv = 3
            }
        }
        when (ncv) {
            0 -> {
                btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                btnVoid.setTextColor(GetColor(R.color.colorSecondary))
            }

            2 -> {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnComplimentary.backgroundTintList = GetColor(R.color.colorSecondary)
                btnComplimentary.setTextColor(GetColor(R.color.color_white))
            }

            3 -> {
                tvItemPrice.setText("0")
                tvItemPrice.setSelection(tvItemPrice.text.toString().length)
                btnVoid.backgroundTintList = GetColor(R.color.colorSecondary)
                btnVoid.setTextColor(GetColor(R.color.color_white))
            }
        }
        tvItemPrice.addTextChangedListener {
            if (tvItemPrice.text.toString().isNotEmpty()) {
                when (ncv) {
                    2 -> {
                        btnComplimentary.backgroundTintList = GetColor(R.color.light_grey)
                        btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
                    }

                    3 -> {
                        btnVoid.backgroundTintList = GetColor(R.color.light_grey)
                        btnVoid.setTextColor(GetColor(R.color.colorSecondary))
                    }
                }
                ncv = 0
            }
        }
        tvItemPrice.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                // If the event is a key-down event on the "enter" button
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    // Perform action on key press
                    Utils.hideKeyboard(this@MenuViewActivity, tvItemPrice)
                    return true
                }
                return false
            }
        })

        when (kotNcv) {
            0 -> {
                linOrderNcv.visibility = View.VISIBLE
            }

            1 -> {

                linOrderNcv.visibility = View.GONE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }

            2 -> {
                linOrderNcv.visibility = View.GONE
                btnVoid.visibility = View.GONE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }

            3 -> {
                linOrderNcv.visibility = View.GONE
                btnComplimentary.visibility = View.GONE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }
        }


        name.text = item.item_name
        linMin.setOnClickListener {
            if (count > 1) {
                count -= 1
                tvQty.setText("$count")
                tvQty.setSelection(tvQty.text.toString().length)
            }
        }
        linAdd.setOnClickListener {
            count += 1
            tvQty.setText("$count")
            tvQty.setSelection(tvQty.text.toString().length)
        }

        btnAdd.text = "Update"

        btnAdd.setOnClickListener {
            dialog.dismiss()
            val arrStr = ArrayList<String>()
            for (chi in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(chi) as Chip
                if (chip.isChecked) {
                    arrStr.add(chip.text.toString())
                }
            }
            val text = if (arrStr.isNotEmpty()) {
                arrStr.joinToString(",")
            } else {
                ""
            }
            if (tvItemPrice.text.toString().isEmpty()) {
                tvItemPrice.setText("0")
            }
            val data = Utils.countTaxDiscount(
                if (item.kot_ncv == 0) {
                    tvItemPrice.text.toString().toDouble()
                } else item.item_price, count, item.item_tax, 0f, 0, 0.0, 1
            ).split("|")

            val tableItem =
                CartItemRow(
                    item.id,
                    0,
                    item.item_name,
                    item.item_short_name,
                    tvItemPrice.text.toString().toDouble(),
                    item.sp_inst,
                    item.item_cat_id,
                    tableId,
                    item.pre_order_id,
                    count,
                    ncv,
                    text,
                    item.kitchen_cat_id, item.item_tax,
                    data[0],
                    data[1], item.sorting
                )
            db!!.UpdateTableItem(tableItem)
            adapterParent!!.refreshList(tableId, -1)
            //displayActionSnackbar(this@MenuViewActivity, resources.getString(R.string.item_updated), 1)
            setAdapter(recyclerView, bottomSheetDialog)
        }

        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.clear()
        menuInflater.inflate(R.menu.view_kot_menu, menu)
        ncv = menu.findItem(R.id.ncv)
        ncv!!.isVisible = true
        ncv!!.setIcon(
            when (kotNcv) {
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
                onBackPressed()
                true
            }

            R.id.ncv -> {
                viewNCV()
                true
            }

            R.id.pre_order -> {
                customerDialog(false)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun viewNCV() {
        val bottomSheetDialog =
            BottomSheetDialog(this@MenuViewActivity, R.style.BottomSheetPostDialogTheme)
        bottomSheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        bottomSheetDialog.setContentView(R.layout.bottomsheet_view_ncv)
        bottomSheetDialog.show()
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.peekHeight = metrics.heightPixels

        val regular = bottomSheetDialog.findViewById<LinearLayout>(R.id.btn_regular)!!
        val noCharge = bottomSheetDialog.findViewById<LinearLayout>(R.id.btn_no_charge)!!
        val complimentary = bottomSheetDialog.findViewById<LinearLayout>(R.id.btn_complimentary)!!
        val void = bottomSheetDialog.findViewById<LinearLayout>(R.id.btn_void)!!
        val pad =
            this@MenuViewActivity.resources.getDimension(com.intuit.sdp.R.dimen._12sdp).toInt()
        val itemsCart = db!!.GetCartItems(tableId)
        when (kotNcv) {
            0 -> {
                regular.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
                regular.setPadding(pad, pad, pad, pad)
            }

            1 -> {
                noCharge.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
                noCharge.setPadding(pad, pad, pad, pad)
            }

            2 -> {
                complimentary.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
                complimentary.setPadding(pad, pad, pad, pad)
            }

            3 -> {
                void.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
                void.setPadding(pad, pad, pad, pad)
            }
        }
        regular.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
            noCharge.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setBackgroundResource(R.drawable.shape_grey_border_white)
            void.setBackgroundResource(R.drawable.shape_grey_border_white)
            regular.setPadding(pad, pad, pad, pad)
            kotNcv = 0
            ncv!!.setIcon(
                when (kotNcv) {
                    1 -> R.drawable.ic_no_charge
                    2 -> R.drawable.ic_complementary
                    3 -> R.drawable.ic_void
                    else -> R.drawable.ic_regular
                }
            )
            db!!.UpdateTableItemsNcv(itemsCart, kotNcv)
            bottomSheetDialog.cancel()
        }
        noCharge.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
            complimentary.setBackgroundResource(R.drawable.shape_grey_border_white)
            void.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setPadding(pad, pad, pad, pad)
            kotNcv = 1
            ncv!!.setIcon(
                when (kotNcv) {
                    1 -> R.drawable.ic_no_charge
                    2 -> R.drawable.ic_complementary
                    3 -> R.drawable.ic_void
                    else -> R.drawable.ic_regular
                }
            )
            db!!.UpdateTableItemsNcv(itemsCart, kotNcv)
            bottomSheetDialog.cancel()
        }
        complimentary.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
            void.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setPadding(pad, pad, pad, pad)
            kotNcv = 2
            ncv!!.setIcon(
                when (kotNcv) {
                    1 -> R.drawable.ic_no_charge
                    2 -> R.drawable.ic_complementary
                    3 -> R.drawable.ic_void
                    else -> R.drawable.ic_regular
                }
            )
            db!!.UpdateTableItemsNcv(itemsCart, kotNcv)
            bottomSheetDialog.cancel()
        }
        void.setOnClickListener {
            regular.setBackgroundResource(R.drawable.shape_grey_border_white)
            noCharge.setBackgroundResource(R.drawable.shape_grey_border_white)
            complimentary.setBackgroundResource(R.drawable.shape_grey_border_white)
            void.setBackgroundResource(R.drawable.shape_red_border_round_new_nvc)
            void.setPadding(pad, pad, pad, pad)
            kotNcv = 3
            ncv!!.setIcon(
                when (kotNcv) {
                    1 -> R.drawable.ic_no_charge
                    2 -> R.drawable.ic_complementary
                    3 -> R.drawable.ic_void
                    else -> R.drawable.ic_regular
                }
            )
            db!!.UpdateTableItemsNcv(itemsCart, kotNcv)
            bottomSheetDialog.cancel()
        }
    }

    private fun viewKOT() {
        val bottomSheetDialog =
            BottomSheetDialog(this@MenuViewActivity, R.style.BottomSheetPostDialogTheme)
        bottomSheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        bottomSheetDialog.setContentView(R.layout.activity_view_kotactivity)
        bottomSheetDialog.show()
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.peekHeight = metrics.heightPixels

        val toolbar =
            bottomSheetDialog.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)!!
        toolbar.title = "View KOT"

        val imgClose = bottomSheetDialog.findViewById<ImageView>(R.id.img_close)!!
        imgClose.setOnClickListener { bottomSheetDialog.dismiss() }

        val imgBackArrow = bottomSheetDialog.findViewById<AppCompatTextView>(R.id.btn_cancel)!!
        val textNoMsg = bottomSheetDialog.findViewById<TextView>(R.id.text_no_msg)!!
        val btnCreateKot = bottomSheetDialog.findViewById<AppCompatTextView>(R.id.btn_create_kot)!!

        btnCreateKot.setOnClickListener {
            bottomSheetDialog.cancel()
            binding!!.btnView.callOnClick()
        }

        val recyclerview = bottomSheetDialog.findViewById<RecyclerView>(R.id.recyclerview)!!

        imgBackArrow.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        recyclerview.layoutManager =
            LinearLayoutManager(this@MenuViewActivity, RecyclerView.VERTICAL, false)
        setAdapter(recyclerview, bottomSheetDialog)
        bottomSheetDialog.setOnDismissListener {
            getWaiterMenuList("update")
        }
        bottomSheetDialog.setOnCancelListener {
            getWaiterMenuList("update")
        }
    }

    private fun setAdapter(recyclerView: RecyclerView, bottomSheetDialog: BottomSheetDialog?) {
        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        if (list.isNullOrEmpty()) {
            showAnim()
            if (bottomSheetDialog != null) bottomSheetDialog.cancel()
        } else {
            val adapter = ViewKotAdapter(
                this@MenuViewActivity,
                list.toMutableList(),
                object : ViewKotAdapter.SetOnItemClick {
                    override fun itemIncreased(
                        position: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        item.qty += 1
                        view.text = item.qty.toString()
                        val cart = db!!.GetCartItems(tableId, item.id)
                        val data = Utils.countTaxDiscount(
                            if (item.kot_ncv == 0) {
                                item.item_price
                            } else 0.0, cart.qty, cart.item_tax, 0f, 0, 0.0, 1
                        ).split("|")

                        val tableItem =
                            CartItemRow(
                                item.id,
                                0,
                                item.item_name,
                                item.item_short_name,
                                item.item_price,
                                item.sp_inst,
                                item.item_cat_id,
                                tableId,
                                item.pre_order_id,
                                item.qty,
                                item.kot_ncv,
                                item.notes,
                                item.kitchen_cat_id,
                                cart.item_tax,
                                data[0],
                                data[1], cart.sorting
                            )
                        db!!.UpdateTableItem(tableItem)
                        adapterParent!!.refreshList(tableId, -1)
                        adapterChild!!.refreshList(tableId, -1)
                    }

                    override fun itemDecreased(
                        position: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        item.qty -= 1
                        view.text = item.qty.toString()
                        val cart = db!!.GetCartItems(tableId, item.id)
                        val data = Utils.countTaxDiscount(
                            if (item.kot_ncv == 0) {
                                item.item_price
                            } else 0.0, cart.qty, cart.item_tax, 0f, 0, 0.0, 1
                        ).split("|")

                        val tableItem =
                            CartItemRow(
                                item.id,
                                0,
                                item.item_name,
                                item.item_short_name,
                                item.item_price,
                                item.sp_inst,
                                item.item_cat_id,
                                tableId,
                                item.pre_order_id,
                                item.qty,
                                item.kot_ncv,
                                item.notes,
                                item.kitchen_cat_id,
                                cart.item_tax,
                                data[0],
                                data[1], cart.sorting
                            )
                        db!!.UpdateTableItem(tableItem)
                        adapterParent!!.refreshList(tableId, -1)
                        adapterChild!!.refreshList(tableId, -1)
                    }

                    override fun onItemDeleted(
                        position: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        db!!.deleteItemOfTable(item.table_id, item.id)
                        setAdapter(recyclerView, bottomSheetDialog)
                        adapterParent!!.refreshList(tableId, -1)
                        adapterChild!!.refreshList(tableId, -1)
                    }

                    override fun onItemClick(
                        position: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        updateItemDialog(item, recyclerView, bottomSheetDialog)
                    }
                })
            recyclerView.adapter = adapter
            recyclerView.visibility = View.VISIBLE
        }
    }


    fun GetColor(color: Int): ColorStateList {
        return ColorStateList(
            states, Utils.getColorState(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    application.getColor(color)
                } else {
                    application.resources.getColor(color)
                }
            )
        )
    }

    private fun customerDialog(isEdit: Boolean) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_customer_details)
        val tvContactNumber: AutoCompleteTextView =
            dialog.findViewById(R.id.tv_contact_number) as AutoCompleteTextView
        val tvName: TextInputEditText = dialog.findViewById(R.id.tv_name) as TextInputEditText
        val tvPerson: TextInputEditText = dialog.findViewById(R.id.tv_ppl) as TextInputEditText
        val tvGst: TextInputEditText = dialog.findViewById(R.id.tv_gst) as TextInputEditText
        val tvAddress: TextInputEditText =
            dialog.findViewById(R.id.edt_address) as TextInputEditText

        val btnOk = dialog.findViewById(R.id.btn_ok) as MaterialButton
        val cancel = dialog.findViewById(R.id.tv_cancel) as MaterialButton
        tvName.setText(strName)
        tvContactNumber.setText(strContactNumber)
        tvPerson.setText(strPerson.toString())
        tvAddress.setText(strAdd)
        tvGst.setText(strGST)
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
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }
}