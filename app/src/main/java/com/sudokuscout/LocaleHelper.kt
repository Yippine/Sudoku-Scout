package com.sudokuscout

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.preference.PreferenceManager
import java.util.*

object LocaleHelper {
    
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_TRADITIONAL_CHINESE = "zh_TW"
    const val LANGUAGE_SIMPLIFIED_CHINESE = "zh_CN"
    
    private const val PREF_LANGUAGE = "selected_language"
    
    fun setLocale(context: Context, language: String): Context {
        saveLanguage(context, language)
        return updateResources(context, language)
    }
    
    fun getLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }
    
    private fun saveLanguage(context: Context, language: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_LANGUAGE, language).apply()
    }
    
    private fun updateResources(context: Context, language: String): Context {
        val locale = getLocaleFromLanguageCode(language)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    private fun getLocaleFromLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            LANGUAGE_TRADITIONAL_CHINESE -> Locale("zh", "TW")
            LANGUAGE_SIMPLIFIED_CHINESE -> Locale("zh", "CN")
            LANGUAGE_ENGLISH -> Locale("en")
            else -> Locale("en")
        }
    }
    
    fun getLanguageDisplayName(context: Context, languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_ENGLISH -> context.getString(R.string.language_english)
            LANGUAGE_TRADITIONAL_CHINESE -> context.getString(R.string.language_traditional_chinese)
            LANGUAGE_SIMPLIFIED_CHINESE -> context.getString(R.string.language_simplified_chinese)
            else -> context.getString(R.string.language_english)
        }
    }
    
    fun attachBaseContext(context: Context): Context {
        return try {
            val language = getLanguage(context)
            updateResources(context, language)
        } catch (e: Exception) {
            // If there's an error reading preferences, fall back to default
            context
        }
    }
    
    fun restartActivity(activity: Activity) {
        activity.recreate()
    }
    
    fun getAllLanguageCodes(): List<String> {
        return listOf(
            LANGUAGE_ENGLISH,
            LANGUAGE_TRADITIONAL_CHINESE,
            LANGUAGE_SIMPLIFIED_CHINESE
        )
    }
}