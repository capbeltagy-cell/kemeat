package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    // Lazy initialize AppDatabase and Repository
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GameRepository(database.gameDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Safe initialization of Google Mobile Ads SDK
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Safe initialization of Firebase components
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
