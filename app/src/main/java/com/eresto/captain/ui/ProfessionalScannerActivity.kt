package com.eresto.captain.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.eresto.captain.databinding.ActivityProfessionalScannerBinding
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager

class ProfessionalScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfessionalScannerBinding
    private lateinit var capture: CaptureManager
    private lateinit var beepManager: BeepManager // For the beep sound

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfessionalScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the beep manager
        beepManager = BeepManager(this)

        // Hide default ZXing UI elements
        binding.barcodeScanner.viewFinder.visibility = View.GONE
        binding.barcodeScanner.statusView.visibility = View.GONE

        capture = CaptureManager(this, binding.barcodeScanner)
        capture.initializeFromIntent(intent, savedInstanceState)

        // Use the continuous callback for live targeting and final result
        binding.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    // Prevent multiple scans
                    binding.barcodeScanner.pause()
                    handleSuccessfulScan(it.text)
                }
            }

            // This is called constantly with potential QR code locations
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                // Pass the points to our custom view to draw the brand-colored dots
                binding.viewfinder.setResultPoints(resultPoints)
            }
        })

        binding.closeButton.setOnClickListener { finish() }
        checkCameraPermission()
    }

    private fun handleSuccessfulScan(qrText: String) {
        // 1. Play beep and vibrate
        beepManager.playBeepSoundAndVibrate()

        // 2. Trigger our new custom success animation
        binding.viewfinder.triggerSuccessAndCollapseAnimation()

        // 3. Delay finishing the activity to allow the animation to complete
        binding.root.postDelayed({
            val resultIntent = Intent().putExtra("SCAN_RESULT", qrText)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }, 800) // Increased delay for the new animation
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // --- Standard Lifecycle Forwarding ---
    override fun onResume() {
        super.onResume(); capture.onResume()
    }

    override fun onPause() {
        super.onPause(); capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy(); capture.onDestroy()
    }
}