package com.eresto.captain.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import com.eresto.captain.R
import com.eresto.captain.adapter.TableListAdapter
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityTabBinding
import com.eresto.captain.model.GetTables
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.SocketForegroundService
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

class TabActivity : BaseActivity() {
    private var binding: ActivityTabBinding? = null
    var list: List<GetTables>? = null
    var type = 1
    var isCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        binding!!.toolbar.title = "Dine-In"
        setSupportActionBar(binding!!.toolbar)
        binding!!.toolbar.setNavigationOnClickListener {
            logoutDialog()
        }
        binding!!.swipeContainer.setOnRefreshListener {
            val jsonObj = JSONObject()
            val cv = JSONObject()
            jsonObj.put("ca", GET_TABLE)
            cv.put("sid", pref!!.getInt(this, "sid"))
            jsonObj.put("cv", cv)
            sendMessageToServer(jsonObj, SocketForegroundService.ACTION_TAB)
            setCallBack(object : OnResponseFromServerPOS {
                override fun onResponse(json: String) {
                    binding!!.swipeContainer.isRefreshing = false
                    setTable()
                }
            })
        }
        setTable()
    }

    override fun onResume() {
        super.onResume()
        val jsonObj = JSONObject()
        val cv = JSONObject()
        jsonObj.put("ca", GET_TABLE)
        cv.put("sid", pref!!.getInt(this, "sid"))
        jsonObj.put("cv", cv)
        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_TAB)
        setCallBack(object : OnResponseFromServerPOS {
            override fun onResponse(json: String) {
                binding!!.swipeContainer.isRefreshing = false
                setTable()
            }
        })
    }

    fun setTable() {
        val db = DBHelper(this@TabActivity)
        list = db.GetTables(type)
        if (!list.isNullOrEmpty()) {
            binding!!.rvTable2.also {
                it.layoutManager = GridLayoutManager(this@TabActivity, 3)
            }
            val adapter = TableListAdapter(this@TabActivity, list!!, type, object :
                TableListAdapter.SetOnItemClick {

                override fun onItemClick(
                    position: Int,
                    tab: GetTables
                ) {
                    when (tab.tab_status) {
                        1 -> {
                            val intent = Intent(this@TabActivity, MenuViewActivity::class.java)
                            intent.putExtra("table_name", tab.tab_label)
                            intent.putExtra("type", type)
                            intent.putExtra("table_id", tab.id)
                            intent.putExtra("name", "")
                            intent.putExtra("number", "")
                            intent.putExtra("address", "")
                            intent.putExtra("is_free_table", true)
                            resultLauncher.launch(intent)
                        }

                        2 -> {

                            val jsonObj = JSONObject()
                            jsonObj.put("ca", GET_TABLE_ORDER)
                            val cv = JSONObject()
                            cv.put("tab_id", tab.id)
                            cv.put("sid", pref!!.getInt(this@TabActivity, "sid"))
                            jsonObj.put("cv", cv)
                            sendMessageToServer(jsonObj, SocketForegroundService.ACTION_ORDER)
                            setCallBack(object : OnResponseFromServerPOS {
                                override fun onResponse(json: String) {
                                    if (!isCalled) {
                                        isCalled = true
                                        setTable()
                                        try {
                                            val jsonObjData = JSONObject(json)
                                            if (jsonObjData.getJSONObject("order_details")
                                                    .getJSONObject("order")
                                                    .getInt("tab_status") != 1
                                            ) {
                                                val intent =
                                                    Intent(
                                                        this@TabActivity,
                                                        QSRKOTActivity::class.java
                                                    )
                                                intent.putExtra("table_name", tab.tab_label)
                                                intent.putExtra("type", type)
                                                intent.putExtra("table_id", tab.id)
                                                intent.putExtra("message", json)
                                                resultLauncher.launch(intent)
                                            } else {
                                                val intent = Intent(
                                                    this@TabActivity,
                                                    MenuViewActivity::class.java
                                                )
                                                intent.putExtra("table_name", tab.tab_label)
                                                intent.putExtra("type", type)
                                                intent.putExtra("table_id", tab.id)
                                                intent.putExtra("name", "")
                                                intent.putExtra("number", "")
                                                intent.putExtra("address", "")
                                                intent.putExtra("is_free_table", true)
                                                resultLauncher.launch(intent)
                                            }
                                        } catch (e: JSONException) {
                                            val intent = Intent(
                                                this@TabActivity,
                                                MenuViewActivity::class.java
                                            )
                                            intent.putExtra("table_name", tab.tab_label)
                                            intent.putExtra("type", type)
                                            intent.putExtra("table_id", tab.id)
                                            intent.putExtra("name", "")
                                            intent.putExtra("number", "")
                                            intent.putExtra("address", "")
                                            intent.putExtra("is_free_table", true)
                                            resultLauncher.launch(intent)
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            })
                        }

                        else -> {
                            displayActionSnackbarBottom(
                                this@TabActivity,
                                "Make Operation from POS",
                                2,
                                false,
                                "Okay",
                                object : OnDialogClick {
                                    override fun onOk() {
                                    }

                                    override fun onCancel() {
                                    }
                                })
                        }
                    }

                }

                override fun onEdit(position: Int, tab: GetTables) {

                }
            })
            binding!!.rvTable2.adapter = adapter
            binding!!.rvTable2.visibility = View.VISIBLE
            binding!!.linNo.visibility = View.GONE
        } else {
            binding!!.rvTable2.visibility = View.GONE
            binding!!.linNo.visibility = View.VISIBLE
        }
    }

    private fun logoutDialog() {
        val dialog = Dialog(this@TabActivity, R.style.DialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_logout)
        Objects.requireNonNull(dialog.window)
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
        val no = dialog.findViewById(R.id.tv_no) as AppCompatTextView
        no.setOnClickListener {
            dialog.dismiss()
        }
        val yes = dialog.findViewById(R.id.tv_yes) as AppCompatTextView
        yes.setOnClickListener {
            dialog.dismiss()
            pref!!.clearSharedPreference(this@TabActivity)
            val bool = db!!.DeleteDB()
            val i = Intent(this@TabActivity, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) { // There are no request codes
                if (result.data != null && result.data!!.getBooleanExtra("exit", false)) {
                    val mIntent =
                        Intent(this@TabActivity, LoginActivity::class.java)
                    startActivity(mIntent)
                    finish()
                } else {
                    setTable()
                }
            }
            isCalled = false
        }

}