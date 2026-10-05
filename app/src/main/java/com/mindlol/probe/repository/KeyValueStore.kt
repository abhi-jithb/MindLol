package com.mindlol.probe.repository

import android.content.Context

/**
 * Key-Value storage abstraction interface for Android SharedPreferences and JVM test isolation.
 */
interface KeyValueStore {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String?)
}

/**
 * Android SharedPreferences implementation for production persistence.
 */
class SharedPreferencesStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("mindlol_counter_prefs", Context.MODE_PRIVATE)

    override fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }
}

/**
 * In-memory storage implementation for fast, isolated JVM unit testing.
 */
class InMemoryKeyValueStore : KeyValueStore {
    private val map = HashMap<String, Any?>()

    override fun getInt(key: String, defaultValue: Int): Int {
        return (map[key] as? Int) ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        map[key] = value
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return (map[key] as? String) ?: defaultValue
    }

    override fun putString(key: String, value: String?) {
        map[key] = value
    }
}
