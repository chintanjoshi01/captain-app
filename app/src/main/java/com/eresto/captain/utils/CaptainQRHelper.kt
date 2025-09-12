
package com.eresto.utils

import android.content.Context
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.eresto.captain.utils.FirebaseLogger
import okhttp3.*
import java.lang.Exception

data class QRData(
    val ip: String,
    val port: String,
    val userId: String,
    val userName: String,
    val shortName: String,
    val ssid: String?,
    val password: String?
)

object CaptainQRHelper {

    fun parseQR(scanned: String): QRData {
        val parts = scanned.split("~")
        return QRData(
            ip = parts[0],
            port = parts[1],
            userId = parts[2],
            userName = parts[3],
            shortName = parts[4],
            ssid = if (parts.size >= 7) parts[5] else null,
            password = if (parts.size >= 7) parts[6] else null
        )
    }

    fun connectToWifi(context: Context, ssid: String, password: String, onConnected: () -> Unit) {
//        onConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.bindProcessToNetwork(network)
                    onConnected()
                }

                override fun onUnavailable() {
                    FirebaseLogger.logException(Exception("Failed to connect to Wi-Fi"), "CaptainQRHelper onUnavailable")
                    Toast.makeText(context, "Failed to connect to Wi-Fi", Toast.LENGTH_SHORT).show()
                }
            }

            try {
                connectivityManager.requestNetwork(request, callback)
            } catch (e: SecurityException) {
                Log.e("CaptainQRHelper", "Permission denied: ${e.message}")
                FirebaseLogger.logException(e, "CaptainQRHelper Connect WIFI")
                Toast.makeText(context, "App lacks network permission", Toast.LENGTH_LONG).show()
            }
        } else {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true
            val wifiConfig = WifiConfiguration().apply {
                SSID = "$ssid"
                preSharedKey = "$password"
            }

            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            Handler(Looper.getMainLooper()).postDelayed({
                onConnected()
            }, 5000)
        }
    }

    fun handleQRAndConnect(context: Context, qrString: String, onConnected: () -> Unit) {
        val data = parseQR(qrString)
        if (data.ssid != null && data.password != null) {
            connectToWifi(context, data.ssid, data.password, onConnected)
        } else {
            onConnected()
        }
    }

    fun connectToWebSocket(ip: String, port: String) {
        val url = "ws://$ip:$port"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        val wsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {

                Log.d("WebSocket", "Connected to POS")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed: ${t.message}")
            }
        }

        client.newWebSocket(request, wsListener)
    }
}
