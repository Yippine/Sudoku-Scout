# Sudoku Scout - API Reference

## Core Classes and Methods

### MainActivity.kt

#### Class Overview
Main activity handling UI interactions and game display.

#### Key Methods
```kotlin
// UI Setup
private fun setupToolbar()
private fun initializeViews()
private fun setupNumberPanel()
private fun setupObservers()

// Game Actions
private fun onNumberButtonClick(number: Int)
private fun onCellSelected(row: Int, col: Int)
private fun onNewGameClick()
private fun onDifficultySelected(difficulty: Difficulty)

// Tool Functions
private fun onUndoClick()
private fun onEraseClick()
private fun onHintClick()
private fun onAutoNotesClick()
private fun onScanClick()
private fun onSavePointClick()
private fun onRestorePointClick()

// UI Updates
private fun updateNumberButtons(gameState: GameState)
private fun updateSavePointButton(gameState: GameState)
private fun updateToolButtons(gameState: GameState)
```

#### Interface Implementation
```kotlin
class MainActivity : AppCompatActivity(), SudokuGridView.OnCellClickListener {
    override fun onCellClick(row: Int, col: Int)
}
```

---

### GameViewModel.kt

#### Class Overview
ViewModel managing game state and business logic coordination.

#### Properties
```kotlin
// LiveData Properties
val gameState: LiveData<GameState>
val currentDifficulty: LiveData<Difficulty>
val isNotesMode: LiveData<Boolean>
val selectedCell: LiveData<Pair<Int, Int>?>
val isGameComplete: LiveData<Boolean>

// Private Properties
private val _gameState = MutableLiveData<GameState>()
private val _currentDifficulty = MutableLiveData<Difficulty>()
private val _isNotesMode = MutableLiveData<Boolean>()
```

#### Key Methods
```kotlin
// Game Control
fun startNewGame(difficulty: Difficulty)
fun selectCell(row: Int, col: Int)
fun inputNumber(number: Int)
fun toggleNotesMode()
fun clearSelectedCell()

// Advanced Features
fun undoLastMove()
fun generateAutoNotes()
fun getHint(): String?
fun scanForPatterns(): List<ScanResult>
fun createSavePoint()
fun restoreSavePoint()

// Game State Queries
fun isValidMove(row: Int, col: Int, number: Int): Boolean
fun isGameComplete(): Boolean
fun getRemainingCount(number: Int): Int
```

---

### GameState.kt

#### Data Class Overview
Represents complete game state including board, moves, and metadata.

#### Properties
```kotlin
data class GameState(
    // Board State
    val initialBoard: Array<IntArray>,
    val currentBoard: Array<IntArray>,
    val playerBoard: Array<IntArray>,
    val notesBoard: Array<Array<Array<Boolean>>>,
    
    // Game Progress
    val moveHistory: List<Move>,
    val isComplete: Boolean,
    val difficulty: Difficulty,
    
    // Save System
    val savePoint: GameState?,
    val hasSavePoint: Boolean,
    
    // UI State
    val selectedRow: Int,
    val selectedCol: Int,
    val invalidCells: Set<Pair<Int, Int>>
)
```

#### Methods
```kotlin
// Board Operations
fun getCellValue(row: Int, col: Int): Int
fun setCellValue(row: Int, col: Int, value: Int): GameState
fun getNotes(row: Int, col: Int): Array<Boolean>
fun setNote(row: Int, col: Int, number: Int, enabled: Boolean): GameState

// Game Logic
fun isValidMove(row: Int, col: Int, number: Int): Boolean
fun checkCompletion(): Boolean
fun getRemainingNumbers(): Map<Int, Int>

// History Management
fun addMove(move: Move): GameState
fun undoLastMove(): GameState
fun createSavePoint(): GameState
fun restoreFromSavePoint(): GameState?
```

---

### SudokuLogic.kt

#### Object Overview
Static utility methods for sudoku generation, solving, and validation.

#### Core Algorithms
```kotlin
// Puzzle Generation
fun generatePuzzle(difficulty: Difficulty): Array<IntArray>
fun generateSolution(): Array<IntArray>
fun removeCellsForDifficulty(board: Array<IntArray>, difficulty: Difficulty)

// Validation
fun isValidSudoku(board: Array<IntArray>): Boolean
fun isValidMove(board: Array<IntArray>, row: Int, col: Int, number: Int): Boolean
fun hasUniqueSolution(puzzle: Array<IntArray>): Boolean

// Solving Algorithms
fun solvePuzzle(board: Array<IntArray>): Boolean
fun findSolution(board: Array<IntArray>): Array<IntArray>?
fun backtrackSolve(board: Array<IntArray>, row: Int, col: Int): Boolean

// Advanced Techniques
fun findNakedSingles(board: Array<IntArray>): List<Cell>
fun findHiddenSingles(board: Array<IntArray>): List<Cell>
fun generatePossibleValues(board: Array<IntArray>, row: Int, col: Int): Set<Int>

// Auto Notes
fun generateAllNotes(board: Array<IntArray>): Array<Array<Array<Boolean>>>
fun generateCellNotes(board: Array<IntArray>, row: Int, col: Int): Array<Boolean>

// Hint System
fun findNextLogicalMove(board: Array<IntArray>): Hint?
fun explainTechnique(technique: SolvingTechnique): String
```

---

### SudokuGridView.kt

#### Custom View Overview
Custom view for rendering and handling sudoku board interactions.

#### Interface
```kotlin
interface OnCellClickListener {
    fun onCellClick(row: Int, col: Int)
}
```

#### Key Methods
```kotlin
// View Lifecycle
override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
override fun onDraw(canvas: Canvas)
override fun onTouchEvent(event: MotionEvent): Boolean

// Game State Management
fun setGameState(gameState: GameState)
fun setSelectedCell(row: Int, col: Int)
fun setCellClickListener(listener: OnCellClickListener)

// Drawing Methods
private fun drawGrid(canvas: Canvas)
private fun drawNumbers(canvas: Canvas)
private fun drawNotes(canvas: Canvas)
private fun drawSelection(canvas: Canvas)
private fun drawInvalidCells(canvas: Canvas)

// Utility Methods
private fun getCellFromCoordinates(x: Float, y: Float): Pair<Int, Int>?
private fun getCellRect(row: Int, col: Int): RectF
private fun getNotePosition(cellRect: RectF, noteIndex: Int): PointF
```

---

### LocaleHelper.kt

#### Object Overview
Utility for multi-language support and locale management.

#### Constants
```kotlin
const val LANGUAGE_ENGLISH = "en"
const val LANGUAGE_TRADITIONAL_CHINESE = "zh_TW"
const val LANGUAGE_SIMPLIFIED_CHINESE = "zh_CN"
private const val PREF_LANGUAGE = "selected_language"
```

#### Methods
```kotlin
// Language Management
fun setLocale(context: Context, language: String): Context
fun getLanguage(context: Context): String
fun attachBaseContext(context: Context): Context

// Language Utilities
fun getLanguageDisplayName(context: Context, languageCode: String): String
fun getAllLanguageCodes(): List<String>
fun getLocaleFromLanguageCode(languageCode: String): Locale

// Activity Helpers
fun restartActivity(activity: Activity)
private fun updateResources(context: Context, language: String): Context
private fun saveLanguage(context: Context, language: String)
```

---

## Enums and Data Classes

### Difficulty Enum
```kotlin
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD,
    EXPERT,
    EVIL
}
```

### Move Data Class
```kotlin
data class Move(
    val row: Int,
    val col: Int,
    val previousValue: Int,
    val newValue: Int,
    val isNote: Boolean = false,
    val noteNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Hint Data Class
```kotlin
data class Hint(
    val row: Int,
    val col: Int,
    val number: Int,
    val technique: SolvingTechnique,
    val explanation: String
)
```

### ScanResult Data Class
```kotlin
data class ScanResult(
    val cells: List<Pair<Int, Int>>,
    val technique: SolvingTechnique,
    val description: String,
    val numbers: List<Int> = emptyList()
)
```

### SolvingTechnique Enum
```kotlin
enum class SolvingTechnique {
    NAKED_SINGLE,
    HIDDEN_SINGLE,
    BASIC_ELIMINATION,
    INTERSECTION_REMOVAL,
    NAKED_PAIR,
    HIDDEN_PAIR
}
```

## Resource References

### String Resources (English - values/strings.xml)
```xml
<!-- App Information -->
<string name="app_name">Sudoku Scout</string>
<string name="settings">Settings</string>

<!-- Difficulties -->
<string name="easy">Easy</string>
<string name="medium">Medium</string>
<string name="hard">Hard</string>
<string name="expert">Expert</string>
<string name="evil">Evil</string>

<!-- Game Actions -->
<string name="new_game">New Game</string>
<string name="notes">Notes</string>
<string name="undo">Undo</string>
<string name="erase">Erase</string>
<string name="hint">Hint</string>
<string name="auto_notes">Auto Notes</string>
<string name="scan">Scan</string>
<string name="save_point">Save</string>
<string name="restore_point">Restore</string>

<!-- Languages -->
<string name="language_english">English</string>
<string name="language_traditional_chinese">繁體中文</string>
<string name="language_simplified_chinese">简体中文</string>
<string name="select_language">Select Language</string>
```

### Layout Resources
- `activity_main.xml`: Main game layout
- `activity_settings.xml`: Settings screen layout
- Custom view components embedded in main layout

### Drawable Resources
- `ic_launcher_foreground.xml`: App icon vector drawable
- `ic_launcher_legacy.xml`: Legacy app icon
- Color state lists for buttons and components

---

## Usage Examples

### Starting a New Game
```kotlin
// In MainActivity
private fun startNewGame() {
    val selectedDifficulty = gameViewModel.currentDifficulty.value ?: Difficulty.EASY
    gameViewModel.startNewGame(selectedDifficulty)
}
```

### Handling Cell Selection
```kotlin
// In MainActivity implementing OnCellClickListener
override fun onCellClick(row: Int, col: Int) {
    gameViewModel.selectCell(row, col)
}
```

### Input Number with Validation
```kotlin
private fun onNumberButtonClick(number: Int) {
    val currentState = gameViewModel.gameState.value
    val selectedCell = gameViewModel.selectedCell.value
    
    if (currentState != null && selectedCell != null) {
        val (row, col) = selectedCell
        if (gameViewModel.isValidMove(row, col, number)) {
            gameViewModel.inputNumber(number)
        }
    }
}
```

### Language Switching
```kotlin
private fun switchLanguage(languageCode: String) {
    LocaleHelper.setLocale(this, languageCode)
    LocaleHelper.restartActivity(this)
}
```

---

*This API reference provides detailed information about all public methods and classes in Sudoku Scout. For implementation details, refer to the source code files.*