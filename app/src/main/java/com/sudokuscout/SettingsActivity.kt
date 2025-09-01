package com.sudokuscout

import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var validationRadioGroup: RadioGroup
    private lateinit var defaultDifficultyRadioGroup: RadioGroup
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var switchAutoNotesOnStart: SwitchMaterial
    private lateinit var switchHighlightSameNumbers: SwitchMaterial
    
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        initializeViews()
        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        validationRadioGroup = findViewById(R.id.validationRadioGroup)
        defaultDifficultyRadioGroup = findViewById(R.id.defaultDifficultyRadioGroup)
        languageRadioGroup = findViewById(R.id.languageRadioGroup)
        switchAutoNotesOnStart = findViewById(R.id.switchAutoNotesOnStart)
        switchHighlightSameNumbers = findViewById(R.id.switchHighlightSameNumbers)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadSettings() {
        // Load validation setting
        val validateImmediately = sharedPreferences.getBoolean("validate_immediately", true)
        if (validateImmediately) {
            validationRadioGroup.check(R.id.radioValidateImmediately)
        } else {
            validationRadioGroup.check(R.id.radioValidateAtEnd)
        }
        
        // Load default difficulty
        val defaultDifficulty = sharedPreferences.getString("default_difficulty", "MEDIUM")
        val difficultyRadioId = when (defaultDifficulty) {
            "EASY" -> R.id.radioEasy
            "MEDIUM" -> R.id.radioMedium
            "HARD" -> R.id.radioHard
            "EXPERT" -> R.id.radioExpert
            "EVIL" -> R.id.radioEvil
            else -> R.id.radioMedium
        }
        defaultDifficultyRadioGroup.check(difficultyRadioId)
        
        // Load language setting
        val currentLanguage = LocaleHelper.getLanguage(this)
        val languageRadioId = when (currentLanguage) {
            LocaleHelper.LANGUAGE_ENGLISH -> R.id.radioEnglish
            LocaleHelper.LANGUAGE_TRADITIONAL_CHINESE -> R.id.radioTraditionalChinese
            LocaleHelper.LANGUAGE_SIMPLIFIED_CHINESE -> R.id.radioSimplifiedChinese
            else -> R.id.radioEnglish
        }
        languageRadioGroup.check(languageRadioId)
        
        // Load other settings
        switchAutoNotesOnStart.isChecked = sharedPreferences.getBoolean("auto_notes_on_start", false)
        switchHighlightSameNumbers.isChecked = sharedPreferences.getBoolean("highlight_same_numbers", true)
    }

    private fun setupListeners() {
        // Validation setting listener
        validationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val validateImmediately = checkedId == R.id.radioValidateImmediately
            sharedPreferences.edit()
                .putBoolean("validate_immediately", validateImmediately)
                .apply()
        }
        
        // Default difficulty listener
        defaultDifficultyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val difficulty = when (checkedId) {
                R.id.radioEasy -> "EASY"
                R.id.radioMedium -> "MEDIUM"
                R.id.radioHard -> "HARD"
                R.id.radioExpert -> "EXPERT"
                R.id.radioEvil -> "EVIL"
                else -> "MEDIUM"
            }
            sharedPreferences.edit()
                .putString("default_difficulty", difficulty)
                .apply()
        }
        
        // Language setting listener
        languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newLanguage = when (checkedId) {
                R.id.radioEnglish -> LocaleHelper.LANGUAGE_ENGLISH
                R.id.radioTraditionalChinese -> LocaleHelper.LANGUAGE_TRADITIONAL_CHINESE
                R.id.radioSimplifiedChinese -> LocaleHelper.LANGUAGE_SIMPLIFIED_CHINESE
                else -> LocaleHelper.LANGUAGE_ENGLISH
            }
            
            val currentLanguage = LocaleHelper.getLanguage(this)
            if (newLanguage != currentLanguage) {
                showLanguageChangeDialog(newLanguage)
            }
        }
        
        // Auto notes on start listener
        switchAutoNotesOnStart.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean("auto_notes_on_start", isChecked)
                .apply()
        }
        
        // Highlight same numbers listener
        switchHighlightSameNumbers.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean("highlight_same_numbers", isChecked)
                .apply()
        }
    }

    private fun showLanguageChangeDialog(newLanguage: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.language))
            .setMessage(getString(R.string.restart_required))
            .setPositiveButton(getString(R.string.restart_now)) { _, _ ->
                LocaleHelper.setLocale(this, newLanguage)
                LocaleHelper.restartActivity(this)
            }
            .setNegativeButton(getString(R.string.restart_later)) { _, _ ->
                LocaleHelper.setLocale(this, newLanguage)
            }
            .setOnDismissListener {
                // Reload language setting to reflect current selection
                loadSettings()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}