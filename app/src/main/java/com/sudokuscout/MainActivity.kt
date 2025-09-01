package com.sudokuscout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.widget.LinearLayout
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), SudokuGridView.OnCellClickListener {

    private lateinit var gameViewModel: GameViewModel
    private lateinit var sudokuGridView: SudokuGridView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var difficultyChip: Chip
    private lateinit var noteModeChip: Chip
    private lateinit var numberPanel: LinearLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    
    // Tool buttons
    private lateinit var btnUndo: Button
    private lateinit var btnErase: Button
    private lateinit var btnHint: Button
    private lateinit var btnAutoNotes: Button
    private lateinit var btnScan: Button
    private lateinit var btnSavePoint: Button
    private lateinit var btnRestorePoint: Button
    
    // Number buttons
    private val numberButtons = mutableListOf<Button>()

    override fun attachBaseContext(newBase: Context) {
        try {
            super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameViewModel = ViewModelProvider(this)[GameViewModel::class.java]
        
        initializeViews()
        setupToolbar()
        setupSudokuGrid()
        setupToolButtons()
        setupNumberButtons()
        setupObservers()
        
        // Start with medium difficulty
        gameViewModel.newGame(Difficulty.MEDIUM)
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        difficultyChip = findViewById(R.id.difficultyChip)
        noteModeChip = findViewById(R.id.noteModeChip)
        numberPanel = findViewById(R.id.numberPanel)
        progressIndicator = findViewById(R.id.progressIndicator)
        
        btnUndo = findViewById(R.id.btnUndo)
        btnErase = findViewById(R.id.btnErase)
        btnHint = findViewById(R.id.btnHint)
        btnAutoNotes = findViewById(R.id.btnAutoNotes)
        btnScan = findViewById(R.id.btnScan)
        btnSavePoint = findViewById(R.id.btnSavePoint)
        btnRestorePoint = findViewById(R.id.btnRestorePoint)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupSudokuGrid() {
        val sudokuContainer = findViewById<FrameLayout>(R.id.sudokuGridFrame)
        sudokuGridView = SudokuGridView(this)
        sudokuGridView.setOnCellClickListener(this)
        sudokuContainer.addView(sudokuGridView)
    }

    private fun setupToolButtons() {
        btnUndo.setOnClickListener {
            if (!gameViewModel.undo()) {
                Toast.makeText(this, getString(R.string.cannot_undo), Toast.LENGTH_SHORT).show()
            }
        }
        
        btnErase.setOnClickListener {
            gameViewModel.eraseSelectedCell()
        }
        
        btnHint.setOnClickListener {
            val hintResult = gameViewModel.getHint()
            if (hintResult == null) {
                Toast.makeText(this, getString(R.string.no_more_hints), Toast.LENGTH_SHORT).show()
            }
        }
        
        btnAutoNotes.setOnClickListener {
            gameViewModel.toggleAutoNotes()
        }
        
        btnScan.setOnClickListener {
            performScan()
        }
        
        btnSavePoint.setOnClickListener {
            gameViewModel.createSavePoint()
        }
        
        btnRestorePoint.setOnClickListener {
            if (!gameViewModel.restoreSavePoint()) {
                Toast.makeText(this, getString(R.string.no_save_point), Toast.LENGTH_SHORT).show()
            }
        }
        
        noteModeChip.setOnCheckedChangeListener { _, isChecked ->
            gameViewModel.setNotesMode(isChecked)
        }
    }

    private fun setupNumberButtons() {
        numberButtons.clear()
        
        // Get all number buttons from the layout
        for (i in 1..9) {
            val buttonId = resources.getIdentifier("btnNumber$i", "id", packageName)
            val button = findViewById<Button>(buttonId)
            numberButtons.add(button)
            
            button.setOnClickListener {
                val number = button.tag.toString().toInt()
                gameViewModel.inputNumber(number)
            }
        }
    }

    private fun setupObservers() {
        gameViewModel.gameState.observe(this) { gameState ->
            sudokuGridView.setGameState(gameState)
            updateNumberButtons(gameState)
            updateSavePointButton(gameState)
        }
        
        gameViewModel.currentDifficulty.observe(this) { difficulty ->
            difficultyChip.text = when (difficulty) {
                Difficulty.EASY -> getString(R.string.easy)
                Difficulty.MEDIUM -> getString(R.string.medium)
                Difficulty.HARD -> getString(R.string.hard)
                Difficulty.EXPERT -> getString(R.string.expert)
                Difficulty.EVIL -> getString(R.string.evil)
                null -> getString(R.string.easy) // Handle potential null
            }
        }
        
        gameViewModel.isNotesMode.observe(this) { isNotesMode ->
            noteModeChip.isChecked = isNotesMode
        }
        
        gameViewModel.gameCompleted.observe(this) { isCompleted ->
            if (isCompleted) {
                showGameCompletedDialog()
            }
        }
        
        gameViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                gameViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateNumberButtons(gameState: GameState) {
        for (i in 0 until numberButtons.size) {
            val number = i + 1
            val button = numberButtons[i]
            val remainingCount = gameState.getRemainingCount(number)
            
            // Update button text with remaining count
            button.text = if (remainingCount > 0) {
                "$number\n$remainingCount"
            } else {
                " \n "
            }
            
            // Disable button if no remaining numbers
            button.isEnabled = remainingCount > 0
            
            // Visual indication
            if (remainingCount == 0) {
                button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            } else {
                button.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }
    }

    private fun updateSavePointButton(gameState: GameState) {
        btnRestorePoint.isEnabled = gameState.hasSavePoint()
    }

    private fun performScan() {
        progressIndicator.show()
        
        // Use coroutine to avoid blocking UI
        CoroutineScope(Dispatchers.Main).launch {
            val combinations = withContext(Dispatchers.Default) {
                gameViewModel.scanForCombinations()
            }
            
            progressIndicator.hide()
            
            if (combinations.isNotEmpty()) {
                sudokuGridView.highlightScanResults(combinations)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.scan_found),
                    Toast.LENGTH_SHORT
                ).show()
                
                // Clear highlight after 3 seconds
                launch {
                    delay(3000)
                    sudokuGridView.clearScanHighlight()
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.no_combinations_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showGameCompletedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.congratulations))
            .setMessage(getString(R.string.sudoku_solved))
            .setPositiveButton("New Game") { _, _ ->
                gameViewModel.newGame()
            }
            .setNegativeButton("OK", null)
            .show()
    }

    override fun onCellClicked(row: Int, col: Int) {
        gameViewModel.selectCell(row, col)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.difficulty_easy -> {
                gameViewModel.newGame(Difficulty.EASY)
                true
            }
            R.id.difficulty_medium -> {
                gameViewModel.newGame(Difficulty.MEDIUM)
                true
            }
            R.id.difficulty_hard -> {
                gameViewModel.newGame(Difficulty.HARD)
                true
            }
            R.id.difficulty_expert -> {
                gameViewModel.newGame(Difficulty.EXPERT)
                true
            }
            R.id.difficulty_evil -> {
                gameViewModel.newGame(Difficulty.EVIL)
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh game state in case settings changed
        gameViewModel.refreshSettings()
    }
}