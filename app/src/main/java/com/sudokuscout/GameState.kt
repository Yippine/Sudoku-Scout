package com.sudokuscout

data class GameSnapshot(
    val grid: Array<Array<SudokuCell>>,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as GameSnapshot
        
        if (timestamp != other.timestamp) return false
        if (!grid.contentDeepEquals(other.grid)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class CombinationGroup(
    val cells: List<Pair<Int, Int>>,
    val numbers: Set<Int>
)

class GameState(private val difficulty: Difficulty) {
    private var _grid: Array<Array<SudokuCell>> = SudokuLogic.createEmptyGrid()
    private val _history: MutableList<GameSnapshot> = mutableListOf()
    private var _savePoint: GameSnapshot? = null
    private var _isNotesMode = false
    private var _validateImmediately = true
    private var _currentSelection: Pair<Int, Int>? = null
    private var _originalSolution: Array<Array<SudokuCell>>? = null
    
    val grid: Array<Array<SudokuCell>> get() = _grid
    val isNotesMode: Boolean get() = _isNotesMode
    val validateImmediately: Boolean get() = _validateImmediately
    val currentSelection: Pair<Int, Int>? get() = _currentSelection
    
    init {
        newGame()
    }
    
    fun newGame() {
        _grid = SudokuLogic.generateSudoku(difficulty)
        
        // Store the original complete solution for multi-solution validation
        _originalSolution = SudokuLogic.copyGrid(_grid)
        SudokuLogic.solve(_originalSolution!!)
        
        _history.clear()
        _savePoint = null
        saveToHistory()
    }
    
    fun setValue(row: Int, col: Int, value: Int): Boolean {
        if (row !in 0 until SudokuLogic.SIZE || col !in 0 until SudokuLogic.SIZE) return false
        if (_grid[row][col].isGiven) return false
        
        saveToHistory()
        
        val oldValue = _grid[row][col].value
        _grid[row][col].value = value
        _grid[row][col].clearNotes()
        
        if (_validateImmediately && value != 0) {
            val isValid = SudokuLogic.isValidMove(_grid, row, col, value)
            _grid[row][col].isError = !isValid
            
            if (!isValid) {
                return false
            }
        } else {
            _grid[row][col].isError = false
        }
        
        // Update notes in related cells if a number was placed
        if (value != 0 && oldValue == 0) {
            updateNotesAfterNumberPlacement(row, col, value)
        }
        
        return true
    }
    
    fun toggleNote(row: Int, col: Int, number: Int): Boolean {
        if (row !in 0 until SudokuLogic.SIZE || col !in 0 until SudokuLogic.SIZE) return false
        if (_grid[row][col].isGiven || _grid[row][col].value != 0) return false
        if (number !in 1..9) return false
        
        saveToHistory()
        
        val currentNote = _grid[row][col].hasNote(number)
        _grid[row][col].setNote(number, !currentNote)
        
        return true
    }
    
    fun setNotesMode(enabled: Boolean) {
        _isNotesMode = enabled
    }
    
    fun setValidateImmediately(enabled: Boolean) {
        _validateImmediately = enabled
        if (!enabled) {
            // Clear all error flags when switching to end validation
            clearErrorFlags()
        }
    }
    
    fun setCurrentSelection(row: Int, col: Int) {
        _currentSelection = if (row in 0 until SudokuLogic.SIZE && col in 0 until SudokuLogic.SIZE) {
            Pair(row, col)
        } else {
            null
        }
    }
    
    fun clearSelection() {
        _currentSelection = null
    }
    
    fun autoFillNotes() {
        saveToHistory()
        
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                if (_grid[row][col].value == 0 && !_grid[row][col].isGiven) {
                    val possibleNumbers = SudokuLogic.getPossibleNumbers(_grid, row, col)
                    _grid[row][col].clearNotes()
                    possibleNumbers.forEach { number ->
                        _grid[row][col].setNote(number, true)
                    }
                }
            }
        }
    }
    
    fun clearAllNotes() {
        saveToHistory()
        
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                if (!_grid[row][col].isGiven) {
                    _grid[row][col].clearNotes()
                }
            }
        }
    }
    
    fun getHint(): Pair<Int, Int>? {
        val hintCell = SudokuLogic.findHint(_grid)
        if (hintCell != null) {
            val (row, col) = hintCell
            val correctValue = SudokuLogic.getCorrectValue(_grid, row, col)
            setValue(row, col, correctValue)
            return hintCell
        }
        return null
    }
    
    fun undo(): Boolean {
        if (_history.size > 1) {
            _history.removeAt(_history.lastIndex)
            val previousState = _history.last()
            _grid = SudokuLogic.copyGrid(previousState.grid)
            return true
        }
        return false
    }
    
    fun createSavePoint() {
        _savePoint = GameSnapshot(SudokuLogic.copyGrid(_grid))
    }
    
    fun restoreSavePoint(): Boolean {
        return _savePoint?.let { savePoint ->
            _grid = SudokuLogic.copyGrid(savePoint.grid)
            saveToHistory()
            true
        } ?: false
    }
    
    fun hasSavePoint(): Boolean = _savePoint != null
    
    fun scan(): List<CombinationGroup> {
        val combinations = mutableListOf<CombinationGroup>()
        
        // Scan rows
        for (row in 0 until SudokuLogic.SIZE) {
            combinations.addAll(scanGroup(getRowCells(row)))
        }
        
        // Scan columns
        for (col in 0 until SudokuLogic.SIZE) {
            combinations.addAll(scanGroup(getColCells(col)))
        }
        
        // Scan 3x3 boxes
        for (boxRow in 0 until SudokuLogic.SIZE step SudokuLogic.BOX_SIZE) {
            for (boxCol in 0 until SudokuLogic.SIZE step SudokuLogic.BOX_SIZE) {
                combinations.addAll(scanGroup(getBoxCells(boxRow, boxCol)))
            }
        }
        
        return combinations.distinctBy { it.cells.toSet() }
    }
    
    private fun scanGroup(cells: List<Pair<Int, Int>>): List<CombinationGroup> {
        val emptyCells = cells.filter { (row, col) -> _grid[row][col].value == 0 }
        if (emptyCells.size < 2) return emptyList()
        
        val combinations = mutableListOf<CombinationGroup>()
        
        // Group cells by their possible numbers
        val cellsByNotes = mutableMapOf<Set<Int>, MutableList<Pair<Int, Int>>>()
        
        for ((row, col) in emptyCells) {
            val possibleNumbers = if (_grid[row][col].getNotesCount() > 0) {
                (1..9).filter { _grid[row][col].hasNote(it) }.toSet()
            } else {
                SudokuLogic.getPossibleNumbers(_grid, row, col).toSet()
            }
            
            if (possibleNumbers.isNotEmpty()) {
                cellsByNotes.getOrPut(possibleNumbers) { mutableListOf() }.add(row to col)
            }
        }
        
        // Find groups where number of cells equals number of possible numbers
        for ((numbers, groupCells) in cellsByNotes) {
            if (numbers.size == groupCells.size && numbers.size > 1) {
                combinations.add(CombinationGroup(groupCells, numbers))
            }
        }
        
        return combinations
    }
    
    private fun getRowCells(row: Int): List<Pair<Int, Int>> {
        return (0 until SudokuLogic.SIZE).map { col -> row to col }
    }
    
    private fun getColCells(col: Int): List<Pair<Int, Int>> {
        return (0 until SudokuLogic.SIZE).map { row -> row to col }
    }
    
    private fun getBoxCells(boxRow: Int, boxCol: Int): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in boxRow until boxRow + SudokuLogic.BOX_SIZE) {
            for (col in boxCol until boxCol + SudokuLogic.BOX_SIZE) {
                cells.add(row to col)
            }
        }
        return cells
    }
    
    fun validateAll(): List<Pair<Int, Int>> {
        val errors = mutableListOf<Pair<Int, Int>>()
        
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                val value = _grid[row][col].value
                if (value != 0) {
                    val isValid = SudokuLogic.isValidMove(_grid, row, col, value)
                    _grid[row][col].isError = !isValid
                    if (!isValid) {
                        errors.add(row to col)
                    }
                }
            }
        }
        
        return errors
    }
    
    fun isGameComplete(): Boolean {
        // First check if all cells are filled
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                if (_grid[row][col].value == 0) {
                    return false
                }
            }
        }
        
        // For multi-solution validation, accept any valid complete solution
        // This is especially important for expert/evil difficulty levels
        return isValidAlternateSolution()
    }
    
    private fun isValidAlternateSolution(): Boolean {
        // Create a copy to test
        val testGrid = SudokuLogic.copyGrid(_grid)
        
        // Check if current solution is valid according to Sudoku rules
        if (!SudokuLogic.isValid(testGrid)) {
            return false
        }
        
        // For expert and evil difficulties, accept any valid solution
        // not just the original one, as there might be multiple solutions
        if (difficulty == Difficulty.EXPERT || difficulty == Difficulty.EVIL) {
            return true
        }
        
        // For easier difficulties, we can be more strict and check
        // if it matches the original intended solution
        _originalSolution?.let { originalSolution ->
            // Check if user's solution matches the original
            for (row in 0 until SudokuLogic.SIZE) {
                for (col in 0 until SudokuLogic.SIZE) {
                    if (_grid[row][col].value != originalSolution[row][col].value) {
                        // Different solution found - verify it's still valid
                        // by checking if the original puzzle with this solution is solvable
                        return isAlternateSolutionValid()
                    }
                }
            }
        }
        
        return true
    }
    
    private fun isAlternateSolutionValid(): Boolean {
        // Create a test grid with only the given numbers
        val testGrid = SudokuLogic.createEmptyGrid()
        
        // Copy only the originally given numbers
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                if (_grid[row][col].isGiven) {
                    testGrid[row][col].value = _grid[row][col].value
                    testGrid[row][col].isGiven = true
                }
            }
        }
        
        // Set current user solution
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                if (!testGrid[row][col].isGiven) {
                    testGrid[row][col].value = _grid[row][col].value
                }
            }
        }
        
        // Check if this forms a valid complete Sudoku
        return SudokuLogic.isValid(testGrid)
    }
    
    fun getRemainingCount(number: Int): Int {
        return SudokuLogic.getRemainingCount(_grid, number)
    }
    
    private fun saveToHistory() {
        if (_history.size >= 50) { // Limit history size
            _history.removeAt(0)
        }
        _history.add(GameSnapshot(SudokuLogic.copyGrid(_grid)))
    }
    
    private fun updateNotesAfterNumberPlacement(row: Int, col: Int, number: Int) {
        // Remove this number from notes in same row
        for (c in 0 until SudokuLogic.SIZE) {
            if (c != col) {
                _grid[row][c].setNote(number, false)
            }
        }
        
        // Remove this number from notes in same column
        for (r in 0 until SudokuLogic.SIZE) {
            if (r != row) {
                _grid[r][col].setNote(number, false)
            }
        }
        
        // Remove this number from notes in same box
        val boxRow = (row / SudokuLogic.BOX_SIZE) * SudokuLogic.BOX_SIZE
        val boxCol = (col / SudokuLogic.BOX_SIZE) * SudokuLogic.BOX_SIZE
        
        for (r in boxRow until boxRow + SudokuLogic.BOX_SIZE) {
            for (c in boxCol until boxCol + SudokuLogic.BOX_SIZE) {
                if (r != row || c != col) {
                    _grid[r][c].setNote(number, false)
                }
            }
        }
    }
    
    private fun clearErrorFlags() {
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                _grid[row][col].isError = false
            }
        }
    }
}