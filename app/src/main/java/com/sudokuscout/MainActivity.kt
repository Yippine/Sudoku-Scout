package com.sudokuscout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// 移除未使用的導入
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
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
    private lateinit var toolbar: MaterialToolbar
    private lateinit var difficultyChip: Chip
    private lateinit var noteModeChip: Chip
    private lateinit var numberPanel: LinearLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    
    // Primary tool buttons (most frequently used)
    private lateinit var btnUndo: Button
    private lateinit var btnErase: Button
    private lateinit var btnHint: Button
    private lateinit var btnMoreTools: Button
    
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
        setContentView(R.layout.activity_main_new)
        
        gameViewModel = ViewModelProvider(this)[GameViewModel::class.java]
        
        initializeViews()
        setupToolbar()
        setupSudokuGrid()
        setupToolButtons()
        setupNumberButtons()
        setupObservers()
        
        // Start with medium difficulty
        gameViewModel.newGame(Difficulty.MEDIUM)
        
        // 確保高亮設定載入
        sudokuGridView.refreshSettings()
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
        btnMoreTools = findViewById(R.id.btnMoreTools)
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
        
        btnMoreTools.setOnClickListener { view ->
            showToolsMenu(view)
        }
        
        noteModeChip.setOnCheckedChangeListener { _, isChecked ->
            gameViewModel.setNotesMode(isChecked)
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
            
            // Provide visual feedback for notes mode
            if (isNotesMode) {
                noteModeChip.setChipBackgroundColorResource(R.color.button_primary)
                noteModeChip.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                noteModeChip.setChipBackgroundColorResource(android.R.color.transparent)
                noteModeChip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
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
            
            // Update button data using our custom view
            button.setNumberData(number, remainingCount)
        }
    }
    

    private fun showToolsMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.tools_menu, popup.menu)
        
        // Update restore point menu item availability
        val restoreItem = popup.menu.findItem(R.id.action_restore_point)
        restoreItem?.isEnabled = gameViewModel.gameState.value?.hasSavePoint() == true
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_auto_notes -> {
                    gameViewModel.toggleAutoNotes()
                    true
                }
                R.id.action_scan_unique_solutions -> {
                    performScanUniqueSolutions()
                    true
                }
                R.id.action_scan_combinations -> {
                    performScanCombinations()
                    true
                }
                R.id.action_save_point -> {
                    gameViewModel.createSavePoint()
                    Toast.makeText(this, getString(R.string.save_point_created), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_restore_point -> {
                    if (!gameViewModel.restoreSavePoint()) {
                        Toast.makeText(this, getString(R.string.no_save_point), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.save_point_restored), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        
        popup.show()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.difficulty_easy -> {
                confirmDifficultyChange(Difficulty.EASY)
                true
            }
            R.id.difficulty_medium -> {
                confirmDifficultyChange(Difficulty.MEDIUM)
                true
            }
            R.id.difficulty_hard -> {
                confirmDifficultyChange(Difficulty.HARD)
                true
            }
            R.id.difficulty_expert -> {
                confirmDifficultyChange(Difficulty.EXPERT)
                true
            }
            R.id.difficulty_evil -> {
                confirmDifficultyChange(Difficulty.EVIL)
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDifficultyChange(newDifficulty: Difficulty) {
        // Check if game is in progress
        val currentState = gameViewModel.gameState.value
        val hasProgress = currentState?.let { state ->
            state.grid.any { row -> row.any { cell -> !cell.isGiven && cell.value != 0 } }
        } ?: false
        
        if (hasProgress) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.change_difficulty))
                .setMessage(getString(R.string.lose_progress_warning))
                .setPositiveButton(getString(R.string.continue_anyway)) { _, _ ->
                    gameViewModel.newGame(newDifficulty)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            gameViewModel.newGame(newDifficulty)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh game state in case settings changed
        gameViewModel.refreshSettings()
        sudokuGridView.refreshSettings()
    }
}