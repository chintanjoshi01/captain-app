package com.eresto.captain.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityLoginBinding
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.SocketForegroundService
import com.eresto.captain.utils.Utils
import org.json.JSONObject

class LoginActivity : BaseActivity() {
    val CAMERA_PERMISSION_REQUEST_CODE = 120
    val FOREGROUND_PERMISSION_REQUEST_CODE = 130
    private var binding: ActivityLoginBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        if (pref!!.getBool(this, "is_login")) {
            val intent = Intent(this, TabActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding!!.connectButton.setOnClickListener {
            if (checkPermissions("check")) {
                // Permissions are granted, start your foreground service
                val intent = Intent(this@LoginActivity, ScannerActivity::class.java)
                resultLauncher.launch(intent)
            } else {
                // Permissions are not granted, request them
                checkPermissions("req")
            }
        }
    }

    private fun checkPermissions(flag: String): Boolean {
        val permissionsToCheck = ArrayList<String>()

        permissionsToCheck.add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToCheck.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        if (flag == "check") {
            return permissionsToCheck.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            val fot = ArrayList<String>()
            permissionsToCheck.all {
                if (ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    fot.add(it)
                }
                false
            }
            ActivityCompat.requestPermissions(
                this,
                fot.toTypedArray(),
                FOREGROUND_PERMISSION_REQUEST_CODE
            )
            return false
        }
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) { // There are no request codes
                val data = result.data?.getStringExtra("data")
                if (data != null) {
                    pref!!.setStr(this@LoginActivity, data.split("~")[0], "ip_address")
                    pref!!.setInt(this@LoginActivity, data.split("~")[1].toInt(), "port")
                    pref!!.setInt(this@LoginActivity, data.split("~")[2].toInt(), "user_id")
                    pref!!.setStr(this@LoginActivity, data.split("~")[3], KeyUtils.name)
                    pref!!.setStr(this@LoginActivity, data.split("~")[4], KeyUtils.shortName)

                    val jsonObj = JSONObject()
                    jsonObj.put("ca", CONNECT)//action
                    jsonObj.put("ui", pref!!.getInt(this@LoginActivity, "user_id"))//user_id
                    jsonObj.put("imei", Utils.getIMEI(this))//imei
                    jsonObj.put("di", "${Build.MODEL}")//device_info
                    sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGIN)
                }

            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == FOREGROUND_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, start your foreground service here
                val intent = Intent(this@LoginActivity, ScannerActivity::class.java)
                resultLauncher.launch(intent)
            } else {
                // Permissions not granted, handle accordingly
                // You may want to inform the user and/or request permissions again
            }
        }
    }
}
