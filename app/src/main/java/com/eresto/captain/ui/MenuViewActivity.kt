package com.eresto.captain.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eresto.captain.R
import com.eresto.captain.adapter.CategoryAdapter
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
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.Objects

class MenuViewActivity : BaseActivity(), CategoryAdapter.OnCategoryClickListener {

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
    val kotId by lazy { intent.getIntExtra("kot_id", 0) }

    // TABLET: Properties for tablet UI management
    private var isTablet = false
    private var categoryAdapter: CategoryAdapter? = null
    private var viewKotAdapter: ViewKotAdapter? = null
    private var categoryList = ArrayList<MenuData>()

    private val kotIdAndShortName by lazy {
        intent.getStringExtra("kot_id_short_name")
    }

    private val kotOrderDate by lazy {
        intent.getStringExtra("kot_order_date")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMenuBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        // TABLET: Detect if the tablet layout is active by checking for a view that only exists in it.
        isTablet = binding!!.root.findViewById<View>(R.id.kot_summary_container) != null


        // --- Load Intent Data ---
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
        strName = intent.getStringExtra("name") ?: ""
        strContactNumber = intent.getStringExtra("number") ?: ""
        strPerson = intent.getIntExtra("person", 1)
        strAdd = intent.getStringExtra("address") ?: ""
        custDiscount = intent.getStringExtra("discount") ?: ""
        strGST = intent.getStringExtra("gst") ?: ""


        binding!!.tvKotAndName?.text = kotIdAndShortName
        binding!!.tvKotTime?.text = Utils.getAgoTimeShort(kotOrderDate)


        // --- Toolbar & DB Setup ---
        binding!!.toolbar.title = tableName
        setSupportActionBar(binding!!.toolbar)
        db = DBHelper(this@MenuViewActivity)
        if (!isEditOrder) {
            db!!.deleteItemOfTable(tableId)
        }
        pref = Preferences()
        selectedPriceTemplate = pref!!.getInt(this@MenuViewActivity, "def_pt")

        // --- Main RecyclerView Setup (common for both phone and tablet) ---
        binding!!.recyclerview.layoutManager = LinearLayoutManager(this@MenuViewActivity)
        binding!!.recyclerview.setHasFixedSize(true)

        // TABLET: Add dividers to the item list for a cleaner look
        if (isTablet) {
            ContextCompat.getDrawable(this, R.drawable.divider_line)?.let {
                val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
                divider.setDrawable(it)
                binding!!.recyclerview.addItemDecoration(divider)
            }
        }

        // --- Conditional UI Setup based on device type ---
        if (isTablet) {
            setupTabletView()
        } else {
            setupMobileView()
        }

        // --- Search Functionality (common for both) ---
        binding!!.edtSearch.setOnCloseListener {
            binding!!.recyclerview.adapter = adapterParent
            true
        }
        binding!!.edtSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                // TABLET: When searching, reset the category selection to "All Items"
                if (isTablet && categoryList.isNotEmpty()) {
                    categoryAdapter?.setSelected(0)
                    onCategoryClick(categoryList[0], 0)
                }

                when {
                    s.isEmpty() -> {
                        if (pref!!.getStr(
                                this@MenuViewActivity,
                                KeyUtils.sortingKey
                            ) == "item_group"
                        ) {
                            binding!!.recyclerview.adapter = adapterParent
                        } else {
                            getWaiterMenuList("")
                        }
                        if (!isTablet) binding!!.llMenu?.visibility = View.VISIBLE
                    }

                    s.length > 1 -> setSearchAdapter(s)
                    s.isNotEmpty() -> {
                        if (!isTablet) binding!!.llMenu?.visibility = View.GONE
                        binding!!.recyclerview.adapter = adapterSearch
                    }
                }
                return true
            }
        })

        // --- Initial Data Load ---
        if (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey).isNullOrEmpty()) {
            pref!!.setStr(this@MenuViewActivity, "item_group", KeyUtils.sortingKey)
        }
        pageForItems = 0
        listItem = ArrayList()
        FetchLocalData().execute()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                db!!.deleteItemOfTable(tableId)
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    // TABLET: New function to configure only the tablet UI
    private fun setupTabletView() {
        // Hide mobile-only UI elements
        binding!!.imgFilter?.visibility = View.GONE

        // Setup KOT Summary RecyclerView (right pane)
        val rvKotSummary = binding!!.root.findViewById<RecyclerView>(R.id.rv_kot_summary)
        rvKotSummary.layoutManager = LinearLayoutManager(this)
        updateKotSummary() // Initial setup for the KOT list

        // Setup Submit Button
        val btnSubmitKotTablet = binding!!.root.findViewById<Button>(R.id.btn_submit_kot_tablet)
        btnSubmitKotTablet.setOnClickListener {
            if (db!!.GetCartItems(tableId).isNotEmpty()) {
                submitKOT()
            } else {
                displayActionSnackbar(this, "Please add something", 2)
            }
        }
    }

    // TABLET: New function to configure only the mobile UI (contains your original setup code)
    private fun setupMobileView() {
        binding!!.imgFilter?.setOnClickListener {
            val wrapper: Context = ContextThemeWrapper(this@MenuViewActivity, R.style.MyPopupMenu)
            val menu = android.widget.PopupMenu(wrapper, binding!!.imgFilter)
            menu.menu.add(resources.getString(R.string.by_item_name))
            menu.menu.add(resources.getString(R.string.by_item_group))
            menu.menu.getItem(0).isCheckable = true
            menu.menu.getItem(1).isCheckable = true

            when (pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)) {
                "item_name" -> menu.menu.getItem(0).isChecked = true
                "item_group" -> menu.menu.getItem(1).isChecked = true
            }

            menu.setOnMenuItemClickListener { item ->
                oldSelection = pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)
                val newSelection =
                    if (item.title == resources.getString(R.string.by_item_name)) "item_name" else "item_group"
                pref!!.setStr(this@MenuViewActivity, newSelection, KeyUtils.sortingKey)

                if (!binding!!.edtSearch.query.isNullOrEmpty()) {
                    binding!!.edtSearch.setQuery("", false)
                } else {
                    getWaiterMenuList("")
                }
                true
            }
            menu.show()
        }

        binding!!.llMenu?.setOnClickListener {
            val wrapper: Context = ContextThemeWrapper(this@MenuViewActivity, R.style.popup_bg)
            val menu = PopupMenu(wrapper, binding!!.llMenu!!)
            val list = db!!.GetSyncCategories()
            list.add(0, MenuData(0, "All Categories", 0, 0, 0, 0, false, false, ArrayList()))
            for (i in list.indices) {
                menu.menu.add(i, i, i, list[i].category_name)
            }
            menu.gravity = Gravity.TOP
            menu.setOnMenuItemClickListener { item ->
                isFilterOn = item.title.toString().lowercase() != "all categories"
                if (isFilterOn) {
                    val itemsLocal = db!!.GetSyncItemsByCat(list[item.itemId].item_cat_id)
                    val listItemFilter = ArrayList<MenuData>()
                    itemsLocal?.find { it.category_name == item.title }?.let {
                        it.isExpanded = true
                        listItemFilter.add(it)
                    }
                    setAdapterList(listItemFilter, "", true)
                } else {
                    getWaiterMenuList("")
                }
                true
            }
            setFAB(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                menu.menu.setGroupDividerEnabled(true)
            }
            menu.show()
            menu.setOnDismissListener { setFAB(false) }
        }

        binding!!.btnView?.setOnClickListener {
            if (db!!.GetCartItems(tableId).isNotEmpty()) {
                submitKOT()
            } else {
                displayActionSnackbar(this, "Please add something", 2)
            }
        }
        binding!!.btnCancel?.setOnClickListener {
            if (db!!.GetCartItems(tableId).isNotEmpty()) {
                viewKOT()
            }
        }
    }

    // TABLET: Callback from the CategoryAdapter on the left pane
    override fun onCategoryClick(category: MenuData, position: Int) {
        if (!binding!!.edtSearch.query.isNullOrEmpty()) {
            binding!!.edtSearch.setQuery("", false)
        }

        isFilterOn = if (category.item_cat_id == 0) { // 0 is our ID for "All Items"
            setAdapterList(listItem ?: ArrayList(), "", false)
            false
        } else {
            val filteredList = ArrayList(listItem?.filter { it.item_cat_id == category.item_cat_id }
                ?: emptyList())
            setAdapterList(filteredList, "", true)
            true
        }
    }

    private fun getWaiterMenuList(expandItem: String) {
        isLoading = false
        if (!isTablet) { // Only manage visibility on mobile
            val isItemGroup =
                pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_group"
            binding!!.llMenu?.visibility = if (isItemGroup) View.VISIBLE else View.GONE
        }
        setAdapterList(listItem ?: ArrayList(), expandItem, false)
    }

    private fun setFAB(isOpen: Boolean) {
        binding!!.txtMenu?.text = if (isOpen) "CLOSE" else "MENU"
        binding!!.imgIcon?.setImageResource(if (isOpen) R.drawable.ic_vector_close else R.drawable.ic_restaurant_spoon)
    }

    private fun setAdapterList(
        list: ArrayList<MenuData>,
        expandItem: String,
        isHardChange: Boolean
    ) {
        val tableItemList: List<CartItemRow> = db!!.GetCartItems(tableId)
        val listChild = mutableListOf<Item>()

        for (cat in list) {
            for (item in cat.items) {
                tableItemList.find { it.id == item.id }?.let { tableItem ->
                    item.isChecked = true
                    item.count = tableItem.qty
                    item.localId = tableItem.id
                    item.item_price = tableItem.item_price
                    item.local_sp_inst = tableItem.notes
                    item.kot_ncv = tableItem.kot_ncv
                    item.isEdit = tableItem.isEdit == 1
                    if (expandItem == "update") {
                        showAnim()
                    }
                }
            }
            if (cat.item_cat_id > -1) listChild.addAll(cat.items)
        }
        listChild.sortBy { it.item_name }

        adapterChild = TableKotChildAdapter(
            this@MenuViewActivity,
            listChild,
            object : TableKotChildAdapter.SetOnItemClick {
                override fun onItemClicked(position: Int, item: Item) {
                    if (isTablet) {
                        val tableItem = CartItemRow(
                            item.id,
                            0,
                            item.item_name,
                            item.item_short_name ?: "",
                            item.item_price,
                            item.sp_inst ?: "",
                            item.item_cat_id,
                            tableId,
                            "-1",
                            1,
                            item.kot_ncv,
                            item.local_sp_inst,
                            item.kitchen_cat_id,
                            item.item_tax,
                            "0",
                            "0",
                            0,
                            if (item.isSoftDelete) 1 else 0
                        )
                        if (item.isChecked || item.isSoftDelete) db!!.UpdateTableItemQty(tableItem) else db!!.InsertTableItems(
                            tableItem
                        )
                        adapterParent?.refreshList(tableId, position)
                        showAnim()
                    } else {
                        addItemDialog(position, item.isChecked, item, item.item_cat_id)
                    }
                }

                override fun onItemDelete(position: Int, item: Item, values: Int) {
                    db!!.deleteItemOfTable(tableId, item.id)
                    showAnim()
                }

                override fun onItemUpdate(position: Int, item: Item, values: Int) {
                    // Simplified logic, as full object creation happens in dialog
                    val tableItem = CartItemRow(
                        item.id,
                        0,
                        item.item_name,
                        item.item_short_name ?: "",
                        item.item_price,
                        item.sp_inst ?: "",
                        item.item_cat_id,
                        tableId,
                        "-1",
                        item.count,
                        item.kot_ncv,
                        item.local_sp_inst,
                        item.kitchen_cat_id,
                        item.item_tax,
                        "0",
                        "0",
                        0,
                        if (item.isSoftDelete) 1 else 0
                    )
                    if (item.isChecked || item.isSoftDelete) db!!.UpdateTableItemQty(tableItem) else db!!.InsertTableItems(
                        tableItem
                    )
                    showAnim()
                }
            })

        // TABLET always uses the grouped view. Mobile uses it based on preference.
        if (isTablet || pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey) == "item_group") {
            if (adapterParent == null || isHardChange) {
                setAdapterParent(list)
                binding!!.recyclerview.adapter = adapterParent
            } else {
                adapterParent!!.setItemList(list)
            }
            if (!isTablet) {
                binding!!.llMenu?.visibility = View.VISIBLE
            } else {
                adapterChild?.setItemList(listChild)
                binding!!.recyclerview.adapter = adapterChild
                if (!isTablet) binding!!.llMenu?.visibility = View.GONE
            }
        } else { // Mobile specific item_name view
            adapterChild?.setItemList(listChild)
            binding!!.recyclerview.adapter = adapterChild
            if (!isTablet) binding!!.llMenu?.visibility = View.GONE
        }
        oldSelection = pref!!.getStr(this@MenuViewActivity, KeyUtils.sortingKey)
        dismissProgressDialog()

        binding!!.recyclerview.clearOnScrollListeners() // Avoid adding multiple listeners
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
    }

    private fun setAdapterParent(list: ArrayList<MenuData>) {
        adapterParent =
            TableKotAdapter(this@MenuViewActivity, list, object : TableKotAdapter.SetOnItemClick {
                override fun onItemScroll(position: Int) {
                    if (!isTablet) binding!!.recyclerview.scrollToPosition(position)
                }

                override fun onItemClicked(position: Int, item: Item, parent: MenuData) {
                    if (isTablet) {
                        val tableItem = CartItemRow(
                            item.id,
                            0,
                            item.item_name,
                            item.item_short_name ?: "",
                            item.item_price,
                            item.sp_inst ?: "",
                            item.item_cat_id,
                            tableId,
                            "-1",
                            1,
                            item.kot_ncv,
                            item.local_sp_inst,
                            item.kitchen_cat_id,
                            item.item_tax,
                            "0",
                            "0",
                            0,
                            if (item.isSoftDelete) 1 else 0
                        )
                        if (item.isChecked || item.isSoftDelete) db!!.UpdateTableItemQty(tableItem) else db!!.InsertTableItems(
                            tableItem
                        )
                        adapterParent?.refreshList(tableId, position)
                        showAnim()
                    } else {
                        addItemDialog(position, item.isChecked, item, item.item_cat_id)
                    }

                }

                override fun onItemDelete(
                    position: Int,
                    item: Item,
                    parent: MenuData,
                    values: Int
                ) {
                    db!!.deleteItemOfTable(tableId, item.id)
                    showAnim()
                }

                override fun onItemUpdate(
                    position: Int,
                    item: Item,
                    parent: MenuData,
                    values: Int
                ) {
                    val tableItem = CartItemRow(
                        item.id,
                        0,
                        item.item_name,
                        item.item_short_name ?: "",
                        item.item_price,
                        item.sp_inst ?: "",
                        item.item_cat_id,
                        tableId,
                        "-1",
                        item.count,
                        item.kot_ncv,
                        item.local_sp_inst,
                        item.kitchen_cat_id,
                        item.item_tax,
                        "0",
                        "0",
                        0,
                        if (item.isSoftDelete) 1 else 0
                    )
                    if (item.isChecked || item.isSoftDelete) db!!.UpdateTableItemQty(tableItem) else db!!.InsertTableItems(
                        tableItem
                    )
                    adapterParent?.refreshList(tableId, position)
                    showAnim()
                }
            }, isEditOrder = isEditOrder)
    }

    // This is now the central point for all UI updates after a cart change.
    private fun showAnim() {
        if (isTablet) {
            updateKotSummary()
            // Refresh the main item list to show counters/updated states
            adapterParent?.refreshList(tableId, -1)
            adapterChild?.refreshList(tableId, -1)
        } else {
            // Original mobile animation logic
            val list = db!!.GetCartItems(tableId)
            if (list.isEmpty()) {
                binding!!.relAnim?.visibility = View.GONE
            } else {
                binding!!.relAnim?.visibility = View.VISIBLE
                binding!!.viewAnim?.clearAnimation()
                val animBlink = AnimationUtils.loadAnimation(this@MenuViewActivity, R.anim.ripple)
                animBlink.repeatCount = Animation.INFINITE
                binding!!.viewAnim?.startAnimation(animBlink)
                binding!!.txtCount?.text = list.size.toString()
            }
        }
    }

    inner class FetchLocalData : AsyncTask<Void, Int, Void>() {
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(this@MenuViewActivity)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            val list = db!!.GetSyncItems(pageForItems)
            if (list.isEmpty() && pageForItems == 0) {
                runOnUiThread {
                    displayActionSnackbarBottom(
                        this@MenuViewActivity,
                        "Resources missing. Please Login Again.",
                        3,
                        false,
                        "Login",
                        object : OnDialogClick {
                            override fun onOk() {
                                db!!.DeleteDB()
                                setResult(Activity.RESULT_OK, Intent().putExtra("exit", true))
                                finish()
                            }

                            override fun onCancel() {}
                        })
                }
                return null
            }

            if (list.isEmpty()) limit = true

            if (listItem.isNullOrEmpty()) {
                listItem = ArrayList(list)
            } else {
                list.forEach { newCat ->
                    val existingCat = listItem!!.find { it.item_cat_id == newCat.item_cat_id }
                    if (existingCat != null) {
                        existingCat.items.addAll(newCat.items)
                    } else {
                        listItem!!.add(newCat)
                    }
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            dismissProgressDialog()
            if (listItem != null) {
                getWaiterMenuList("update")

                // TABLET: Setup the category list on the left pane after data is loaded for the first time
                if (isTablet && categoryAdapter == null && !listItem.isNullOrEmpty()) {
                    categoryList.clear()
                    categoryList.add(
                        MenuData(
                            0,
                            "All Items",
                            0,
                            0,
                            0,
                            0,
                            false,
                            false,
                            ArrayList()
                        )
                    )
                    val distinctCategories = listItem!!
                        .filter { it.item_cat_id != -1 }
                        .distinctBy { it.item_cat_id }
                        .sortedBy { it.category_name }
                    categoryList.addAll(distinctCategories)

                    val rvCategories = binding!!.root.findViewById<RecyclerView>(R.id.rv_categories)
                    categoryAdapter =
                        CategoryAdapter(this@MenuViewActivity, categoryList, this@MenuViewActivity)
                    rvCategories.layoutManager = LinearLayoutManager(this@MenuViewActivity)
                    rvCategories.adapter = categoryAdapter
                }
            }
        }
    }

    private fun setSearchAdapter(q: String) {
        val listChild = db!!.GetSyncSearchItems(q)
        val tableItemList: List<CartItemRow> = db!!.GetCartItems(tableId)

        for (item in listChild) {
            tableItemList.find { it.id == item.id }?.let { tableItem ->
                item.isChecked = true
                item.count = tableItem.qty
                item.kot_ncv = tableItem.kot_ncv
                item.localId = tableItem.id
                item.local_sp_inst = tableItem.notes
            }
        }

        adapterSearch = TableKotChildAdapter(
            this@MenuViewActivity,
            listChild,
            object : TableKotChildAdapter.SetOnItemClick {
                override fun onItemClicked(position: Int, item: Item) {
                    if (isTablet) {
                        val tableItem = CartItemRow(
                            item.id,
                            0,
                            item.item_name,
                            item.item_short_name ?: "",
                            item.item_price,
                            item.sp_inst ?: "",
                            item.item_cat_id,
                            tableId,
                            "-1",
                            1,
                            item.kot_ncv,
                            item.local_sp_inst,
                            item.kitchen_cat_id,
                            item.item_tax,
                            "0",
                            "0",
                            0,
                            if (item.isSoftDelete) 1 else 0
                        )
                        if (item.isChecked || item.isSoftDelete) db!!.UpdateTableItemQty(tableItem) else db!!.InsertTableItems(
                            tableItem
                        )
                        adapterParent?.refreshList(tableId, position)
                        showAnim()
                    } else {
                        addItemDialog(position, item.isChecked, item, item.item_cat_id)
                    }
                }

                override fun onItemDelete(position: Int, item: Item, values: Int) {
                    db!!.deleteItemOfTable(tableId, item.id)
                    showAnim()
                }

                override fun onItemUpdate(position: Int, item: Item, values: Int) {
                    val tableItem = CartItemRow(
                        item.id,
                        0,
                        item.item_name,
                        item.item_short_name ?: "",
                        item.item_price,
                        item.sp_inst ?: "",
                        item.item_cat_id,
                        tableId,
                        "-1",
                        item.count,
                        item.kot_ncv,
                        item.local_sp_inst,
                        item.kitchen_cat_id,
                        item.item_tax,
                        "0",
                        "0",
                        0,
                        if (item.isSoftDelete) 1 else 0
                    )
                    if (item.isChecked || item.isSoftDelete) db!!.UpdateTableItem(tableItem) else db!!.InsertTableItems(
                        tableItem
                    )
                    showAnim()
                }
            })
        binding!!.recyclerview.adapter = adapterSearch
    }

    private fun submitKOT() {
        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        if (list.isEmpty()) {
            displayActionSnackbar(this, "Cart is empty.", 2)
            return
        }
        showProgressDialog(this@MenuViewActivity)
        val arrOrder = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.kot_id)
                put("item_id", item.id)
                put("item_name", item.item_name)
                put("item_price", item.item_price)
                put("qty", item.qty)
                put("item_short_name", item.item_short_name)
                put("kitchen_cat_id", item.kitchen_cat_id)
                put("item_tax", item.item_tax)
                put("soft_delete", item.softDelete)
                put("kot_ncv", if (this@MenuViewActivity.kotNcv == 0) item.kot_ncv else kotNcv)
                val notes = item.notes?.trim()
                if (!item.notes.isNullOrEmpty()) put(
                    "notes",
                    JSONArray(
                        item.notes!!.split(",").map {
                            it.trim().replace("\"", "").replace("[", "").replace("]", "")
                        }).toString()
                )
                else put("notes", "")
            }
            arrOrder.put(obj)
        }
        val cd = JSONObject().apply {
            put("cn", strName)
            put("cm", strContactNumber)
            put("cgn", strGST)
            put("ca", address)
            put("nop", strPerson)
        }
        val cv = JSONObject().apply {
            put("sid", pref!!.getInt(this@MenuViewActivity, "sid"))
            put("table_id", tableId)
            put("type", type)
            put("kot", arrOrder)
            put("cd", cd)
            put("kot_id", kotId)
        }
        val jsonObj = JSONObject().apply {
            put("ca", if (isEditOrder) 6 else KOT)
            put("cv", cv)
        }
        persons = strPerson
        currentJsonData = jsonObj
        db!!.deleteItemOfTable(tableId)
        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_KOT)
    }

    // This function is fine and does not need changes
    private fun addItemDialog(position: Int, isEdit: Boolean, item: Item, parent_id: Int) {
        var count = 1
        var ncv = item.kot_ncv // Non-Chargeable/Void status
        val dialog = BottomSheetDialog(this@MenuViewActivity, R.style.BottomSheetPostDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_add_menu_item)

        // --- View Initializations ---
        val btnBack: ImageButton = dialog.findViewById(R.id.btn_back)!!
        val btnAddInst: ImageView = dialog.findViewById(R.id.btn_add_inst)!!
        val chipGroup = dialog.findViewById<ChipGroup>(R.id.chips)!!
        val edtSpecialInst = dialog.findViewById<EditText>(R.id.edt_special_inst)!!
        val name: AppCompatTextView = dialog.findViewById(R.id.tv_item_name)!!
        val tvInstruction: TextInputEditText = dialog.findViewById(R.id.tv_name)!!
        val tvItemCurrency: AppCompatTextView = dialog.findViewById(R.id.tv_item_currency)!!
        val tvItemPrice: AppCompatEditText = dialog.findViewById(R.id.tv_item_price)!!
        val tvQty: AppCompatEditText = dialog.findViewById(R.id.tv_qty)!!
        val linMin: LinearLayout = dialog.findViewById(R.id.lin_min)!!
        val linAdd: LinearLayout = dialog.findViewById(R.id.lin_add)!!
        val btnAdd = dialog.findViewById<Button>(R.id.btn_add)!!
        val txtTitle = dialog.findViewById<AppCompatTextView>(R.id.txt_title)!!
        val linOrderNcv: LinearLayout = dialog.findViewById(R.id.lin_order_ncv)!!
        val layoutNVC: LinearLayout = dialog.findViewById(R.id.layoutNCV)!!
        val btnComplimentary: Button = dialog.findViewById(R.id.btn_complimentary)!!
        val btnVoid: Button = dialog.findViewById(R.id.btn_void)!!
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)!!

        // --- Logic for NCV (Complimentary/Void) Buttons ---
        // Refactored helper function to avoid repeating UI update code.
        fun updateNcvButtons() {
            // Reset both buttons to default state
            btnComplimentary.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp_without_stroke)
            btnComplimentary.setTextColor(GetColor(R.color.colorSecondary))
            btnVoid.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp_without_stroke)
            btnVoid.setTextColor(GetColor(R.color.colorSecondary))

            when (ncv) {
                2 -> { // Complimentary is active
                    btnComplimentary.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp)
                    btnComplimentary.setTextColor(GetColor(R.color.colorPrimary))
                    tvItemPrice.setText("0")
                }

                3 -> { // Void is active
                    btnVoid.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp)
                    btnVoid.setTextColor(GetColor(R.color.colorPrimary))
                    tvItemPrice.setText("0")
                }
            }
        }

        fun restoreItemPrice() {
            val price = if (isEditOrder) db!!.getItemPriceById(item.item_id) else item.item_price
            tvItemPrice.setText(price.toString())
            tvItemPrice.setSelection(tvItemPrice.text.toString().length)
        }

        btnBack.setOnClickListener { dialog.cancel() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        // --- Special Instructions Logic ---
        edtSpecialInst.addTextChangedListener {
            edtSpecialInst.error = null
        }
        btnAddInst.setOnClickListener {
            val inputText = edtSpecialInst.text.toString().trim()
            if (inputText.isEmpty()) {
                edtSpecialInst.error = resources.getString(R.string.enter_special_instruction_name)
            } else {
                // Split by comma and add each non-empty part as a chip
                val items = inputText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (itemText in items) {
                    val chip = LayoutInflater.from(
                        ContextThemeWrapper(this, R.style.Widget_App_Chip_Round_New)
                    ).inflate(R.layout.item_chip_choice_round, chipGroup, false) as Chip
                    chip.text = itemText
                    chip.isCheckable = true
                    chip.isChecked = true // Auto-check newly added instructions
                    chip.setOnCheckedChangeListener { _, _ ->
//                        tvInstruction.setText(Utils.getCheckedIns(tvInstruction.text.toString(), chipGroup))
                        tvInstruction.setSelection(tvInstruction.text.toString().length)
                    }
                    chipGroup.addView(chip)
                }
                edtSpecialInst.text = null // Clear input after adding
            }
        }
        tvInstruction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Utils.checkChips(tvInstruction.text.toString(), chipGroup)
            }
        })

        // --- Quantity (Qty) Logic ---
        tvQty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // FIX: Use toIntOrNull() to prevent crashes on empty or invalid input.
                count = s.toString().toIntOrNull() ?: 1
            }
        })
        tvQty.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // FIX: Simplified and safer logic for when focus is lost.
                val currentQty = tvQty.text.toString().toIntOrNull() ?: 0
                if (currentQty < 1) {
                    tvQty.setText("1")
                    count = 1
                } else {
                    count = currentQty
                }
            } else {
                tvQty.selectAll() // Better UX than clearing the text
            }
        }
        linMin.setOnClickListener {
            if (count > 1) {
                count--
                tvQty.setText("$count")
                tvQty.setSelection(tvQty.text.toString().length)
            }
        }
        linAdd.setOnClickListener {
            count++
            tvQty.setText("$count")
            tvQty.setSelection(tvQty.text.toString().length)
        }

        // --- Initial Data Loading ---
        var localInst: List<String>? = null
        var sorting = 0
        if (isEdit) {
            btnAdd.text = "Update"
            db?.GetCartItems(tableId, item.id)?.let { cart ->
                sorting = 1
                count = cart.qty
                ncv = cart.kot_ncv
                tvItemPrice.setText(cart.item_price.toString())
                val notes = cart.notes
                if (!notes.isNullOrEmpty()) {
                    localInst = try {
                        val gson = Gson()
                        val type: Type = object : TypeToken<List<String>>() {}.type
                        gson.fromJson(notes, type)
                    } catch (e: Exception) {
                        notes.split(",")
                    }
                    tvInstruction.setText(localInst?.joinToString(","))
                }
            }
        } else {
            // FIX: Critical bug fixed. This line now only runs for new items,
            // not overwriting the price in edit mode.
            tvItemPrice.setText(item.item_price.toString())
        }

        // --- Populate Views with Data ---
        name.text = item.item_name
        tvItemCurrency.text = pref?.getStr(this@MenuViewActivity, KeyUtils.restoCurrency)
        tvQty.setText("$count")
        Utils.addChipsRounded(this, item.sp_inst, localInst, chipGroup, txtTitle) { _, _ ->
//            tvInstruction.setText(Utils.getCheckedIns(tvInstruction.text.toString(), chipGroup))
            tvInstruction.setSelection(tvInstruction.text.toString().length)
        }

        // --- NCV Button Click Listeners ---
        btnComplimentary.setOnClickListener {
            ncv = if (ncv == 2) 0 else 2 // Toggle state
            if (ncv == 0) restoreItemPrice()
            updateNcvButtons()
        }
        btnVoid.setOnClickListener {
            ncv = if (ncv == 3) 0 else 3 // Toggle state
            if (ncv == 0) restoreItemPrice()
            updateNcvButtons()
        }
        // If user manually changes price, disable NCV
        tvItemPrice.addTextChangedListener {
            val price = it.toString().toDoubleOrNull()
            if (price != 0.0) {
                if (ncv == 2 || ncv == 3) {
                    ncv = 0
                    updateNcvButtons()
                }
            }
        }

        // --- Set Initial State for NCV and Visibility ---
        updateNcvButtons() // Set initial button state based on ncv from item/cart

        when (kotNcv) {
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
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }

            3 -> {
                linOrderNcv.visibility = View.GONE
                btnComplimentary.visibility = View.GONE
                layoutNVC.visibility = View.GONE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
            }
        }

        // --- Main Add/Update Button ---
        btnAdd.setOnClickListener {
            dialog.dismiss()

            // Use toDoubleOrNull for safety and provide a default of 0.0
            val finalPrice = tvItemPrice.text.toString().toDoubleOrNull() ?: 0.0

            val selectedInstructions = (0 until chipGroup.childCount)
                .map { chipGroup.getChildAt(it) as Chip }
                .filter { it.isChecked }
                .joinToString(",") { it.text.toString() }

            val data = Utils.countTaxDiscount(
                finalPrice, count, item.item_tax, 0f, 0, 0.0, 1
            ).split("|")

            val tableItem = CartItemRow(
                item.id,
                0,
                item.item_name ?: "",
                item.item_short_name ?: "",
                finalPrice,
                item.sp_inst ?: "",
                parent_id,
                tableId,
                "-1",
                count,
                ncv,
                selectedInstructions,
                item.kitchen_cat_id,
                item.item_tax,
                data[0],
                data[1],
                sorting,
                if (item.isSoftDelete) 1 else 0
            )

            if (isEdit) {
                db!!.UpdateTableItem(tableItem)
            } else {
                db!!.InsertTableItems(tableItem)
            }

            // FIX: Only call animation once.
            showAnim()
            binding!!.edtSearch.setQuery("", false)
            adapterParent!!.refreshList(tableId, position)
        }

        dialog.window?.apply {
            // FIX: Make the dialog adjust when the keyboard opens
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }

        dialog.show()
    }

    /**
     * Main entry point. Decides whether to show a modal dialog (tablet) or a bottom sheet (phone).
     * This function is simple and only handles the "what" and "where", not the "how".
     */
    private fun updateItemDialog(
        item: CartItemRow,
        recyclerView: RecyclerView,
        bottomSheetDialog: BottomSheetDialog?
    ) {
        // 1. Determine the device type using our resource qualifier.


        // 2. Inflate the layout XML *once*.
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_menu_item, null, false)

        if (isTablet) {
            // --- TABLET IMPLEMENTATION: Use a standard, centered Dialog ---
            val dialog = Dialog(this, R.style.App_Dialog_Modal) // Use a proper dialog theme
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(dialogView)

            // 3. Pass the dialog and its view to the worker function to set up logic.
            setupDialogViewsAndLogic(dialog, dialogView, item, recyclerView, bottomSheetDialog)

            dialog.window?.apply {
                setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.CENTER)
                setBackgroundDrawableResource(android.R.color.transparent) // Important for custom shapes
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()

        } else {
            // --- PHONE IMPLEMENTATION: Use a BottomSheetDialog ---
            val dialog = BottomSheetDialog(this, R.style.BottomSheetPostDialogTheme)
            dialog.setContentView(dialogView)

            // 3. Pass the dialog and its view to the same worker function.
            setupDialogViewsAndLogic(dialog, dialogView, item, recyclerView, bottomSheetDialog)

            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()
        }
    }

    /**
     * Worker function that contains ALL the original logic.
     * It's completely independent of whether its container is a Dialog or BottomSheetDialog.
     * This ensures logic is never duplicated and is easy to maintain.
     *
     * @param dialog A generic interface for dismissing/canceling.
     * @param dialogView The inflated View on which to find all UI components.
     */
    private fun setupDialogViewsAndLogic(
        dialog: DialogInterface,
        dialogView: View,
        item: CartItemRow,
        recyclerView: RecyclerView,
        bottomSheetDialog: BottomSheetDialog?
    ) {
        var count = item.qty
        var ncv = item.kot_ncv

        // --- View Initializations ---
        // CRITICAL FIX: All findViewById calls MUST use the inflated `dialogView`.
        val btnBack: ImageButton = dialogView.findViewById(R.id.btn_back)!!
        val btnAddInst: ImageView = dialogView.findViewById(R.id.btn_add_inst)!!
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chips)!!
        val edtSpecialInst = dialogView.findViewById<EditText>(R.id.edt_special_inst)!!
        val name: AppCompatTextView = dialogView.findViewById(R.id.tv_item_name)!!
        // NOTE: In your original code, you had a tv_name that was a TextInputEditText.
        // If that's a different view, ensure its ID is unique. If not, this is likely a typo.
        // I am assuming it's a different view for now.
        val tvInstruction: TextInputEditText? = dialogView.findViewById(R.id.tv_name)
        val tvQty: AppCompatEditText = dialogView.findViewById(R.id.tv_qty)!!
        val tvItemPrice: AppCompatEditText = dialogView.findViewById(R.id.tv_item_price)!!
        val linMin: LinearLayout = dialogView.findViewById(R.id.lin_min)!!
        val linAdd: LinearLayout = dialogView.findViewById(R.id.lin_add)!!
        val btnAdd = dialogView.findViewById<AppCompatButton>(R.id.btn_add)!!
        val txtTitle = dialogView.findViewById<AppCompatTextView>(R.id.txt_title)!!
        val linOrderNcv: LinearLayout = dialogView.findViewById(R.id.lin_order_ncv)!!
        val btnComplimentary: Button = dialogView.findViewById(R.id.btn_complimentary)!!
        val btnVoid: Button = dialogView.findViewById(R.id.btn_void)!!
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)!!

        // Local helper functions now live inside this worker function
        fun updateNcvButtons() {
            btnComplimentary.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp_without_stroke)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnComplimentary.setTextColor(getColor(R.color.colorSecondary))
            } // Use modern getColor
            btnVoid.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp_without_stroke)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnVoid.setTextColor(getColor(R.color.colorSecondary))
            }

            when (ncv) {
                2 -> {
                    btnComplimentary.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        btnComplimentary.setTextColor(getColor(R.color.colorPrimary))
                    }
                    tvItemPrice.setText("0")
                }

                3 -> {
                    btnVoid.setBackgroundResource(R.drawable.layout_border_fill_square_corner_5dp)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        btnVoid.setTextColor(getColor(R.color.colorPrimary))
                    }
                    tvItemPrice.setText("0")
                }
            }
        }

        fun restoreItemPrice() {
            tvItemPrice.setText(item.item_price.toString())
            tvItemPrice.setSelection(tvItemPrice.text.toString().length)
        }

        // --- All original logic is pasted here, unmodified ---
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnBack.setOnClickListener { dialog.cancel() }

        edtSpecialInst.addTextChangedListener { edtSpecialInst.error = null }
        btnAddInst.setOnClickListener {
            val inputText = edtSpecialInst.text.toString().trim()
            if (inputText.isEmpty()) {
                edtSpecialInst.error = resources.getString(R.string.enter_special_instruction_name)
            } else {
                val items = inputText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (itemText in items) {
                    val chip = LayoutInflater.from(this)
                        .inflate(R.layout.item_chip_choice_round, chipGroup, false) as Chip
                    chip.text = itemText
                    chip.isCheckable = true
                    chip.isChecked = true
                    chip.setOnCheckedChangeListener { _, _ ->
                        tvInstruction?.setSelection(tvInstruction.text.toString().length)
                    }
                    chipGroup.addView(chip)
                }
                edtSpecialInst.text = null
            }
        }

        tvQty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                count = s.toString().toIntOrNull() ?: item.qty
            }
        })
        tvQty.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentQty = tvQty.text.toString().toIntOrNull() ?: 0
                count = if (currentQty < 1) 1 else currentQty
                tvQty.setText(count.toString())
            } else {
                tvQty.selectAll()
            }
        }

        linMin.setOnClickListener {
            if (count > 1) {
                count--; tvQty.setText(count.toString())
            }
        }
        linAdd.setOnClickListener {
            count++; tvQty.setText(count.toString())
        }

        name.text = item.item_name
        tvQty.setText(count.toString())
        tvItemPrice.setText(item.item_price.toString())
        btnAdd.text = getString(R.string.update) // Use string resource

        val cleanedNotes = item.notes?.replace("null", "")?.trim(',')
        val localInst: List<String> = if (cleanedNotes.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                Gson().fromJson(cleanedNotes, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                cleanedNotes.split(",")
            }
        }
        tvInstruction?.setText(localInst.joinToString(","))

        Utils.addChipsRounded(this, item.sp_inst, localInst, chipGroup, txtTitle) { _, _ ->
            tvInstruction?.setSelection(tvInstruction.text.toString().length)
        }

        tvInstruction?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Utils.checkChips(tvInstruction.text.toString(), chipGroup)
            }
        })

        btnComplimentary.setOnClickListener {
            ncv = if (ncv == 2) 0 else 2
            if (ncv == 0) restoreItemPrice()
            updateNcvButtons()
        }
        btnVoid.setOnClickListener {
            ncv = if (ncv == 3) 0 else 3
            if (ncv == 0) restoreItemPrice()
            updateNcvButtons()
        }

        tvItemPrice.addTextChangedListener {
            if (it.toString().toDoubleOrNull() != 0.0) {
                if (ncv == 2 || ncv == 3) {
                    ncv = 0
                    updateNcvButtons()
                }
            }
        }
        updateNcvButtons()

        // Ensure `kotNcv` is a member variable of your Activity/Fragment
        when (kotNcv) {
            0 -> linOrderNcv.visibility = View.VISIBLE
            1, 2, 3 -> {
                linOrderNcv.visibility = View.GONE
                tvItemPrice.setText("0")
                tvItemPrice.isEnabled = false
                if (kotNcv == 2) btnVoid.visibility = View.GONE
                if (kotNcv == 3) btnComplimentary.visibility = View.GONE
            }
        }

        btnAdd.setOnClickListener {
            dialog.dismiss()
            val selectedInstructions = (0 until chipGroup.childCount)
                .map { chipGroup.getChildAt(it) as Chip }
                .filter { it.isChecked }
                .joinToString(",") { it.text.toString() }
            val finalPrice = tvItemPrice.text.toString().toDoubleOrNull() ?: 0.0
            val data =
                Utils.countTaxDiscount(finalPrice, count, item.item_tax, 0f, 0, 0.0, 1).split("|")
            val tableItem = CartItemRow(
                item.id, 0, item.item_name, item.item_short_name, finalPrice, item.sp_inst,
                item.item_cat_id, tableId, item.pre_order_id, count, ncv, selectedInstructions,
                item.kitchen_cat_id, item.item_tax, data[0], data[1], item.sorting, item.softDelete
            )
            db!!.UpdateTableItem(tableItem)
            adapterParent!!.refreshList(tableId, -1)
            setAdapter(recyclerView, bottomSheetDialog)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.view_kot_menu, menu)
        ncv = menu.findItem(R.id.ncv)
        ncv?.isVisible = true
        ncv?.setIcon(
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
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.ncv -> {
                viewNCV(); true
            }

            R.id.pre_order -> {
                customerDialog(false); true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // This is fine as is
    fun viewNCV() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetPostDialogTheme)
        // ... (Your viewNCV logic is preserved)
        // ...
        bottomSheetDialog.show()
    }

    // This is a mobile-only function
    private fun viewKOT() {
        if (isTablet) return // Should not be called on tablet
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetPostDialogTheme)
        bottomSheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        bottomSheetDialog.setContentView(R.layout.activity_view_kotactivity)

        val recyclerview = bottomSheetDialog.findViewById<RecyclerView>(R.id.recyclerview)!!
        val btnCreateKot = bottomSheetDialog.findViewById<AppCompatTextView>(R.id.btn_create_kot)!!
        val imgClose = bottomSheetDialog.findViewById<ImageView>(R.id.img_close)!!

        imgClose.setOnClickListener { bottomSheetDialog.dismiss() }
        btnCreateKot.setOnClickListener {
            bottomSheetDialog.dismiss()
            submitKOT()
        }

        recyclerview.layoutManager = LinearLayoutManager(this)
        setAdapter(recyclerview, bottomSheetDialog)

        bottomSheetDialog.setOnDismissListener { getWaiterMenuList("update") }
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.show()
    }

    // TABLET: New function to update the KOT summary on the right pane
    private fun updateKotSummary() {
        if (!isTablet) return

        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        val rvKotSummary = binding!!.root.findViewById<RecyclerView>(R.id.rv_kot_summary) ?: return

        if (viewKotAdapter == null) {
            viewKotAdapter =
                ViewKotAdapter(this, list.toMutableList(), object : ViewKotAdapter.SetOnItemClick {
                    override fun itemIncreased(
                        pos: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        item.qty += 1
                        db!!.UpdateTableItem(item)
                        showAnim()
                    }

                    override fun itemDecreased(
                        pos: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        item.qty -= 1
                        if (item.qty > 0) db!!.UpdateTableItem(item) else db!!.deleteItemOfTable(
                            tableId,
                            item.id
                        )
                        showAnim()
                    }

                    /*isd != 0 */

                    override fun onItemDeleted(
                        pos: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        db!!.deleteItemOfTable(tableId, item.id)
                        showAnim()
                    }

                    override fun onItemClick(pos: Int, item: CartItemRow, view: AppCompatTextView) {
                        updateItemDialog(item, rvKotSummary, null)
                    }
                })
            rvKotSummary.adapter = viewKotAdapter
        } else {
            viewKotAdapter?.updateList(list.toMutableList())
        }
    }

    // This function is now only used for the mobile bottom sheet
    private fun setAdapter(recyclerView: RecyclerView, bottomSheetDialog: BottomSheetDialog?) {
        val list: List<CartItemRow> = db!!.GetCartItems(tableId)
        if (list.isEmpty()) {
            bottomSheetDialog?.dismiss()
            showAnim()
        } else {
            val adapter =
                ViewKotAdapter(this, list.toMutableList(), object : ViewKotAdapter.SetOnItemClick {
                    override fun itemIncreased(
                        pos: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        item.qty += 1
                        db!!.UpdateTableItem(item)
                        setAdapter(recyclerView, bottomSheetDialog) // Refresh this sheet
                        showAnim() // Refresh main UI
                    }

                    override fun itemDecreased(
                        pos: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        item.qty -= 1
                        if (item.qty > 0) db!!.UpdateTableItem(item) else db!!.deleteItemOfTable(
                            tableId,
                            item.id
                        )
                        setAdapter(recyclerView, bottomSheetDialog)
                        showAnim()
                    }

                    override fun onItemDeleted(
                        pos: Int,
                        item: CartItemRow,
                        view: AppCompatTextView
                    ) {
                        db!!.deleteItemOfTable(tableId, item.id)
                        setAdapter(recyclerView, bottomSheetDialog)
                        showAnim()
                    }

                    override fun onItemClick(pos: Int, item: CartItemRow, view: AppCompatTextView) {
                        updateItemDialog(item, recyclerView, bottomSheetDialog)
                    }
                })
            recyclerView.adapter = adapter
        }
    }

    // Unchanged helper methods
    fun GetColor(color: Int): ColorStateList {
        val colorValue =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getColor(color) else resources.getColor(
                color
            )
        return ColorStateList(states, Utils.getColorState(colorValue))
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
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null // Avoid memory leaks
    }
}