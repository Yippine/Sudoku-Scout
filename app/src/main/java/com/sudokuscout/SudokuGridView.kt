package com.sudokuscout

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class SudokuGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnCellClickListener {
        fun onCellClicked(row: Int, col: Int)
    }

    private var gameState: GameState? = null
    private var onCellClickListener: OnCellClickListener? = null
    
    private var cellSize = 0f
    private var gridLeft = 0f
    private var gridTop = 0f
    
    // Paints for drawing
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_grid_line, null)
        strokeWidth = dpToPx(1f)
        style = Paint.Style.STROKE
    }
    
    private val thickLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_thick_line, null)
        strokeWidth = dpToPx(3f)
        style = Paint.Style.STROKE
    }
    
    private val cellBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_cell_background, null)
        style = Paint.Style.FILL
    }
    
    private val selectedCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_cell_selected, null)
        style = Paint.Style.FILL
    }
    
    private val highlightedCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_cell_highlighted, null)
        style = Paint.Style.FILL
    }
    
    private val errorCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_cell_error, null)
        style = Paint.Style.FILL
    }
    
    private val sameNumberHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_highlight_same, null)
        style = Paint.Style.FILL
    }
    
    private val scanHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_scan_highlight, null)
        style = Paint.Style.FILL
    }
    
    private val givenTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_text_given, null)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val userTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_text_user, null)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val errorTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_text_error, null)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val notesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.sudoku_notes, null)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    
    // Scan highlighting
    private var scanHighlightedCells = emptySet<Pair<Int, Int>>()
    
    fun setGameState(gameState: GameState) {
        this.gameState = gameState
        invalidate()
    }
    
    fun setOnCellClickListener(listener: OnCellClickListener) {
        this.onCellClickListener = listener
    }
    
    fun highlightScanResults(combinations: List<CombinationGroup>) {
        scanHighlightedCells = combinations.flatMap { it.cells }.toSet()
        invalidate()
    }
    
    fun clearScanHighlight() {
        scanHighlightedCells = emptySet()
        invalidate()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val padding = dpToPx(16f)
        val gridSize = min(w, h) - padding * 2
        cellSize = gridSize / 9f
        
        gridLeft = (w - gridSize) / 2f
        gridTop = (h - gridSize) / 2f
        
        // Update text sizes based on cell size
        givenTextPaint.textSize = cellSize * 0.6f
        userTextPaint.textSize = cellSize * 0.6f
        errorTextPaint.textSize = cellSize * 0.6f
        notesPaint.textSize = cellSize * 0.25f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val gameState = this.gameState ?: return
        
        drawCellBackgrounds(canvas, gameState)
        drawGrid(canvas)
        drawNumbers(canvas, gameState)
        drawNotes(canvas, gameState)
    }
    
    private fun drawCellBackgrounds(canvas: Canvas, gameState: GameState) {
        val selection = gameState.currentSelection
        val grid = gameState.grid
        
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cellRect = getCellRect(row, col)
                val cell = grid[row][col]
                
                // Determine background color
                val paint = when {
                    // Scan highlight has highest priority
                    Pair(row, col) in scanHighlightedCells -> scanHighlightPaint
                    
                    // Error highlight
                    cell.isError -> errorCellPaint
                    
                    // Selected cell
                    selection == Pair(row, col) -> selectedCellPaint
                    
                    // Highlight same numbers
                    selection != null && cell.value != 0 && 
                    grid[selection.first][selection.second].value == cell.value -> 
                        sameNumberHighlightPaint
                    
                    // Highlight related cells (same row, column, or box)
                    selection != null && isRelatedCell(row, col, selection.first, selection.second) ->
                        highlightedCellPaint
                    
                    else -> cellBackgroundPaint
                }
                
                canvas.drawRect(cellRect, paint)
            }
        }
    }
    
    private fun drawGrid(canvas: Canvas) {
        // Draw thin lines
        for (i in 0..9) {
            val offset = i * cellSize
            
            // Vertical lines
            canvas.drawLine(
                gridLeft + offset, gridTop,
                gridLeft + offset, gridTop + cellSize * 9,
                gridLinePaint
            )
            
            // Horizontal lines
            canvas.drawLine(
                gridLeft, gridTop + offset,
                gridLeft + cellSize * 9, gridTop + offset,
                gridLinePaint
            )
        }
        
        // Draw thick lines for 3x3 boxes
        for (i in 0..3) {
            val offset = i * cellSize * 3
            
            // Vertical thick lines
            canvas.drawLine(
                gridLeft + offset, gridTop,
                gridLeft + offset, gridTop + cellSize * 9,
                thickLinePaint
            )
            
            // Horizontal thick lines
            canvas.drawLine(
                gridLeft, gridTop + offset,
                gridLeft + cellSize * 9, gridTop + offset,
                thickLinePaint
            )
        }
    }
    
    private fun drawNumbers(canvas: Canvas, gameState: GameState) {
        val grid = gameState.grid
        
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = grid[row][col]
                if (cell.value != 0) {
                    val centerX = gridLeft + col * cellSize + cellSize / 2
                    val centerY = gridTop + row * cellSize + cellSize / 2
                    
                    val paint = when {
                        cell.isError -> errorTextPaint
                        cell.isGiven -> givenTextPaint
                        else -> userTextPaint
                    }
                    
                    // Center the text vertically
                    val textHeight = paint.descent() - paint.ascent()
                    val textY = centerY + textHeight / 2 - paint.descent()
                    
                    canvas.drawText(
                        cell.value.toString(),
                        centerX,
                        textY,
                        paint
                    )
                }
            }
        }
    }
    
    private fun drawNotes(canvas: Canvas, gameState: GameState) {
        val grid = gameState.grid
        
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = grid[row][col]
                if (cell.value == 0 && cell.getNotesCount() > 0) {
                    drawCellNotes(canvas, row, col, cell)
                }
            }
        }
    }
    
    private fun drawCellNotes(canvas: Canvas, row: Int, col: Int, cell: SudokuCell) {
        val cellLeft = gridLeft + col * cellSize
        val cellTop = gridTop + row * cellSize
        val noteSize = cellSize / 3f
        
        for (number in 1..9) {
            if (cell.hasNote(number)) {
                val noteRow = (number - 1) / 3
                val noteCol = (number - 1) % 3
                
                val noteX = cellLeft + noteCol * noteSize + noteSize / 2
                val noteY = cellTop + noteRow * noteSize + noteSize / 2
                
                // Center the text vertically
                val textHeight = notesPaint.descent() - notesPaint.ascent()
                val textY = noteY + textHeight / 2 - notesPaint.descent()
                
                canvas.drawText(
                    number.toString(),
                    noteX,
                    textY,
                    notesPaint
                )
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val col = ((event.x - gridLeft) / cellSize).toInt()
            val row = ((event.y - gridTop) / cellSize).toInt()
            
            if (row in 0 until 9 && col in 0 until 9) {
                onCellClickListener?.onCellClicked(row, col)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun getCellRect(row: Int, col: Int): RectF {
        val left = gridLeft + col * cellSize
        val top = gridTop + row * cellSize
        return RectF(left, top, left + cellSize, top + cellSize)
    }
    
    private fun isRelatedCell(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        // Same row
        if (row1 == row2) return true
        
        // Same column
        if (col1 == col2) return true
        
        // Same 3x3 box
        val box1Row = row1 / 3
        val box1Col = col1 / 3
        val box2Row = row2 / 3
        val box2Col = col2 / 3
        
        return box1Row == box2Row && box1Col == box2Col
    }
    
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}