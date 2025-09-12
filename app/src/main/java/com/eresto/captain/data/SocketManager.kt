package com.eresto.captain.data


import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.model.GetTables
import com.eresto.captain.model.MenuData
import com.eresto.captain.model.kitCat
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.FirebaseLogger
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.SocketForegroundService
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// Enum to represent the connection state in a structured way.
sealed class ConnectionState {
    object Connected : ConnectionState()
    data class Disconnected(val reason: String?) : ConnectionState()
    object Connecting : ConnectionState()
}

class SocketManager private constructor(private val context: Context) {

    private var webSocket: WebSocket? = null
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingMessage: String? = null // To hold a message that needs to be sent upon connection
    private var retryCount = 0
    /**
     * Returns the current connection state.
     * @return true if the webSocket is not null (i.e., connected), false otherwise.
     */
    fun isConnected(): Boolean = webSocket != null

    // Using OkHttp's built-in ping to keep the connection alive.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    // --- Start of Public API for the Service to use ---

    /**
     * Attempts to connect to the WebSocket server.
     * This is the primary entry point for establishing a connection.
     */
    fun connect() {
        // Prevent multiple connection attempts simultaneously
        if (webSocket != null || client.dispatcher.queuedCallsCount() > 0) {
            Log.d(TAG, "Connection attempt ignored: Already connected or connecting.")
            return
        }

        val pref = Preferences()
        val serverIp = pref.getStr(context, "ip_address")
        val serverPort = pref.getInt(context, "port")
        val serverPath = "/ws" // As in your original code

        if (serverIp.isNullOrEmpty() || serverPort == 0) {
            handleConnectionError(IOException("IP Address or Port not configured."))
            return
        }

        val url = "ws://$serverIp:$serverPort$serverPath"
        val request = Request.Builder().url(url).build()
        Log.d(TAG, "Attempting to connect to: $url")
        client.newWebSocket(request, SocketListener())
    }

    /**
     * Sends a message through the WebSocket. If not connected, it will trigger a connection
     * attempt and queue the message to be sent once connected.
     */
    fun sendMessage(jsonMessage: String) {
        managerScope.launch {
            if (webSocket != null) {
                Log.d(TAG, "WebSocket is connected. Sending message: $jsonMessage")
                webSocket?.send(jsonMessage)
            } else {
                Log.d(TAG, "WebSocket not connected. Queuing message and starting connection.")
                pendingMessage = jsonMessage
                connect()
            }
        }
    }

    /**
     * Gracefully disconnects the WebSocket.
     */
    fun disconnect(reason: String = "Client initiated disconnect") {
        Log.d(TAG, "Disconnecting... Reason: $reason")
        webSocket?.close(1000, reason)
        webSocket = null
    }

    // --- End of Public API ---

    private fun handleConnectionError(e: Throwable) {
        FirebaseLogger.logException(Exception(e), "SocketManager")
        val mIntent = Intent(SocketForegroundService.CURRENT_ACTION)
        if (retryCount < 3) {
            retryCount++
            mIntent.putExtra("error", "${e.message}")
            mIntent.putExtra("is_try_again", true)
            mIntent.putExtra("retryCont", retryCount)
        } else {
            mIntent.putExtra("error", "POS not Available")
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(mIntent)
        e.printStackTrace()
    }

    // This is your original processing logic, moved here without changes.
    private fun processServerMessage(response: String) {
        try {
            if (response.isNotEmpty()) {
                val pref = Preferences()
                val db = DBHelper(context)
                Log.e("jflsjfs", "Response ::: $response")
                val main = JSONObject(response)
                val gson = Gson()
                val jsonObj = JSONObject()

                var sendMessage = when (val value = main.get("pa")) {
                    is Int -> value
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
                if (main.has("pa")) {
                    val paValue = main.optString("pa")
                    Log.e("SocketService", "Received pa value: $paValue")
                    sendMessage = when (paValue.toIntOrNull()) {
                        BaseActivity.ERROR -> BaseActivity.ERROR
                        BaseActivity.WARRING -> BaseActivity.WARRING
                        else -> paValue.toIntOrNull() ?: BaseActivity.ERROR
                    }
                }
                when (sendMessage) {
                    BaseActivity.CONNECT -> {
                        var intent = Intent(SocketForegroundService.ACTION_LOGIN)
                        jsonObj.put("aa", BaseActivity.SHOW_PROGRESS)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        val pv = main.getJSONObject("pv")
                        val fl = pv.optInt("fl", -1)
                        pref.setInt(context, pv.getJSONObject("as").getInt("ri"), "resto_id")
                        pref.setStr(context, pv.getJSONObject("as").getString("rn"), "resto_name")
                        pref.setInt(context, pv.getJSONObject("as").getInt("sid"), "sid")
                        if (fl == 0) {
                            intent = Intent(SocketForegroundService.ACTION_LOGIN)
                            jsonObj.put("aa", BaseActivity.CONNECT)
                            intent.putExtra("ua", jsonObj.toString())
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        } else {
                            if (pv.getJSONArray("tables").length() > 0) {
                                val list = ArrayList<GetTables>()
                                val tables = pv.getJSONArray("tables")
                                for (i in 0 until tables.length()) {
                                    val rp = tables.getString(i)
                                    val array = rp.split(JOINT)
                                    list.add(GetTables(array[0].toInt(), array[2].toInt(), array[1], array[3].toInt(), array[4].toInt()))
                                    Log.e("fklfj", "Order type ::: ${array[4]}")
                                }
                                db.InsertTable(list)
                            }
                            if (pv.getJSONArray("kit_cat").length() > 0) {
                                val list = ArrayList<kitCat>()
                                val kitCat = pv.getJSONArray("kit_cat")
                                for (i in 0 until kitCat.length()) {
                                    val rp = kitCat.getString(i)
                                    val array = rp.split(JOINT)
                                    list.add(kitCat(array[0].toInt(), array[1]))
                                }
                                db.InsertKitCat(list)
                            }
                            if (pv.getJSONArray("menu").length() > 0) {
                                db.deleteAllSyncItems()
                                db.deleteAllMenuSyncCategory()
                                val list = gson.fromJson(pv.getJSONArray("menu").toString(), Array<MenuData>::class.java)
                                for (i in list.indices) {
                                    val selectedPriceTemplate = list[i].pt ?: 0
                                    db.InsertMenuSyncCategory(list[i], selectedPriceTemplate)
                                }
                            }
                            intent = Intent(SocketForegroundService.ACTION_LOGIN)
                            jsonObj.put("aa", BaseActivity.CONNECT)
                            intent.putExtra("ua", jsonObj.toString())
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        }

                    }
                    BaseActivity.KOT -> {
                        val intent = Intent(SocketForegroundService.ACTION_KOT)
                        jsonObj.put("aa", BaseActivity.KOT)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.EDIT_KOT -> {
                        val intent = Intent(SocketForegroundService.ACTION_KOT)
                        jsonObj.put("aa", BaseActivity.KOT)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.GET_TABLE -> {
                        val pv = main.getJSONObject("pv")
                        updateTable(pv, db)
                        val intent = Intent(SocketForegroundService.ACTION_TAB)
                        jsonObj.put("aa", BaseActivity.GET_TABLE)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.CLOSE_ORDER -> {
                        val intent = Intent(SocketForegroundService.ACTION_ORDER)
                        jsonObj.put("aa", BaseActivity.CLOSE_ORDER)
                        intent.putExtra("ua", jsonObj.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.GET_TABLE_ORDER -> {
                        val intent = Intent(SocketForegroundService.ACTION_ORDER)
                        jsonObj.put("aa", BaseActivity.GET_TABLE_ORDER)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.getJSONObject("pv").toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.ERROR -> {
                        val intent = Intent(SocketForegroundService.CURRENT_ACTION)
                        intent.putExtra("error", main.getString("pv"))
                        intent.putExtra("is_try_again", false)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.WARRING -> {
                        val intent = Intent(SocketForegroundService.CURRENT_ACTION)
                        intent.putExtra("error", main.getString("pv"))
                        intent.putExtra("is_try_again", true)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.TABLE_CLOSED -> {
                        val intent = Intent(SocketForegroundService.CURRENT_ACTION)
                        intent.putExtra("error", main.getString("pv"))
                        intent.putExtra("isShowCancel", false)
                        intent.putExtra("isTableClosed", true)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.LOGOUT -> {
                        val intent = Intent(SocketForegroundService.ACTION_LOGOUT)
                        jsonObj.put("aa", BaseActivity.LOGOUT)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.toString())
                        val pv = main.getJSONObject("pv").getInt("status")
                        if (pv == 1) {
                            pref.setBool(context, false, "is_login")
                        }
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                    BaseActivity.CUSTOMER_DETAILS -> {
                        val intent = Intent(SocketForegroundService.ACTION_CUSTOMER)
                        jsonObj.put("aa", BaseActivity.CUSTOMER_DETAILS)
                        intent.putExtra("ua", jsonObj.toString())
                        intent.putExtra("pv", main.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                }
            }
        } catch (e: JSONException) {
            FirebaseLogger.logException(e, "processServerMessage")
            e.printStackTrace()
        }
    }

    // Also moved from the original service
    private fun updateTable(pv: JSONObject, db: DBHelper) {
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

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            super.onOpen(ws, response)
            Log.d(TAG, "WebSocket connection opened.")
            webSocket = ws
            retryCount = 0

            // If there was a message waiting to be sent, send it now.
            pendingMessage?.let {
                Log.d(TAG, "Sending queued message: $it")
                ws.send(it)
                pendingMessage = null
            }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            super.onMessage(ws, text)
            Log.d(TAG, "Received message: $text")
            processServerMessage(text)
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            super.onClosing(ws, code, reason)
            Log.d(TAG, "WebSocket closing: $code / $reason")
            webSocket = null
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(ws, t, response)
            Log.e(TAG, "WebSocket failure", t)
            webSocket = null
            handleConnectionError(Throwable("Connection Failed\n" +
                    "Please ensure that the Eresto Edge Machine and this Captain Mobile are connected to the same Wi-Fi network."))
        }
    }

    companion object {
        private const val TAG = "SocketManager"
        private const val JOINT = "~"

        @Volatile private var INSTANCE: SocketManager? = null

        fun getInstance(context: Context): SocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocketManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}