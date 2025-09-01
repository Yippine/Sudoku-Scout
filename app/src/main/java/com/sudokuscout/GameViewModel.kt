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
        _currentDifficulty.value = Difficulty.MEDIUM
        _isNotesMode.value = false
        _gameCompleted.value = false
    }
    
    fun newGame(difficulty: Difficulty = _currentDifficulty.value ?: Difficulty.MEDIUM) {
        _currentDifficulty.value = difficulty
        
        val validateImmediately = sharedPreferences.getBoolean("validate_immediately", true)
        
        currentGameState = GameState(difficulty).apply {
            setValidateImmediately(validateImmediately)
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
                    gameState.toggleNote(row, col, number)
                } else {
                    // Set number
                    val success = gameState.setValue(row, col, number)
                    if (!success && gameState.validateImmediately) {
                        _errorMessage.value = getApplication<Application>()
                            .getString(R.string.invalid_number)
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
            val hintCell = gameState.getHint()
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
        return currentGameState?.scan() ?: emptyList()
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
            val validateImmediately = sharedPreferences.getBoolean("validate_immediately", true)
            gameState.setValidateImmediately(validateImmediately)
            _gameState.value = gameState
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