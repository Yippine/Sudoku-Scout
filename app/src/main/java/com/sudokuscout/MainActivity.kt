package com.sudokuscout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// 移除未使用的導入
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.widget.LinearLayout
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import android.widget.PopupMenu
import android.view.View
import android.view.KeyEvent

class MainActivity : AppCompatActivity(), SudokuGridView.OnCellClickListener {

    private lateinit var gameViewModel: GameViewModel
    private lateinit var sudokuGridView: SudokuGridView
    private lateinit var noteModeButton: Button
    private lateinit var numberPanel: LinearLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    
    // Primary tool buttons (most frequently used)
    private lateinit var btnUndo: Button
    private lateinit var btnErase: Button
    private lateinit var btnHint: Button
    
    // 移除未使用的按鈕變數
    
    // Number buttons
    private val numberButtons = mutableListOf<NumberButtonView>()

    override fun attachBaseContext(newBase: Context) {
        try {
            super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        
        gameViewModel = ViewModelProvider(this)[GameViewModel::class.java]
        
        initializeViews()
        setupSudokuGrid()
        setupToolButtons()
        setupNumberButtons()
        setupObservers()
        
        // Get difficulty from intent or use medium as default
        val difficultyName = intent.getStringExtra("DIFFICULTY")
        val selectedDifficulty = if (difficultyName != null) {
            try {
                Difficulty.valueOf(difficultyName)
            } catch (e: IllegalArgumentException) {
                Difficulty.MEDIUM
            }
        } else {
            Difficulty.MEDIUM
        }
        
        gameViewModel.newGame(selectedDifficulty)
        
        // 確保高亮設定載入
        sudokuGridView.refreshSettings()
    }

    private fun initializeViews() {
        noteModeButton = findViewById(R.id.btnNoteModeToggle)
        numberPanel = findViewById(R.id.numberPanel)
        progressIndicator = findViewById(R.id.progressIndicator)
        
        btnUndo = findViewById(R.id.btnUndo)
        btnErase = findViewById(R.id.btnErase)
        btnHint = findViewById(R.id.btnHint)
        
        // Header buttons
        val btnBack = findViewById<View>(R.id.btnBack)
        val btnNewGame = findViewById<View>(R.id.btnNewGame)
        val btnHeaderSettings = findViewById<View>(R.id.btnHeaderSettings)
        
        // Additional tool buttons
        val btnAutoNotes = findViewById<Button>(R.id.btnAutoNotes)
        val btnSavePoint = findViewById<Button>(R.id.btnSavePoint)
        val btnRestorePoint = findViewById<Button>(R.id.btnRestorePoint)
        val btnScanSolutions = findViewById<Button>(R.id.btnScanSolutions)
        val btnScanCombinations = findViewById<Button>(R.id.btnScanCombinations)
        
        setupHeaderButtons(btnBack, btnNewGame, btnHeaderSettings)
        setupAdditionalToolButtons(btnAutoNotes, btnSavePoint, btnRestorePoint, btnScanSolutions, btnScanCombinations)
    }

    private fun setupHeaderButtons(btnBack: View, btnNewGame: View, btnHeaderSettings: View) {
        btnBack.setOnClickListener {
            finish() // Return to main menu
        }
        
        btnNewGame.setOnClickListener {
            showNewGameConfirmationDialog()
        }
        
        btnHeaderSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun setupAdditionalToolButtons(
        btnAutoNotes: Button,
        btnSavePoint: Button,
        btnRestorePoint: Button,
        btnScanSolutions: Button,
        btnScanCombinations: Button
    ) {
        btnAutoNotes.setOnClickListener {
            gameViewModel.toggleAutoNotes()
        }
        
        btnSavePoint.setOnClickListener {
            gameViewModel.createSavePoint()
            Toast.makeText(this, getString(R.string.save_point_created), Toast.LENGTH_SHORT).show()
        }
        
        btnRestorePoint.setOnClickListener {
            if (!gameViewModel.restoreSavePoint()) {
                Toast.makeText(this, getString(R.string.no_save_point), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.save_point_restored), Toast.LENGTH_SHORT).show()
            }
        }
        
        btnScanSolutions.setOnClickListener {
            performScanUniqueSolutions()
        }
        
        btnScanCombinations.setOnClickListener {
            performScanCombinations()
        }
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
        
        noteModeButton.setOnClickListener {
            val currentMode = gameViewModel.isNotesMode.value ?: false
            gameViewModel.setNotesMode(!currentMode)
        }
    }

    private fun setupNumberButtons() {
        numberButtons.clear()
        
        // 高效能：直接使用資源ID而不是getIdentifier()
        val buttonIds = intArrayOf(
            R.id.btnNumber1, R.id.btnNumber2, R.id.btnNumber3,
            R.id.btnNumber4, R.id.btnNumber5, R.id.btnNumber6,
            R.id.btnNumber7, R.id.btnNumber8, R.id.btnNumber9
        )
        
        for (i in buttonIds.indices) {
            val number = i + 1
            val button = findViewById<NumberButtonView>(buttonIds[i])
            numberButtons.add(button)
            
            // 設定按鈕數據和標籤
            button.setNumberData(number, 9)
            button.tag = number
            
            button.setOnClickListener {
                val buttonNumber = it.tag as Int
                gameViewModel.inputNumber(buttonNumber)
            }
            
            // 長按切換個別註記
            button.setOnLongClickListener {
                val buttonNumber = it.tag as Int
                val success = gameViewModel.toggleSingleNote(buttonNumber)
                if (success) {
                    Toast.makeText(this, 
                        getString(R.string.note_toggled, buttonNumber), 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
        }
    }

    private fun setupObservers() {
        gameViewModel.gameState.observe(this) { gameState ->
            sudokuGridView.setGameState(gameState)
            updateNumberButtons(gameState)
        }
        
        gameViewModel.isNotesMode.observe(this) { isNotesMode ->
            // Update button state for proper background styling
            noteModeButton.isSelected = isNotesMode
            
            // Update text and drawable colors based on state
            if (isNotesMode) {
                noteModeButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
                // Force update drawable tint for selected state
                noteModeButton.compoundDrawablesRelative.forEach { drawable ->
                    drawable?.setTint(ContextCompat.getColor(this, R.color.primary))
                }
            } else {
                noteModeButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
                // Force update drawable tint for normal state
                noteModeButton.compoundDrawablesRelative.forEach { drawable ->
                    drawable?.setTint(ContextCompat.getColor(this, R.color.primary))
                }
            }
            
            // Force refresh the button appearance
            noteModeButton.invalidate()
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
        val validNumbers = gameState.getValidNumbersForSelectedCell()
        
        for (i in 0 until numberButtons.size) {
            val number = i + 1
            val button = numberButtons[i]
            val remainingCount = gameState.getRemainingCount(number)
            
            // Check if this number is valid for the currently selected cell
            val isValidForSelection = if (gameState.currentSelection != null) {
                validNumbers.contains(number)
            } else {
                true // If no cell is selected, show all as normal
            }
            
            // Update button data using our custom view
            button.setNumberData(number, remainingCount, isValidForSelection)
        }
    }
    


    private fun performScanUniqueSolutions() {
        progressIndicator.show()
        
        // Use coroutine to avoid blocking UI
        CoroutineScope(Dispatchers.Main).launch {
            val uniqueSolutions = withContext(Dispatchers.Default) {
                gameViewModel.scanForUniqueSolutions()
            }
            
            progressIndicator.hide()
            
            if (uniqueSolutions.isNotEmpty()) {
                sudokuGridView.highlightScanResults(uniqueSolutions)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.unique_solutions_found),
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
                    getString(R.string.no_unique_solutions_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun performScanCombinations() {
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

    private fun showNewGameConfirmationDialog() {
        val gameState = gameViewModel.gameState.value
        
        // Check if game has progress (any non-empty cells)
        val hasProgress = gameState?.let { state ->
            for (row in 0..8) {
                for (col in 0..8) {
                    val cell = state.grid[row][col]
                    if (cell.value != 0 || cell.getNotesCount() > 0) {
                        return@let true
                    }
                }
            }
            false
        } ?: false
        
        if (hasProgress) {
            // Show confirmation dialog if there's progress
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.confirm_new_game))
                .setMessage(getString(R.string.new_game_warning))
                .setPositiveButton(getString(R.string.continue_anyway)) { _, _ ->
                    gameViewModel.newGame()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            // Start new game directly if no progress
            gameViewModel.newGame()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle number key input when a cell is selected
        val gameState = gameViewModel.gameState.value
        if (gameState?.currentSelection != null) {
            val number = when (keyCode) {
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> 1
                KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> 2
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> 3
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> 4
                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> 5
                KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> 6
                KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> 7
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> 8
                KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> 9
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> 0 // 0 for erase
                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> 0 // Delete key for erase
                else -> null
            }
            
            if (number != null) {
                if (number == 0) {
                    // Erase the selected cell
                    gameViewModel.eraseSelectedCell()
                } else {
                    // Input the number (1-9)
                    gameViewModel.inputNumber(number)
                }
                return true
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }

    
    override fun onResume() {
        super.onResume()
        // Refresh game state in case settings changed
        gameViewModel.refreshSettings()
        sudokuGridView.refreshSettings()
    }
}