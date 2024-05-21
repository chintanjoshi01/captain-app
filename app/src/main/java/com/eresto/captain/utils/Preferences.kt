package com.eresto.captain.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences {
    fun setBool(context: Context?, bool: Boolean, key: String?) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putBoolean(key, bool)
            editor.commit()
        } catch (e: Exception) {
        }
    }

    fun setStr(context: Context?, Str: String?, key: String?) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putString(key, Str)
            editor.commit()
        } catch (e: Exception) {
        }
    }

    fun setLng(context: Context?, Str: Long, key: String?) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putLong(key, Str)
            editor.commit()
        } catch (e: Exception) {
        }
    }

    fun setInt(context: Context?, ints: Int, key: String?) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putInt(key, ints)
            editor.commit()
        } catch (e: Exception) {
        }
    }

    fun getStr(context: Context?, key: String?): String {
        return try {
            val preferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getString(key, null) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getLng(context: Context?, key: String?): Long {
        return try {
            val preferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getLong(key, 0L)
        } catch (e: Exception) {
            0L
        }
    }

    fun getBool(context: Context?, key: String?): Boolean {
        return try {
            val preferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getBoolean(key, false)
        } catch (e: Exception) {
            false
        }
    }

    fun getInt(context: Context?, key: String?): Int {
        return try {
            val preferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getInt(key, 0)
        } catch (e: Exception) {
            0
        }
    }

    fun clearSharedPreference(context: Context?) {
        try {
            val preferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            val editor: SharedPreferences.Editor = preferences.edit()
            editor.clear()
            editor.commit()

        } catch (e: Exception) {

        }
    }
}