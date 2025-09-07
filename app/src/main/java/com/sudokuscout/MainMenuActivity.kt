package com.sudokuscout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

class MainMenuActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        try {
            super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        
        setupDifficultyCards()
        setupSettingsButton()
    }

    private fun setupDifficultyCards() {
        val easyCard = findViewById<MaterialCardView>(R.id.cardEasy)
        val mediumCard = findViewById<MaterialCardView>(R.id.cardMedium)
        val hardCard = findViewById<MaterialCardView>(R.id.cardHard)
        val expertCard = findViewById<MaterialCardView>(R.id.cardExpert)
        val evilCard = findViewById<MaterialCardView>(R.id.cardEvil)

        easyCard.setOnClickListener { startGame(Difficulty.EASY) }
        mediumCard.setOnClickListener { startGame(Difficulty.MEDIUM) }
        hardCard.setOnClickListener { startGame(Difficulty.HARD) }
        expertCard.setOnClickListener { startGame(Difficulty.EXPERT) }
        evilCard.setOnClickListener { startGame(Difficulty.EVIL) }
    }

    private fun setupSettingsButton() {
        val settingsButton = findViewById<View>(R.id.btnHeaderSettings)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun startGame(difficulty: Difficulty) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("DIFFICULTY", difficulty.name)
        }
        startActivity(intent)
    }
}