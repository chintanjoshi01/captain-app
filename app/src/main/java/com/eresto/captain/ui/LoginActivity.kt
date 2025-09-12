package com.eresto.captain.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.eresto.captain.base.BaseActivity
import com.eresto.captain.databinding.ActivityLoginBinding
import com.eresto.captain.databinding.DialogManualLoginBinding
import com.eresto.captain.utils.KeyUtils
import com.eresto.captain.utils.SocketForegroundService
import com.eresto.captain.utils.Utils
import com.eresto.utils.CaptainQRHelper
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LoginActivity : BaseActivity() {
    val CAMERA_PERMISSION_REQUEST_CODE = 120
    val FOREGROUND_PERMISSION_REQUEST_CODE = 130
    private var binding: ActivityLoginBinding? = null


    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val scanResult = data?.getStringExtra("SCAN_RESULT")
            Log.d("LoginFragment", "Raw scan result: $scanResult")

            if (scanResult.isNullOrEmpty()) {
                showInvalidQRError("Scan result was empty.")
                return@registerForActivityResult
            }

            // --- STEP 2: Validate the QR code before processing ---
            try {
                val parts = scanResult.split("~")

                // --- STEP 3: Perform the validation checks ---
                if (parts.size == EXPECTED_QR_PARTS) {
                    // --- Validation Successful! Proceed with original logic ---
                    Log.d("LoginFragment", "QR Code is valid. Processing...")

                    val ipAddress = parts[0]
                    val port = parts[1].toInt() // The try-catch handles NumberFormatException
                    val userId = parts[2].toInt()
                    val name = parts[3]
                    val shortName = parts[4]

                    // Save the data to preferences
                    pref!!.setStr(this@LoginActivity, ipAddress, "ip_address")
                    pref!!.setInt(this@LoginActivity, port, "port")
                    pref!!.setInt(this@LoginActivity, userId, "user_id")
                    pref!!.setStr(this@LoginActivity, name, KeyUtils.name)
                    pref!!.setStr(this@LoginActivity, shortName, KeyUtils.shortName)
                    pref!!.setStr(this@LoginActivity, scanResult, "login_data")

                    // Connect to the server
                    CaptainQRHelper.handleQRAndConnect(this@LoginActivity, scanResult) {
                        val jsonObj = JSONObject()
                        jsonObj.put("ca", CONNECT)
                        jsonObj.put("ui", userId)
                        jsonObj.put("imei", Utils.getIMEI(this))
                        jsonObj.put("di", "${Build.MODEL}")
                        jsonObj.put("pid", 1)
                        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGIN)
                    }
                } else {
                    // --- Validation Failed: Wrong format or tag ---
                    Log.e(
                        "LoginFragment",
                        "QR Validation Failed. Parts count: ${parts.size}, Tag: ${parts.getOrNull(5)}"
                    )
                    throw IllegalArgumentException("Invalid QR code format.")
                }
            } catch (e: Exception) {
                // --- STEP 4: Catch any errors (wrong parts, not a number, etc.) and show an error ---
                Log.e("LoginFragment", "Error processing QR code", e)
                showInvalidQRError("Incompatible or invalid QR code scanned. Please use a valid eResto Captain QR code.")
            }
        } else {
            Log.d("LoginFragment", "Scan was cancelled by user.")
        }
    }

    /**
     * Helper function to show a consistent error message for invalid QR codes.
     */
    private fun showInvalidQRError(message: String) {
        // Using the dialog from your BaseActivity for a better UX than a Toast
        displayActionSnackbarBottom(
            this,
            message,
            2, // Type 2 is the red error icon
            false, // Don't show a cancel button
            "OK", // Button text
            object : OnDialogClick {
                override fun onOk() {

                }

                override fun onCancel() {

                }
            }
        )
    }


    override fun onResume() {
        super.onResume()
        reCreateSocket()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        /* if (pref!!.getBool(this, "is_login")) {
             val intent = Intent(this, TabActivity::class.java)
             startActivity(intent)
             finish()
         }*/
        binding!!.qrLoginButton.setOnClickListener {
            val intent = Intent(this, ProfessionalScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }
        binding!!.manualLoginButton.setOnClickListener {
            showManualLoginDialog()
        }
//        binding!!.tvStep3.text = getStyledText(this , resources.getString(R.string.step_3), R.color.blackText2)
    }

    // --- STEP 1: Define the validation tag and expected parts count ---
    companion object {
        private const val QR_VALIDATION_TAG = "ErestoCaptainApp"
        private const val EXPECTED_QR_PARTS = 7
    }


    /**
     * Shows the custom dialog for manual login.
     */
    private fun showManualLoginDialog() {
        val dialogBinding = DialogManualLoginBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        // Make the dialog background transparent and set its width
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Close button inside the dialog
        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Login button inside the dialog
        dialogBinding.loginButton.setOnClickListener {
            val ipAddress = dialogBinding.ipAddressEditText.text.toString().trim()
            val portNumber = dialogBinding.portNumberEditText.text.toString().trim()
            val userId = dialogBinding.userIdEditText.text.toString().trim()
            // WiFi SSID is collected but not used in the login data string in your original code
            // val wifiSsid = dialogBinding.wifiSsidEditText.text.toString().trim()

            if (ipAddress.isNotEmpty() && portNumber.isNotEmpty() && userId.isNotEmpty()) {
                // Construct a data string similar to the one from the QR code.
                // We leave name and shortName empty as they are not provided in manual login.
                val loginData = "$ipAddress~$portNumber~$userId~~"
                handleLogin(loginData)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun handleLogin(data: String) {
        try {
            val parts = data.split("~")
            pref!!.setStr(this@LoginActivity, parts[0], "ip_address")
            pref!!.setInt(this@LoginActivity, parts[1].toInt(), "port")
            pref!!.setInt(this@LoginActivity, parts[2].toInt(), "user_id")
            pref!!.setStr(this@LoginActivity, parts.getOrNull(3) ?: "", KeyUtils.name)
            pref!!.setStr(this@LoginActivity, parts.getOrNull(4) ?: "", KeyUtils.shortName)
            pref!!.setStr(this@LoginActivity, data, "login_data")

            CaptainQRHelper.handleQRAndConnect(this@LoginActivity, data) {
                val jsonObj = JSONObject()
                jsonObj.put("ca", CONNECT) //action
                jsonObj.put("ui", pref!!.getInt(this@LoginActivity, "user_id")) //user_id
                jsonObj.put("imei", Utils.getIMEI(this)) //imei
                jsonObj.put("di", "${Build.MODEL}") //device_info
                jsonObj.put("pid", 1)
                sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGIN)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid login data.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
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
                    pref!!.setStr(this@LoginActivity, data, "login_data")
                    CaptainQRHelper.handleQRAndConnect(this@LoginActivity, data) {
                        val jsonObj = JSONObject()
                        jsonObj.put("ca", CONNECT)//action
                        jsonObj.put("ui", pref!!.getInt(this@LoginActivity, "user_id"))//user_id
                        jsonObj.put("imei", Utils.getIMEI(this))//imei
                        jsonObj.put("di", "${Build.MODEL}")//device_info
                        jsonObj.put("pid", 1)
                        sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGIN)
                    }

                    /* val jsonObj = JSONObject()
                     jsonObj.put("ca", CONNECT)//action
                     jsonObj.put("ui", pref!!.getInt(this@LoginActivity, "user_id"))//user_id
                     jsonObj.put("imei", Utils.getIMEI(this))//imei
                     jsonObj.put("di", "${Build.MODEL}")//device_info
                     sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGIN)*/
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


    private fun getStyledText(
        context: Context,
        text: String,
        colorResId: Int,
        bracketFontSizeSp: Int = com.intuit.ssp.R.dimen._12ssp // Font size for text inside ()
    ): SpannableString {

        if (text.contains("(") && text.contains(")")) {
            val startIndex = text.indexOf("(")
            val endIndex = text.indexOf(")") + 1
            val spannable = SpannableString(text)

            val color = ContextCompat.getColor(context, colorResId)

            // Apply text color to the text inside ()
            spannable.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply font size only for text inside ()
            spannable.setSpan(
                AbsoluteSizeSpan(bracketFontSizeSp, true), // 'true' makes it SP-based
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return spannable
        }

        return SpannableString(text)
    }


    private fun reCreateSocket() {
        val currentTime = System.currentTimeMillis()
        val pauseTime = pref!!.getLng(this, "pause_time")
        if (pauseTime > 0) {
            val difference = currentTime - pauseTime
            val minutesPassed = TimeUnit.MILLISECONDS.toMinutes(difference)
            // Check against the 30-minute constant
            if (minutesPassed >= SESSION_TIMEOUT_MINUTES && pref!!.getBool(this, "is_login")) {
                displayActionSnackbarBottom(
                    this,
                    "Your session has expired. Please log in again.",
                    3, // Warning type
                    false,
                    "Login",
                    object : OnDialogClick {
                        override fun onOk() {
                            // Perform a full logout and redirect
                            pref!!.clearSharedPreference(this@LoginActivity)
                            db!!.DeleteDB()
                            val intent = Intent(this@LoginActivity, LoginActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }

                        override fun onCancel() {}
                    })
            } else if (pref!!.getBool(this, "is_login")) {
                val loginData = pref!!.getStr(this, "login_data")
                val userId = pref!!.getInt(this, "user_id")
                CaptainQRHelper.handleQRAndConnect(this@LoginActivity, loginData) {
                    val jsonObj = JSONObject()
                    jsonObj.put("ca", CONNECT)
                    jsonObj.put("ui", userId)
                    jsonObj.put("imei", Utils.getIMEI(this))
                    jsonObj.put("di", "${Build.MODEL}")
                    jsonObj.put("pid", 1)
                    sendMessageToServer(jsonObj, SocketForegroundService.ACTION_LOGIN)
                }
            }
        }
    }

}
