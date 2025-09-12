package com.eresto.captain.ui.tables

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.eresto.captain.R
import com.eresto.captain.adapter.TableListAdapter
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityTabBinding
import com.eresto.captain.model.GetTables
import com.eresto.captain.ui.MenuViewActivity
import com.eresto.captain.ui.QSRKOTActivity
import com.eresto.captain.utils.ConfirmationDialogParams
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.DialogManager
import com.eresto.captain.utils.OnConfirmationClick
import com.eresto.captain.utils.SocketForegroundService
import org.json.JSONException
import org.json.JSONObject

class TabActivity : BaseActivity() {
    private lateinit var binding: ActivityTabBinding
    var type = 1
    var isTablet = false

    // Initialize the ViewModel using the factory to handle constructor dependencies.
    private val viewModel: TabViewModel by viewModels {
        TabViewModelFactory(TabRepository(this), DBHelper(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isTablet = resources.getBoolean(R.bool.isTablet)
        binding.toolbar.title = "Dine-In"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.tvRestoName.text = pref!!.getStr(this, "resto_name")
        binding.ivUserAvatar.setOnClickListener {
            logoutDialog()
        }
        binding.ivLogo.isVisible = isTablet

        // Observe the LiveData from the ViewModel.
        // This block will execute automatically whenever the table list changes.
        viewModel.tableList.observe(this) { tables ->
            if (tables != null) {
                Log.d("TabActivity", "Table list updated with ${tables.size} items.")
                updateTableList(tables)
            } else {
                Log.e("TabActivity", "Table list received from ViewModel is null!")
            }
        }

        // Swipe to refresh sends a single request for new data.
        binding.swipeContainer.setOnRefreshListener {
            viewModel.requestFreshTableData(pref!!)
            binding.swipeContainer.isRefreshing = false
        }

        // Setup the single, intelligent callback to link the BroadcastReceiver's response to our ViewModel.
        // This is the ONLY place setCallBack should be called in this activity.
        setCallBack(object : OnResponseFromServerPOS {
            override fun onResponse(json: String) {
                Log.d("TabActivity", "onResponse received for action: ${SocketForegroundService.CURRENT_ACTION}")
                Log.d("TabActivity", "onResponse JSON: $json")

                // Use the action to decide what to do
                when (SocketForegroundService.CURRENT_ACTION) {
                    SocketForegroundService.ACTION_ORDER -> {
                        // This is a response for a specific occupied table
                        handleTableOrderResponseFromSocket(json)
                    }
                    else -> {
                        // This handles all other cases, including general table refreshes.
                        // The server has sent new data, which BaseActivity has saved to the DB.
                        // Now, we tell the ViewModel to reload its list from the DB to update the UI.
                        Log.d("TabActivity", "Received a general update. Refreshing tables from DB.")
                        viewModel.refreshTablesFromDb()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // When the screen becomes visible, request a single, fresh update of table data.
        viewModel.requestFreshTableData(pref!!)
    }

    override fun onPause() {
        super.onPause()
    }

    /**
     * Updates the RecyclerView with a new list of tables.
     * Handles creating the adapter for the first time or updating an existing one.
     */
    private fun updateTableList(tables: List<GetTables>) {
        if (tables.isNullOrEmpty()) {
            binding.rvTable2.visibility = View.GONE
            binding.linNo.visibility = View.VISIBLE
            return
        }

        binding.rvTable2.visibility = View.VISIBLE
        binding.linNo.visibility = View.GONE

        if (binding.rvTable2.layoutManager == null) {
            val columnCount = resources.getInteger(R.integer.table_grid_columns)
            binding.rvTable2.layoutManager = GridLayoutManager(this, columnCount)
        }

        val adapter = binding.rvTable2.adapter as? TableListAdapter
        if (adapter == null) {
            binding.rvTable2.adapter = TableListAdapter(this, tables, type, tableClickListener)
        } else {
            adapter.updateData(tables)
        }
    }

    /**
     * A single listener object for all table click events in the RecyclerView.
     */
    private val tableClickListener = object : TableListAdapter.SetOnItemClick {
        override fun onItemClick(position: Int, tab: GetTables) {
            when (tab.tab_status) {
                1 -> { // Free table
                    val intent = Intent(this@TabActivity, MenuViewActivity::class.java)
                    intent.putExtra("table_name", tab.tab_label)
                    intent.putExtra("type", type)
                    intent.putExtra("table_id", tab.id)
                    intent.putExtra("is_free_table", true)
                    resultLauncher.launch(intent)
                }

                2 -> { // Occupied table
                    // Send the request. The single callback in onCreate will handle the response.
                    // DO NOT set a new callback here.
                    val jsonObj = JSONObject()
                    jsonObj.put("ca", GET_TABLE_ORDER)
                    val cv = JSONObject()
                    cv.put("tab_id", tab.id)
                    cv.put("sid", pref!!.getInt(this@TabActivity, "sid"))
                    jsonObj.put("cv", cv)
                    sendMessageToServer(jsonObj, SocketForegroundService.ACTION_ORDER)
                }

                else -> {
                    displayActionSnackbarBottom(
                        this@TabActivity,
                        "Make Operation from Eresto Edge",
                        2,
                        false,
                        "Okay",
                        object : OnDialogClick {
                            override fun onOk() {}
                            override fun onCancel() {}
                        })
                }
            }
        }

        override fun onEdit(position: Int, tab: GetTables) {
            // Handle edit action if needed in the future.
        }
    }

    /**
     * Handles the specific response for an ACTION_ORDER from the main callback.
     */
    private fun handleTableOrderResponseFromSocket(json: String) {
        if (json.isEmpty()) {
            Log.e("TabActivity", "Received empty response for ACTION_ORDER")
            return
        }

        try {
            val jsonObjData = JSONObject(json)
            val tableId = jsonObjData.getJSONObject("od").getJSONObject("order").getInt("table_id")
            val table = viewModel.tableList.value?.find { it.id == tableId }

            if (table != null) {
                // We found the table, proceed to launch the correct activity.
                handleTableOrderResponse(json, table)
            } else {
                Log.e("TabActivity", "Could not find table with id $tableId in the current list.")
                // As a fallback, refresh the whole list in case the data is stale.
                viewModel.refreshTablesFromDb()
            }
        } catch (e: JSONException) {
            Log.e("TabActivity", "Error parsing table order response", e)
        }
    }

    /**
     * Processes the JSON response for a specific table's order details and launches the correct Activity.
     */
    private fun handleTableOrderResponse(json: String, tab: GetTables) {
        try {
            val jsonObjData = JSONObject(json)
            if (jsonObjData.getJSONObject("od").getJSONObject("order").getInt("tab_status") != 1) {
                val intent = Intent(this@TabActivity, QSRKOTActivity::class.java)
                intent.putExtra("table_name", tab.tab_label)
                intent.putExtra("type", type)
                intent.putExtra("table_id", tab.id)
                intent.putExtra("message", json)
                resultLauncher.launch(intent)
            } else {

                // The table has become free, launch the menu to start a new order
                val intent = Intent(this@TabActivity, MenuViewActivity::class.java)
                intent.putExtra("table_name", tab.tab_label)
                intent.putExtra("type", type)
                intent.putExtra("table_id", tab.id)
                intent.putExtra("is_free_table", true)
                resultLauncher.launch(intent)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Fallback to open the menu if JSON parsing fails.
            val intent = Intent(this@TabActivity, MenuViewActivity::class.java)
            intent.putExtra("table_name", tab.tab_label)
            intent.putExtra("type", type)
            intent.putExtra("table_id", tab.id)
            intent.putExtra("is_free_table", true)
            resultLauncher.launch(intent)
        }
    }

    /**
     * Displays the logout confirmation dialog and handles the logout action.
     */
    private fun logoutDialog() {
        val params = ConfirmationDialogParams(
            logo = R.drawable.ic_logo, // Your logo resource
            title = "Are you sure you want to log out?",
            subtitle = "Logging out will stop your active session.",
            positiveButtonText = "Log Out",
            negativeButtonText = "Cancel",
            clickListener = object : OnConfirmationClick {
                override fun onPositive() {
                    dialog?.dismiss()
                    val jsonObj = JSONObject()
                    jsonObj.put("ca", LOGOUT)
                    jsonObj.put("ui", pref!!.getInt(this@TabActivity, "user_id"))
                    val cv = JSONObject()
                    cv.put("sid", pref!!.getInt(this@TabActivity, "sid"))
                    jsonObj.put("cv", cv)
                    sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGOUT)
                    Log.d("ConfirmationDialog", "Logging out...")
                }

                override fun onNegative() {
                    Log.d("ConfirmationDialog", "Logout cancelled.")
                }
            }
        )
        DialogManager.showConfirmationDialog(this, params)
    }

    /**
     * Handles the result when returning from another activity (e.g., MenuViewActivity).
     */
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // When returning from an activity that might have changed table status,
                // always request a fresh update from the server.
                viewModel.requestFreshTableData(pref!!)
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                dialogDefaultPrinterSetting(this)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}