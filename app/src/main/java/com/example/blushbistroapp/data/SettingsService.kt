package com.example.blushbistroapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsService private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getThemePreference(): Boolean {
        return sharedPreferences.getBoolean(KEY_THEME_PREFERENCE, false)
    }

    fun updateThemePreference(isDarkMode: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_THEME_PREFERENCE, isDarkMode)
        }
    }

    fun getNotificationPreference(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_PREFERENCE, true)
    }

    fun updateNotificationPreference(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_NOTIFICATION_PREFERENCE, isEnabled)
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "BlushBistroSettings"
        private const val KEY_THEME_PREFERENCE = "theme_preference"
        private const val KEY_NOTIFICATION_PREFERENCE = "notification_preference"

        @Volatile
        private var instance: SettingsService? = null

        fun getInstance(context: Context): SettingsService {
            return instance ?: synchronized(this) {
                instance ?: SettingsService(context.applicationContext).also { instance = it }
            }
        }
    }
} 