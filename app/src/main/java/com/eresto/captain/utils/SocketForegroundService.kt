package com.eresto.captain.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.eresto.captain.R
import com.eresto.captain.base.BaseActivity.Companion.CLOSE_ORDER
import com.eresto.captain.base.BaseActivity.Companion.CONNECT
import com.eresto.captain.base.BaseActivity.Companion.CUSTOMER_DETAILS
import com.eresto.captain.base.BaseActivity.Companion.ERROR
import com.eresto.captain.base.BaseActivity.Companion.GET_TABLE
import com.eresto.captain.base.BaseActivity.Companion.GET_TABLE_ORDER
import com.eresto.captain.base.BaseActivity.Companion.KOT
import com.eresto.captain.base.BaseActivity.Companion.LOGOUT
import com.eresto.captain.base.BaseActivity.Companion.SHOW_PROGRESS
import com.eresto.captain.base.BaseActivity.Companion.WARRING
import com.eresto.captain.model.GetTables
import com.eresto.captain.model.MenuData
import com.eresto.captain.model.PrinterRespo
import com.eresto.captain.model.kitCat
import com.eresto.captain.ui.LoginActivity
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SocketForegroundService : Service() {

    private var mSocket: Socket? = null
    private var input: BufferedReader? = null
    private var output: PrintWriter? = null

    private val CHANNEL_ID = "SocketForegroundService"
    private  var retryCont = 0

    private var isRunning = false
    private lateinit var executor: ExecutorService
    private val TAG = "MyServerService"
    private val JOINT = "~"

    companion object {
        const val ACTION_TAB = "com.eresto.captain.action.TAB"
        const val ACTION_KOT = "com.eresto.captain.action.KOT"
        const val ACTION_ORDER = "com.eresto.captain.action.ORDER"
        const val ACTION_LOGIN = "com.eresto.captain.action.LOGIN"
        const val ACTION_LOGOUT = "com.eresto.captain.action.LOGOUT"
        const val ACTION_CUSTOMER = "com.eresto.captain.action.CUSTOMER"
        var CURRENT_ACTION = ""
    }

    interface SocketServiceCallback {
        fun onMessageReceived(action: JSONObject, message: JSONObject)
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Start Command")
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            startForeground(
                1,
                notification
            );
        }
        executor = Executors.newSingleThreadExecutor()
        executor.execute {
            startServer(this, intent)
        }
        return START_STICKY
    }

    private fun startServer(context: Context, intent: Intent?) {
        try {
            val pref = Preferences()
            val serverIp = pref.getStr(this, "ip_address")
            val serverPort = pref.getInt(this, "port")
            mSocket = Socket(serverIp, serverPort)
            retryCont = 0
            Log.d(TAG, "Socket Created")
            input = BufferedReader(InputStreamReader(mSocket!!.getInputStream()))
            output = PrintWriter(mSocket!!.getOutputStream(), true)
            mSocket!!.keepAlive = true
            if (intent?.getStringExtra("json") != null) {
                makeConnection(context, JSONObject(intent.getStringExtra("json")!!))
            }
            mSocket!!.close()
        } catch (e: IOException) {
            executor.shutdown()
            FirebaseLogger.logException(e, "SocketForegroundService")
            val mIntent = Intent(CURRENT_ACTION)
            if (retryCont < 3) {
                retryCont++
                mIntent.putExtra("error", "${e.message}")
                mIntent.putExtra("is_try_again", true)
                mIntent.putExtra("retryCont", retryCont)
                LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent)
            } else {
                mIntent.putExtra("error", "POS not Available")
                LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent)
                e.printStackTrace()
            }
        }
    }

    fun makeConnection(context: Context, jsonObj: JSONObject) {
        if (mSocket != null) {
            val output = PrintWriter(mSocket!!.getOutputStream(), true)
            val input = BufferedReader(InputStreamReader(mSocket!!.getInputStream()))

            // Send message to server

            Log.d(TAG, "Send message to Server: ${jsonObj.toString()}")
            output.println(jsonObj.toString())

            // Receive response from server
            val response = input.readLine()

            Log.d(TAG, "Get message from Server: ${response.toString()}")
            if (!response.isNullOrEmpty()) {
                val pref = Preferences()
                val db = DBHelper(this)
                Log.e("jflsjfs", "Responce ::: $response")
                val main = JSONObject(response)
                val gson = Gson()
                var sendMessage =  when (val value = main.get("pa")) {
                    is Int -> value
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
                if (main.has("pa")) {
                    val paValue = main.optString("pa")  // Get "pa" as a string
                    Log.e("SocketService", "Received pa value: $paValue")  // Debugging
                    sendMessage = when (paValue.toIntOrNull()) {
                        ERROR -> ERROR
                        WARRING -> WARRING
                        else -> paValue.toIntOrNull() ?: ERROR  // Default to ERROR if conversion fails
                    }
                }
                when (sendMessage) {
                    CONNECT -> {
                        var intent = Intent(ACTION_LOGIN)
                        jsonObj.put("aa", SHOW_PROGRESS)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                        val pv = main.getJSONObject("pv")
                        /*App Setting*/
                        pref.setInt(
                            this,
                            pv.getJSONObject("as").getInt("d_pt"),
                            "def_pt"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("as").getInt("ri"),
                            "resto_id"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("as").getInt("sid"),
                            "sid"
                        )

                        /*KOT Print*/
                        pref.setBool(this, true, "prn_setting")
                        pref.setBool(
                            this,
                            pv.getJSONObject("print")
                                .getBoolean("ck"),
                            "cb_kot"
                        )
                        pref.setBool(
                            this,
                            pv.getJSONObject("print")
                                .getBoolean("kps"),
                            "kot_print_submission"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("print")
                                .getInt("ktp"),
                            "kot_type_printer"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("print")
                                .getInt("kdt"),
                            "" +
                                    "kot_design_type"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("print")
                                .getInt("kdp"),
                            "kot_default_printer"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("print")
                                .getInt("kp"),
                            "kot_copies"
                        )
                        pref.setInt(
                            this,
                            pv.getJSONObject("print")
                                .getInt("kit_print"),
                            "kitchen_print"
                        )
                        pref.setStr(
                            this,
                            pv.getJSONObject("print")
                                .getString("kit_cat_print"),
                            "kit_cat_print"
                        )
                        if (pv.getJSONArray("resto_printers").length() > 0) {
                            val list = ArrayList<PrinterRespo>()
                            val restoPrinter = pv.getJSONArray("resto_printers")
                            for (i in 0 until restoPrinter.length()) {
                                val rp = restoPrinter.getString(i)
                                val array = rp.split(JOINT)
                                list.add(
                                    PrinterRespo(
                                        array[0].toInt(),//id
                                        array[1],//printer_name
                                        array[2].toInt(),//printer_connection_type_id
                                        array[3].toInt(),//printer_type
                                        array[4],//ip_add
                                        array[5],//port_add
                                        array[6],//printer_port
                                        "0"
                                    )
                                )
                            }
                            db.InsertPrinter(list)
                        }
                        if (pv.getJSONArray("tables").length() > 0) {
                            val list = ArrayList<GetTables>()
                            val tables = pv.getJSONArray("tables")
                            for (i in 0 until tables.length()) {
                                val rp = tables.getString(i)
                                val array = rp.split(JOINT)
                                list.add(
                                    GetTables(
                                        array[0].toInt(),//id
                                        array[2].toInt(),//tab_status
                                        array[1],//tab_label
                                        array[3].toInt()//tab_type
                                    )
                                )
                            }
                            db.InsertTable(list)
                        }
                        if (pv.getJSONArray("kit_cat").length() > 0) {
                            val list = ArrayList<kitCat>()
                            val kitCat = pv.getJSONArray("kit_cat")
                            for (i in 0 until kitCat.length()) {
                                val rp = kitCat.getString(i)
                                val array = rp.split(JOINT)
                                list.add(
                                    kitCat(
                                        array[0].toInt(),//id
                                        array[1]
                                    )
                                )
                            }
                            db.InsertKitCat(list)
                        }
                        if (pv.getJSONArray("menu").length() > 0) {
                            val list = gson.fromJson(
                                pv.getJSONArray("menu").toString(),
                                Array<MenuData>::class.java
                            )
                            for (i in list!!.indices) {
                                val selectedPriceTemplate = list[i].pt ?: 0
                                // Simulate your background task progress
                                // Update the progress in percentage
                                db.InsertMenuSyncCategory(list[i], selectedPriceTemplate)
                            }
                        }
                        intent = Intent(ACTION_LOGIN)
                        jsonObj.put("aa", CONNECT)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    KOT -> {
                        val intent = Intent(ACTION_KOT)
                        jsonObj.put("aa", KOT)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.toString())
                        Log.e("jflsfjs", "OBJECT :: $")
                        val pv = main.getJSONObject("pv").getJSONObject("tb")
                        updateTable(pv, db)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    GET_TABLE -> {
                        val pv = main.getJSONObject("pv")
                        updateTable(pv, db)
                        val intent = Intent(ACTION_TAB)
                        jsonObj.put("aa", GET_TABLE)
                        intent.putExtra("ua", jsonObj.toString())
                        Log.e("jflsfjs", "GET_TABLE :: ${pv.toString()}")
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    CLOSE_ORDER -> {
                        val pv = main.getJSONObject("pv")
                        updateTable(pv, db)
                        val intent = Intent(ACTION_ORDER)
                        jsonObj.put("aa", CLOSE_ORDER)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    GET_TABLE_ORDER -> {
                        val intent = Intent(ACTION_ORDER)
                        jsonObj.put("aa", GET_TABLE_ORDER)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.getJSONObject("pv").toString())
                        val pv = main.getJSONObject("pv").getJSONObject("tb")
                        Log.e("jflsfjs", "OBJECT 4 pv :: ${main.getJSONObject("pv").toString()}")
                        Log.e("jflsfjs", "OBJECT 4 aa :: $GET_TABLE_ORDER")
                        Log.e("jflsfjs", "OBJECT 4 ua :: $jsonObj.toString()")
                        updateTable(pv, db)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    ERROR -> {
                        val intent = Intent(CURRENT_ACTION)
                        intent.putExtra("error", main.getString("pv"))
                        intent.putExtra("is_try_again", false)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }
                    WARRING -> {
                        val intent = Intent(CURRENT_ACTION)
                        intent.putExtra("error", main.getString("pv"))
                        intent.putExtra("is_try_again", true)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    LOGOUT -> {
                        val intent = Intent(ACTION_LOGOUT)
                        jsonObj.put("aa", LOGOUT)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.toString())
                        val pv = main.getJSONObject("pv").getInt("status")
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    CUSTOMER_DETAILS -> {
                        val intent = Intent(ACTION_CUSTOMER)
                        jsonObj.put("aa", CUSTOMER_DETAILS)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.toString())
                        Log.e("jjladljja", "MAin Service $main")
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                }
            }
        } else {
            val serviceIntent = Intent(context, SocketForegroundService::class.java)
            serviceIntent.putExtra("json", jsonObj.toString())
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun updateTable(pv: JSONObject, db: DBHelper) {
        db.UpdateTable(1, "")
        if (!pv.getString("r").isNullOrEmpty()) {
            db.UpdateTable(2, pv.getString("r"))
        }
        if (!pv.getString("c").isNullOrEmpty()) {
            db.UpdateTable(3, pv.getString("c"))
        }
        if (!pv.getString("p").isNullOrEmpty()) {
            db.UpdateTable(5, pv.getString("p"))
        }
        if (!pv.getString("a").isNullOrEmpty()) {
            db.UpdateTable(4, pv.getString("a"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        executor.shutdown()
        try {
            mSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun createNotificationChannel() {
        Log.d(TAG, "Notification Channel Created")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Socket Service"
            val descriptionText = "Socket Foreground Service"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        Log.d(TAG, "Notification Created")
        val notificationIntent = Intent(this, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Captain Service is On")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_launcher_not)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        return notification
    }
}
