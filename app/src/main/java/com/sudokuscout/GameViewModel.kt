package com.sudokuscout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState
    
    private val _currentDifficulty = MutableLiveData<Difficulty>()
    val currentDifficulty: LiveData<Difficulty> = _currentDifficulty
    
    private val _isNotesMode = MutableLiveData<Boolean>()
    val isNotesMode: LiveData<Boolean> = _isNotesMode
    
    private val _gameCompleted = MutableLiveData<Boolean>()
    val gameCompleted: LiveData<Boolean> = _gameCompleted
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private var currentGameState: GameState? = null
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    
    init {
        // 從設定載入預設難度
        val defaultDifficultyString = sharedPreferences.getString("default_difficulty", "MEDIUM")
        val defaultDifficulty = when (defaultDifficultyString) {
            "EASY" -> Difficulty.EASY
            "MEDIUM" -> Difficulty.MEDIUM
            "HARD" -> Difficulty.HARD
            "EXPERT" -> Difficulty.EXPERT
            "EVIL" -> Difficulty.EVIL
            else -> Difficulty.MEDIUM
        }
        
        _currentDifficulty.value = defaultDifficulty
        _isNotesMode.value = false
        _gameCompleted.value = false
    }
    
    fun newGame(difficulty: Difficulty = _currentDifficulty.value ?: Difficulty.MEDIUM) {
        _currentDifficulty.value = difficulty
        
        val validateImmediately = sharedPreferences.getBoolean("validate_immediately", true)
        val autoNotesOnStart = sharedPreferences.getBoolean("auto_notes_on_start", false)
        
        currentGameState = GameState(difficulty).apply {
            setValidateImmediately(validateImmediately)
            
            // Auto-fill notes if setting is enabled
            if (autoNotesOnStart) {
                autoFillNotes()
            }
        }
        
        _gameState.value = currentGameState
        _gameCompleted.value = false
    }
    
    fun selectCell(row: Int, col: Int) {
        currentGameState?.let { gameState ->
            gameState.setCurrentSelection(row, col)
            _gameState.value = gameState
        }
    }
    
    fun inputNumber(number: Int) {
        currentGameState?.let { gameState ->
            val selection = gameState.currentSelection
            if (selection != null) {
                val (row, col) = selection
                
                if (gameState.isNotesMode) {
                    // Toggle note
                    val success = gameState.toggleNote(row, col, number)
                    if (!success) {
                        _errorMessage.value = getApplication<Application>()
                            .getString(R.string.duplicate_number_in_notes, number)
                    }
                } else {
                    // Set number
                    val result = gameState.setValueWithResult(row, col, number)
                    when (result) {
                        InputResult.SUCCESS -> {
                            // 成功，無需額外處理
                        }
                        InputResult.DUPLICATE_NUMBER -> {
                            _errorMessage.value = getApplication<Application>()
                                .getString(R.string.duplicate_number_error, number)
                        }
                        InputResult.WRONG_ANSWER -> {
                            _errorMessage.value = getApplication<Application>()
                                .getString(R.string.incorrect_answer_hint)
                        }
                        InputResult.INVALID_CELL -> {
                            _errorMessage.value = getApplication<Application>()
                                .getString(R.string.invalid_cell)
                        }
                    }
                }
                
                _gameState.value = gameState
                checkGameCompletion()
            }
        }
    }
    
    fun eraseSelectedCell() {
        currentGameState?.let { gameState ->
            val selection = gameState.currentSelection
            if (selection != null) {
                val (row, col) = selection
                if (!gameState.grid[row][col].isGiven) {
                    gameState.setValue(row, col, 0)
                    gameState.grid[row][col].clearNotes()
                    _gameState.value = gameState
                }
            }
        }
    }
    
    fun toggleSingleNote(number: Int): Boolean {
        return currentGameState?.let { gameState ->
            val selection = gameState.currentSelection
            if (selection != null) {
                val (row, col) = selection
                if (gameState.grid[row][col].value == 0 && !gameState.grid[row][col].isGiven) {
                    val success = gameState.toggleNote(row, col, number)
                    if (success) {
                        _gameState.value = gameState
                    } else {
                        _errorMessage.value = getApplication<Application>()
                            .getString(R.string.invalid_note, number)
                    }
                    return@let success
                }
            }
            false
        } ?: false
    }
    
    fun undo(): Boolean {
        return currentGameState?.let { gameState ->
            val success = gameState.undo()
            if (success) {
                _gameState.value = gameState
            }
            success
        } ?: false
    }
    
    fun getHint(): Pair<Int, Int>? {
        return currentGameState?.let { gameState ->
            // Try to get hint for selected cell first
            val selection = gameState.currentSelection
            val hintCell = if (selection != null) {
                gameState.getHintForCell(selection.first, selection.second)
            } else {
                gameState.getHint()
            }
            
            if (hintCell != null) {
                _gameState.value = gameState
                checkGameCompletion()
            }
            hintCell
        }
    }
    
    fun setNotesMode(enabled: Boolean) {
        _isNotesMode.value = enabled
        currentGameState?.setNotesMode(enabled)
    }
    
    fun toggleAutoNotes() {
        currentGameState?.let { gameState ->
            if (gameState.grid.any { row -> 
                row.any { cell -> cell.value == 0 && cell.getNotesCount() > 0 } 
            }) {
                // If notes exist, clear them
                gameState.clearAllNotes()
            } else {
                // If no notes exist, auto-fill them
                gameState.autoFillNotes()
            }
            _gameState.value = gameState
        }
    }
    
    fun createSavePoint() {
        currentGameState?.let { gameState ->
            gameState.createSavePoint()
            _gameState.value = gameState
        }
    }
    
    fun restoreSavePoint(): Boolean {
        return currentGameState?.let { gameState ->
            val success = gameState.restoreSavePoint()
            if (success) {
                _gameState.value = gameState
            }
            success
        } ?: false
    }
    
    fun scanForCombinations(): List<CombinationGroup> {
        return currentGameState?.scanCombinations() ?: emptyList()
    }
    
    fun scanForUniqueSolutions(): List<CombinationGroup> {
        return currentGameState?.scanUniqueSolutions() ?: emptyList()
    }
    
    fun validateGame(): List<Pair<Int, Int>> {
        return currentGameState?.let { gameState ->
            val errors = gameState.validateAll()
            _gameState.value = gameState
            errors
        } ?: emptyList()
    }
    
    fun refreshSettings() {
        currentGameState?.let { gameState ->
            val previousValidateImmediately = gameState.validateImmediately
            val validateImmediately = sharedPreferences.getBoolean("validate_immediately", true)
            
            gameState.setValidateImmediately(validateImmediately)
            
            // 如果從"完成後校驗"切換到"馬上校驗"，檢查是否有錯誤數字
            if (!previousValidateImmediately && validateImmediately) {
                val invalidEntries = findInvalidEntries(gameState)
                if (invalidEntries.isNotEmpty()) {
                    // 提示用戶發現錯誤數字，但不強制清除
                    _errorMessage.value = getApplication<Application>()
                        .getString(R.string.validation_mode_changed_with_errors, invalidEntries.size)
                }
            }
            
            _gameState.value = gameState
        }
    }
    
    private fun findInvalidEntries(gameState: GameState): List<Pair<Int, Int>> {
        val invalidEntries = mutableListOf<Pair<Int, Int>>()
        
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = gameState.grid[row][col]
                if (!cell.isGiven && cell.value != 0) {
                    // 檢查是否為正確答案（馬上校驗模式的標準）
                    val correctValue = SudokuLogic.getCorrectValue(gameState.grid, row, col)
                    if (cell.value != correctValue) {
                        invalidEntries.add(row to col)
                        cell.isError = true
                    } else {
                        cell.isError = false
                    }
                }
            }
        }
        
        return invalidEntries
    }
    
    private fun validateCurrentGame() {
        currentGameState?.let { gameState ->
            val invalidEntries = findInvalidEntries(gameState)
            
            if (invalidEntries.isNotEmpty()) {
                _errorMessage.value = getApplication<Application>()
                    .getString(R.string.validation_errors_found)
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    private fun checkGameCompletion() {
        currentGameState?.let { gameState ->
            if (gameState.isGameComplete()) {
                _gameCompleted.value = true
            }
        }
    }
}