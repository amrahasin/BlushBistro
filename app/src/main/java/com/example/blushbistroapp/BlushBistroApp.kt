package com.example.blushbistroapp

import android.app.Application
import com.google.firebase.FirebaseApp

class BlushBistroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
} 