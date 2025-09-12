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
import androidx.core.app.NotificationCompat
import com.eresto.captain.R
import com.eresto.captain.data.SocketManager
import com.eresto.captain.ui.LoginActivity

class SocketForegroundService : Service() {

    private val CHANNEL_ID = "SocketForegroundService"
    private val TAG = "SocketServiceWrapper"

    // Keep these constants here as they are part of the Service's public Intent API
    companion object {
        const val ACTION_TAB = "com.eresto.captain.action.TAB"
        const val ACTION_KOT = "com.eresto.captain.action.KOT"
        const val ACTION_ORDER = "com.eresto.captain.action.ORDER"
        const val ACTION_LOGIN = "com.eresto.captain.action.LOGIN"
        const val ACTION_LOGOUT = "com.eresto.captain.action.LOGOUT"
        const val ACTION_CUSTOMER = "com.eresto.captain.action.CUSTOMER"
        var CURRENT_ACTION = ""
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Start Command Received")

        // Start the service in the foreground
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        // Get the SocketManager instance
        val socketManager = SocketManager.getInstance(this)

        // Delegate the message sending to the SocketManager
        val jsonMessage = intent?.getStringExtra("json")
        if (jsonMessage != null) {
            socketManager.sendMessage(jsonMessage)
        } else {
            // If service is started without a message, ensure connection is active
            socketManager.connect()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroyed")
        // Inform the SocketManager to disconnect when the service is killed
        SocketManager.getInstance(this).disconnect("Service is being destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Notification logic remains unchanged ---
    private fun createNotificationChannel() {
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
        val notificationIntent = Intent(this, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Captain Service is On")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_launcher_not)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }
}