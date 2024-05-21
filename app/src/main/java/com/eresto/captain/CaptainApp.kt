package com.eresto.captain

import android.app.Application
import com.google.firebase.FirebaseApp

class CaptainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}