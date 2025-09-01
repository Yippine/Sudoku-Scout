package com.sudokuscout

data class SudokuCell(
    var value: Int = 0,
    var isGiven: Boolean = false,
    var isError: Boolean = false,
    var notes: BooleanArray = BooleanArray(9) { false }
) {
    fun hasNote(number: Int): Boolean {
        return if (number in 1..9) notes[number - 1] else false
    }
    
    fun setNote(number: Int, enabled: Boolean) {
        if (number in 1..9) {
            notes[number - 1] = enabled
        }
    }
    
    fun clearNotes() {
        notes.fill(false)
    }
    
    fun getNotesCount(): Int {
        return notes.count { it }
    }
    
    fun copy(): SudokuCell {
        return SudokuCell(
            value = value,
            isGiven = isGiven,
            isError = isError,
            notes = notes.copyOf()
        )
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as SudokuCell
        
        if (value != other.value) return false
        if (isGiven != other.isGiven) return false
        if (isError != other.isError) return false
        if (!notes.contentEquals(other.notes)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = value
        result = 31 * result + isGiven.hashCode()
        result = 31 * result + isError.hashCode()
        result = 31 * result + notes.contentHashCode()
        return result
    }
}