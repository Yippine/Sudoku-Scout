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

enum class InputResult {
    SUCCESS,
    DUPLICATE_NUMBER,  // 重複數字 - 任何模式都不允許
    WRONG_ANSWER,      // 馬上校驗模式下的錯誤答案
    INVALID_CELL       // 無效的格子 (已填入或給定)
}

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
        return setValueWithResult(row, col, value) == InputResult.SUCCESS
    }
    
    fun setValueWithResult(row: Int, col: Int, value: Int): InputResult {
        if (row !in 0 until SudokuLogic.SIZE || col !in 0 until SudokuLogic.SIZE) {
            return InputResult.INVALID_CELL
        }
        if (_grid[row][col].isGiven) {
            return InputResult.INVALID_CELL
        }
        
        if (value != 0) {
            // 第一層：基本規則檢查 - 任何模式都不允許重複數字
            val hasBasicViolation = !SudokuLogic.isValidMove(_grid, row, col, value)
            if (hasBasicViolation) {
                return InputResult.DUPLICATE_NUMBER
            }
            
            if (_validateImmediately) {
                // 第二層：馬上校驗 - 檢查是否為唯一正解
                val correctValue = SudokuLogic.getCorrectValue(_grid, row, col)
                val isCorrectAnswer = (value == correctValue)
                
                // 如果不是正確答案，阻止輸入
                if (!isCorrectAnswer) {
                    return InputResult.WRONG_ANSWER
                }
            }
        }
        
        saveToHistory()
        
        val oldValue = _grid[row][col].value
        _grid[row][col].value = value
        _grid[row][col].clearNotes()
        
        // 設置錯誤狀態
        if (value != 0) {
            if (_validateImmediately) {
                // 馬上校驗模式下，只有通過驗證的數字才會到這裡，所以不是錯誤
                _grid[row][col].isError = false
            } else {
                // 完成後校驗模式：允許輸入但不在此時檢查錯誤
                _grid[row][col].isError = false
            }
        } else {
            _grid[row][col].isError = false
        }
        
        // Update notes in related cells if a number was placed
        if (value != 0 && oldValue == 0) {
            updateNotesAfterNumberPlacement(row, col, value)
        }
        
        return InputResult.SUCCESS
    }
    
    fun toggleNote(row: Int, col: Int, number: Int): Boolean {
        if (row !in 0 until SudokuLogic.SIZE || col !in 0 until SudokuLogic.SIZE) return false
        if (_grid[row][col].isGiven || _grid[row][col].value != 0) return false
        if (number !in 1..9) return false
        
        val currentNote = _grid[row][col].hasNote(number)
        
        // If trying to add a note (not remove), check if it's valid
        if (!currentNote) {
            // Check if this number already exists in row, column, or box
            if (!SudokuLogic.isValidMove(_grid, row, col, number)) {
                return false // Invalid note - number already exists in constraints
            }
        }
        
        saveToHistory()
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
    
    fun getHintForCell(row: Int, col: Int): Pair<Int, Int>? {
        // Check if the selected cell can receive a hint
        if (row !in 0 until SudokuLogic.SIZE || col !in 0 until SudokuLogic.SIZE) return null
        if (_grid[row][col].value != 0 || _grid[row][col].isGiven) return null
        
        val correctValue = SudokuLogic.getCorrectValue(_grid, row, col)
        if (setValue(row, col, correctValue)) {
            return Pair(row, col)
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
        return scanCombinations()
    }
    
    fun scanCombinations(): List<CombinationGroup> {
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
    
    fun scanUniqueSolutions(): List<CombinationGroup> {
        val uniqueSolutions = mutableListOf<CombinationGroup>()
        
        // First, scan for cells with only one note (single candidate)
        for (row in 0 until SudokuLogic.SIZE) {
            for (col in 0 until SudokuLogic.SIZE) {
                if (_grid[row][col].value == 0 && _grid[row][col].getNotesCount() == 1) {
                    // Find the single note
                    val singleNote = (1..9).find { _grid[row][col].hasNote(it) }
                    if (singleNote != null) {
                        uniqueSolutions.add(CombinationGroup(
                            cells = listOf(row to col),
                            numbers = setOf(singleNote)
                        ))
                    }
                }
            }
        }
        
        // Scan rows for unique solutions
        for (row in 0 until SudokuLogic.SIZE) {
            uniqueSolutions.addAll(scanGroupForUniqueSolutions(getRowCells(row)))
        }
        
        // Scan columns for unique solutions
        for (col in 0 until SudokuLogic.SIZE) {
            uniqueSolutions.addAll(scanGroupForUniqueSolutions(getColCells(col)))
        }
        
        // Scan 3x3 boxes for unique solutions
        for (boxRow in 0 until SudokuLogic.SIZE step SudokuLogic.BOX_SIZE) {
            for (boxCol in 0 until SudokuLogic.SIZE step SudokuLogic.BOX_SIZE) {
                uniqueSolutions.addAll(scanGroupForUniqueSolutions(getBoxCells(boxRow, boxCol)))
            }
        }
        
        return uniqueSolutions.distinctBy { it.cells.toSet() }
    }
    
    private fun scanGroup(cells: List<Pair<Int, Int>>): List<CombinationGroup> {
        val emptyCells = cells.filter { (row, col) -> _grid[row][col].value == 0 }
        if (emptyCells.size < 2) return emptyList()
        
        val combinations = mutableListOf<CombinationGroup>()
        
        // Group cells by their possible numbers (only use user's manual notes)
        val cellsByNotes = mutableMapOf<Set<Int>, MutableList<Pair<Int, Int>>>()
        
        for ((row, col) in emptyCells) {
            // Only consider cells that have user-filled notes
            // If no notes exist, skip this cell (user hasn't analyzed it yet)
            if (_grid[row][col].getNotesCount() > 0) {
                val possibleNumbers = (1..9).filter { _grid[row][col].hasNote(it) }.toSet()
                
                if (possibleNumbers.isNotEmpty()) {
                    cellsByNotes.getOrPut(possibleNumbers) { mutableListOf() }.add(row to col)
                }
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
    
    private fun scanGroupForUniqueSolutions(cells: List<Pair<Int, Int>>): List<CombinationGroup> {
        val emptyCells = cells.filter { (row, col) -> _grid[row][col].value == 0 }
        if (emptyCells.isEmpty()) return emptyList()
        
        val uniqueSolutions = mutableListOf<CombinationGroup>()
        
        // Check each number 1-9 to see if it has only one possible position in this group
        for (number in 1..9) {
            // Skip if this number is already present in the group
            if (cells.any { (row, col) -> _grid[row][col].value == number }) {
                continue
            }
            
            val possibleCells = emptyCells.filter { (row, col) ->
                // Only consider cells that have user-filled notes
                // If no notes exist, skip this cell (user hasn't analyzed it yet)
                if (_grid[row][col].getNotesCount() == 0) {
                    false
                } else {
                    // Use only user's manual notes
                    _grid[row][col].hasNote(number)
                }
            }
            
            // If only one cell can contain this number, it's a unique solution
            if (possibleCells.size == 1) {
                uniqueSolutions.add(CombinationGroup(possibleCells, setOf(number)))
            }
        }
        
        return uniqueSolutions
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
                val cell = _grid[row][col]
                if (cell.value != 0 && !cell.isGiven) {
                    var isError = false
                    
                    // 基本規則檢查：重複數字（任何模式都要檢查）
                    if (!SudokuLogic.isValidMove(_grid, row, col, cell.value)) {
                        isError = true
                    }
                    
                    // 完成後校驗：檢查是否為正確解答
                    if (!_validateImmediately) {
                        val correctValue = SudokuLogic.getCorrectValue(_grid, row, col)
                        if (cell.value != correctValue) {
                            isError = true
                        }
                    }
                    
                    cell.isError = isError
                    if (isError) {
                        errors.add(row to col)
                    }
                } else {
                    // 清除給定數字或空格的錯誤標記
                    cell.isError = false
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
        
        // 在完成後校驗模式下，需要檢查是否有錯誤
        if (!_validateImmediately) {
            // 執行完整檢查並標記錯誤
            val errors = validateAll()
            if (errors.isNotEmpty()) {
                return false // 有錯誤則未完成
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