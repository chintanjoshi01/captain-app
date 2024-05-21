package com.eresto.captain.ui.tables


import com.eresto.captain.base.BaseActivity
import com.eresto.captain.base.BaseActivity.Companion.GET_TABLE
import com.eresto.captain.utils.FirebaseLogger
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.SocketForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class TabRepository(private val baseActivity: BaseActivity) {
    private var fetchJob: Job? = null // Store the coroutine reference

    fun startFetchingTables(pref: Preferences, callback: (String) -> Unit) {
        fetchJob?.cancel() // Cancel existing job if running
        fetchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                baseActivity.setCallBack(object : BaseActivity.OnResponseFromServerPOS {
                    override fun onResponse(json: String) {
                        callback(json) // Send data to ViewModel
                    }
                })
                while (isActive) {
                    val jsonObj = JSONObject().apply {
                        put("ca", GET_TABLE)
                        put("cv", JSONObject().apply {
                            put("sid", pref.getInt(baseActivity.activity, "sid"))
                        })
                    }
                    withContext(Dispatchers.Main) {
                        baseActivity.sendMessageToServer(
                            jsonObj,
                            SocketForegroundService.ACTION_TAB
                        )
                    }
                    delay(2000)
                }
            } catch (e: CancellationException) {
                FirebaseLogger.logException(e, "TabRepository")
            } catch (e: Exception) {
                FirebaseLogger.logException(e, "TabRepository")
                e.printStackTrace()
            }
        }
    }

    fun stopFetchingTables() {
        fetchJob?.cancel()
    }
}
