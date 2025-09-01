package com.sudokuscout

import kotlin.random.Random

enum class Difficulty {
    EASY, MEDIUM, HARD, EXPERT, EVIL
}

class SudokuLogic {
    companion object {
        const val SIZE = 9
        const val BOX_SIZE = 3
        
        fun isValidMove(grid: Array<Array<SudokuCell>>, row: Int, col: Int, number: Int): Boolean {
            // Check row
            for (c in 0 until SIZE) {
                if (c != col && grid[row][c].value == number) {
                    return false
                }
            }
            
            // Check column
            for (r in 0 until SIZE) {
                if (r != row && grid[r][col].value == number) {
                    return false
                }
            }
            
            // Check 3x3 box
            val boxRow = (row / BOX_SIZE) * BOX_SIZE
            val boxCol = (col / BOX_SIZE) * BOX_SIZE
            
            for (r in boxRow until boxRow + BOX_SIZE) {
                for (c in boxCol until boxCol + BOX_SIZE) {
                    if ((r != row || c != col) && grid[r][c].value == number) {
                        return false
                    }
                }
            }
            
            return true
        }
        
        fun getPossibleNumbers(grid: Array<Array<SudokuCell>>, row: Int, col: Int): List<Int> {
            if (grid[row][col].value != 0) return emptyList()
            
            val possible = mutableListOf<Int>()
            for (num in 1..9) {
                if (isValidMove(grid, row, col, num)) {
                    possible.add(num)
                }
            }
            return possible
        }
        
        fun solve(grid: Array<Array<SudokuCell>>): Boolean {
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    if (grid[row][col].value == 0) {
                        for (num in 1..9) {
                            if (isValidMove(grid, row, col, num)) {
                                grid[row][col].value = num
                                if (solve(grid)) {
                                    return true
                                }
                                grid[row][col].value = 0
                            }
                        }
                        return false
                    }
                }
            }
            return true
        }
        
        fun hasUniqueSolution(grid: Array<Array<SudokuCell>>): Boolean {
            val workingGrid = copyGrid(grid)
            val solutions = countSolutions(workingGrid, 2) // We only need to know if there's more than 1
            return solutions == 1
        }
        
        private fun countSolutions(grid: Array<Array<SudokuCell>>, maxCount: Int): Int {
            var solutionCount = 0
            
            fun backtrack(): Unit {
                if (solutionCount >= maxCount) return
                
                for (row in 0 until SIZE) {
                    for (col in 0 until SIZE) {
                        if (grid[row][col].value == 0) {
                            for (num in 1..9) {
                                if (isValidMove(grid, row, col, num)) {
                                    grid[row][col].value = num
                                    backtrack()
                                    grid[row][col].value = 0
                                    if (solutionCount >= maxCount) return
                                }
                            }
                            return
                        }
                    }
                }
                solutionCount++
            }
            
            backtrack()
            return solutionCount
        }
        
        fun isComplete(grid: Array<Array<SudokuCell>>): Boolean {
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    if (grid[row][col].value == 0) {
                        return false
                    }
                }
            }
            return isValid(grid)
        }
        
        fun isValid(grid: Array<Array<SudokuCell>>): Boolean {
            // Check rows
            for (row in 0 until SIZE) {
                val seen = BooleanArray(SIZE + 1)
                for (col in 0 until SIZE) {
                    val value = grid[row][col].value
                    if (value != 0) {
                        if (seen[value]) return false
                        seen[value] = true
                    }
                }
            }
            
            // Check columns
            for (col in 0 until SIZE) {
                val seen = BooleanArray(SIZE + 1)
                for (row in 0 until SIZE) {
                    val value = grid[row][col].value
                    if (value != 0) {
                        if (seen[value]) return false
                        seen[value] = true
                    }
                }
            }
            
            // Check 3x3 boxes
            for (boxRow in 0 until SIZE step BOX_SIZE) {
                for (boxCol in 0 until SIZE step BOX_SIZE) {
                    val seen = BooleanArray(SIZE + 1)
                    for (row in boxRow until boxRow + BOX_SIZE) {
                        for (col in boxCol until boxCol + BOX_SIZE) {
                            val value = grid[row][col].value
                            if (value != 0) {
                                if (seen[value]) return false
                                seen[value] = true
                            }
                        }
                    }
                }
            }
            
            return true
        }
        
        fun generateSudoku(difficulty: Difficulty): Array<Array<SudokuCell>> {
            val grid = createEmptyGrid()
            
            // Fill diagonal boxes first
            fillDiagonalBoxes(grid)
            
            // Solve the rest
            solve(grid)
            
            // Mark all cells as given initially
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    grid[row][col].isGiven = true
                }
            }
            
            // Remove numbers based on difficulty
            val cellsToRemove = when (difficulty) {
                Difficulty.EASY -> 40
                Difficulty.MEDIUM -> 50
                Difficulty.HARD -> 55
                Difficulty.EXPERT -> 60
                Difficulty.EVIL -> 65
            }
            
            removeNumbers(grid, cellsToRemove)
            
            return grid
        }
        
        private fun fillDiagonalBoxes(grid: Array<Array<SudokuCell>>) {
            for (box in 0 until SIZE step BOX_SIZE) {
                fillBox(grid, box, box)
            }
        }
        
        private fun fillBox(grid: Array<Array<SudokuCell>>, row: Int, col: Int) {
            val numbers = (1..9).shuffled(Random.Default)
            var index = 0
            for (r in row until row + BOX_SIZE) {
                for (c in col until col + BOX_SIZE) {
                    grid[r][c].value = numbers[index++]
                }
            }
        }
        
        private fun removeNumbers(grid: Array<Array<SudokuCell>>, count: Int) {
            val cells = mutableListOf<Pair<Int, Int>>()
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    cells.add(Pair(row, col))
                }
            }
            cells.shuffle(Random.Default)
            
            var removed = 0
            for ((row, col) in cells) {
                if (removed >= count) break
                
                val backup = grid[row][col].value
                grid[row][col].value = 0
                grid[row][col].isGiven = false
                
                val workingGrid = copyGrid(grid)
                if (hasUniqueSolution(workingGrid)) {
                    removed++
                } else {
                    // Restore the number if removing it creates multiple solutions
                    grid[row][col].value = backup
                    grid[row][col].isGiven = true
                }
            }
        }
        
        fun createEmptyGrid(): Array<Array<SudokuCell>> {
            return Array(SIZE) { Array(SIZE) { SudokuCell() } }
        }
        
        fun copyGrid(original: Array<Array<SudokuCell>>): Array<Array<SudokuCell>> {
            return Array(SIZE) { row ->
                Array(SIZE) { col ->
                    original[row][col].copy()
                }
            }
        }
        
        fun findHint(grid: Array<Array<SudokuCell>>): Pair<Int, Int>? {
            // Find the first empty cell that has a unique solution
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    if (grid[row][col].value == 0) {
                        val possible = getPossibleNumbers(grid, row, col)
                        if (possible.size == 1) {
                            return Pair(row, col)
                        }
                    }
                }
            }
            
            // If no unique solution found, return any empty cell
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    if (grid[row][col].value == 0) {
                        return Pair(row, col)
                    }
                }
            }
            
            return null
        }
        
        fun getCorrectValue(grid: Array<Array<SudokuCell>>, row: Int, col: Int): Int {
            val workingGrid = copyGrid(grid)
            solve(workingGrid)
            return workingGrid[row][col].value
        }
        
        fun getRemainingCount(grid: Array<Array<SudokuCell>>, number: Int): Int {
            var count = 0
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    if (grid[row][col].value == number) {
                        count++
                    }
                }
            }
            return 9 - count
        }
    }
}