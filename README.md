# Sudoku Scout

A feature-rich Android Sudoku game with advanced functions including notes, scanning, save points, and more.

## 🌟 Key Features

### 🎮 Game Core
- **Five Difficulty Levels**: Easy, Medium, Hard, Expert, Evil
- **Smart Sudoku Generation**: Guarantees unique solution puzzles
- **Multi-Solution Validation**: Expert and Evil levels support multiple valid solutions

### ✏️ Notes System
- **9-Position Notes**: Each empty cell can mark possible numbers in 9 positions
  - Position mapping: 1-Top Left, 2-Top, 3-Top Right, 4-Left, 5-Center, 6-Right, 7-Bottom Left, 8-Bottom, 9-Bottom Right
- **Auto Notes**: One-click to fill possible number notes for all empty cells
- **Notes Toggle**: Easy switching between number input and notes mode

### 🔍 Advanced Features
- **Scan Function**: Smart detection of number combinations
  - Detects number combinations in each row, column, and 3x3 box
  - Highlights found combinations to assist reasoning
- **Save Point System**: 
  - Save current game state
  - Quick restoration after experimental number placement
- **Hint System**: Smart hints for next possible number placement

### 🎨 User Interface
- **Intuitive Operation**: Click to select cells, highlight related rows, columns, and boxes
- **Number Panel**: Display numbers 1-9 with remaining counts
- **Error Indication**: Immediate or delayed error detection
- **Theme Colors**: Beautiful Material Design interface

### ⚙️ Settings Options
- **Validation Mode**: 
  - Immediate Validation: Check errors instantly when entering numbers
  - End Validation: Check all errors at once when game is completed
- **Auto Functions**: Auto-fill notes on new game option
- **Highlight Settings**: Same number highlighting control

## 🚀 Technical Features

### 📱 Android Architecture
- **MVVM Pattern**: ViewModel + LiveData reactive programming
- **Material Design**: Modern UI design
- **Custom View**: High-performance Sudoku board rendering
- **SharedPreferences**: Settings persistence

### 🧠 Algorithm Implementation
- **Backtracking Solver**: Efficient Sudoku solving algorithm
- **Unique Solution Verification**: Ensures generated Sudoku has unique solution
- **Combination Detection**: Smart number combination identification
- **Multi-Solution Support**: High difficulty levels support multiple solutions

## 📋 Usage Guide

### Basic Operations
1. **Select Cell**: Click empty cell to select
2. **Enter Number**: Click number buttons on the bottom panel
3. **Notes Mode**: Enable notes mode and click numbers to add small marks in cells
4. **Erase**: Select cell and click erase button to clear content

### Advanced Feature Usage
1. **Auto Notes**: Click auto notes button to add possible numbers to all empty cells
2. **Scan**: Click scan button to find number combinations, found combinations will be highlighted
3. **Save Point**: 
   - Click "Save Point" to save current state
   - Click "Restore Point" to return to saved state
4. **Hint**: Click hint button to get next step suggestion

### Settings Configuration
1. Enter settings page to adjust game options
2. Choose error detection mode (immediate/end)
3. Set default difficulty level
4. Adjust auto functions and highlight options

## 🛠️ Technical Requirements

- **Android Version**: API 24+ (Android 7.0)
- **Programming Language**: Kotlin
- **UI Framework**: Material Design Components
- **Architecture**: MVVM with LiveData

## 📝 Project Structure

```
app/src/main/java/com/sudokuscout/
├── MainActivity.kt          # Main activity
├── SettingsActivity.kt      # Settings page
├── GameViewModel.kt         # Game logic ViewModel
├── GameState.kt            # Game state management
├── SudokuLogic.kt          # Sudoku core algorithms
├── SudokuGridView.kt       # Custom Sudoku board View
└── SudokuCell.kt           # Sudoku cell data class
```

## 🎯 Key Highlights

1. **Complete Notes System**: 9-position notes make reasoning more intuitive
2. **Smart Scan Function**: Automatically identify number combinations, improve solving efficiency
3. **Save Point Mechanism**: Support experimental operations without fear of making mistakes
4. **Multi-Solution Validation**: High difficulty puzzles support multiple valid solutions
5. **User Friendly**: Rich settings options and intuitive operation interface

This is a professional-grade application designed for Sudoku enthusiasts, offering suitable challenges for both beginners and experts!
