# Sudoku Scout - Technical Architecture

## Project Overview
Sudoku Scout is built using modern Android development practices with Kotlin, following the MVVM architectural pattern and Material Design 3 guidelines.

## Architecture Pattern

### MVVM (Model-View-ViewModel)
```
View (Activity/Layout) ↔ ViewModel ↔ Model (GameState)
       ↓                    ↓              ↓
   UI Components      Business Logic   Data Layer
```

### Key Components

#### 1. MainActivity.kt
- **Role**: Main UI controller and view layer
- **Responsibilities**:
  - UI event handling (clicks, touches)
  - Observing ViewModel state changes
  - Managing UI components (buttons, grid, toolbar)
  - Language switching functionality
  - Menu and navigation handling

#### 2. GameViewModel.kt
- **Role**: Business logic and state management
- **Responsibilities**:
  - Managing game state through LiveData
  - Coordinating between UI and game logic
  - Handling user actions (number input, notes, hints)
  - Managing game flow (new game, difficulty changes)

#### 3. GameState.kt
- **Role**: Data model and game state container
- **Properties**:
  - Current puzzle board state
  - Player moves and notes
  - Game progress and completion status
  - Save point data
  - Current difficulty level

#### 4. SudokuLogic.kt
- **Role**: Core sudoku algorithms and validation
- **Functions**:
  - Puzzle generation with unique solutions
  - Move validation against sudoku rules
  - Solution checking and completion detection
  - Advanced solving techniques for hints

#### 5. SudokuGridView.kt
- **Role**: Custom view for game board rendering
- **Features**:
  - Custom drawing of 9x9 grid with sub-grids
  - Touch event handling for cell selection
  - Visual rendering of numbers and 9-position notes
  - Highlighting selected cells and invalid moves

## Data Flow

### 1. User Input Flow
```
User Touch → MainActivity → GameViewModel → GameState → SudokuGridView
                ↓              ↓             ↓            ↓
           UI Updates ← LiveData ← State Change ← Visual Update
```

### 2. Game Logic Flow
```
User Action → ViewModel → SudokuLogic → Validation → State Update → UI Refresh
```

### 3. Language Switching Flow
```
Settings → LocaleHelper → Context Update → Activity Recreate → UI Refresh
```

## Key Design Patterns

### 1. Observer Pattern
- **LiveData**: Reactive UI updates when game state changes
- **Observer Methods**: UI components observe ViewModel state
- **Automatic Updates**: Views update automatically when data changes

### 2. Custom View Pattern
- **SudokuGridView**: Extends View for custom drawing
- **Measurement**: Proper onMeasure() for layout
- **Drawing**: Custom onDraw() for game board rendering
- **Touch Handling**: onTouchEvent() for user interaction

### 3. Singleton Pattern
- **LocaleHelper**: Static utility for language management
- **Game State**: Single source of truth for game data

## Memory Management

### 1. Lifecycle Awareness
- **ViewModel**: Survives configuration changes
- **LiveData**: Automatic lifecycle management
- **Observers**: Automatically cleaned up when activity destroyed

### 2. Resource Optimization
- **View Recycling**: Efficient button reuse
- **Paint Objects**: Reused for drawing operations
- **Bitmap Caching**: Minimal bitmap usage for performance

## Multi-Language Architecture

### 1. Resource Structure
```
res/
├── values/strings.xml           (English - default)
├── values-zh-rTW/strings.xml    (Traditional Chinese)
└── values-zh-rCN/strings.xml    (Simplified Chinese)
```

### 2. LocaleHelper Implementation
- **Context Wrapping**: Provides localized context
- **Preference Storage**: Saves language selection
- **Runtime Switching**: Changes language without app restart

## Build Configuration

### 1. Gradle Setup
- **Compile SDK**: 33 (Android 13)
- **Target SDK**: 33
- **Min SDK**: 24 (Android 7.0)
- **Java Version**: 8 compatibility

### 2. Dependencies
```kotlin
// Core Android
implementation 'androidx.core:core-ktx:1.10.1'
implementation 'androidx.appcompat:appcompat:1.6.1'

// Material Design
implementation 'com.google.android.material:material:1.9.0'

// Architecture Components
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'

// UI Components
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.preference:preference-ktx:1.2.1'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
```

## Threading Model

### 1. Main Thread
- **UI Operations**: All UI updates on main thread
- **LiveData**: Automatically posts to main thread
- **Touch Events**: Handled on main thread

### 2. Background Processing
- **Puzzle Generation**: Heavy computation moved to background
- **Coroutines**: Used for async operations
- **Algorithm Execution**: Solving algorithms run async when needed

## State Management

### 1. Game State Persistence
- **Current Game**: Maintained in ViewModel
- **Save Points**: Stored in GameState
- **Settings**: Persisted in SharedPreferences

### 2. Configuration Changes
- **ViewModel Survival**: Survives rotation and configuration changes
- **State Restoration**: Automatic state restoration
- **Language Persistence**: Maintained across app restarts

## Error Handling

### 1. Input Validation
- **Move Validation**: Prevents invalid sudoku moves
- **Null Safety**: Kotlin null safety features
- **Exception Handling**: Try-catch blocks for critical operations

### 2. Graceful Degradation
- **Language Fallback**: Falls back to English if translation missing
- **Resource Fallback**: Default resources if specific ones unavailable
- **Safe State**: App remains functional even with partial failures

## Performance Considerations

### 1. Rendering Optimization
- **Custom Drawing**: Optimized onDraw() implementation
- **View Invalidation**: Minimal unnecessary redraws
- **Paint Reuse**: Efficient paint object usage

### 2. Memory Efficiency
- **LiveData**: Automatic cleanup of observers
- **Weak References**: Where appropriate to prevent leaks
- **Resource Management**: Proper cleanup in lifecycle methods

## Security Considerations

### 1. Input Sanitization
- **Move Validation**: All moves validated before application
- **State Integrity**: Game state protected from invalid modifications

### 2. Data Protection
- **No Sensitive Data**: No personal or sensitive information stored
- **Local Storage**: All data stored locally on device

## Testing Strategy

### 1. Unit Testing Framework
- **JUnit**: For business logic testing
- **Espresso**: For UI testing
- **Test Structure**: Tests organized by component

### 2. Testable Architecture
- **Separation of Concerns**: Business logic separated from UI
- **Dependency Injection**: Easy mocking for tests
- **Pure Functions**: Sudoku algorithms are pure functions

---

*This architecture documentation provides the technical foundation for understanding and modifying Sudoku Scout. For specific implementation details, refer to the source code files mentioned in each section.*