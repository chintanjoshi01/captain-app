package com.eresto.captain.ui.tables

import com.eresto.captain.base.BaseActivity
import com.eresto.captain.utils.Preferences
import com.eresto.captain.utils.SocketForegroundService
import org.json.JSONObject

/**
 * The repository is now simplified. Its only role is to construct and send
 * a single network request to get the current table status.
 * It no longer manages any loops or coroutine jobs.
 */
class TabRepository(private val baseActivity: BaseActivity) {

    fun requestTableUpdate(pref: Preferences) {
        val jsonObj = JSONObject().apply {
            put("ca", BaseActivity.GET_TABLE)
            put("cv", JSONObject().apply {
                put("sid", pref.getInt(baseActivity, "sid"))
            })
        }
        // Send the one-time request. The response will be handled by the
        // BroadcastReceiver in BaseActivity.
        baseActivity.sendMessageToServer(
            jsonObj,
            SocketForegroundService.ACTION_TAB
        )
    }
}