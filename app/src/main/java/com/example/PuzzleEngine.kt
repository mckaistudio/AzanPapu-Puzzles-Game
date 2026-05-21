package com.example

import kotlin.math.abs
import kotlin.random.Random

data class PuzzleState(
    val gridSize: Int,
    val tiles: List<Int>, // Current tiles on the board, where value = original position (0 to gridSize*gridSize - 1)
    val emptyTileValue: Int = gridSize * gridSize - 1,
    val movesCount: Int = 0,
    val secondsElapsed: Int = 0,
    val isSolved: Boolean = false,
    val initialShuffleDone: Boolean = false
) {
    fun getEmptyIndex(): Int {
        return tiles.indexOf(emptyTileValue)
    }

    fun canMove(index: Int): Boolean {
        if (isSolved) return false
        val emptyIndex = getEmptyIndex()
        val row = index / gridSize
        val col = index % gridSize
        val emptyRow = emptyIndex / gridSize
        val emptyCol = emptyIndex % gridSize
        return (abs(row - emptyRow) + abs(col - emptyCol)) == 1
    }
}

class PuzzleEngine(private val gridSize: Int) {
    private val totalTiles = gridSize * gridSize
    private val emptyTileValue = totalTiles - 1

    /**
     * Initializes a solvable shuffled puzzle.
     * We start with the solved state and simulate random valid sliding moves.
     * This guarantees that the generated puzzle is 100% solvable.
     */
    fun createInitialState(): PuzzleState {
        var tileList = (0 until totalTiles).toList()
        
        // Let's do 100-200 random walks/slides from the solved state
        val random = Random(System.currentTimeMillis())
        var emptyIndex = totalTiles - 1
        val moveCountLimit = when (gridSize) {
            3 -> 80
            4 -> 150
            else -> 200
        }
        
        for (step in 0 until moveCountLimit) {
            val emptyRow = emptyIndex / gridSize
            val emptyCol = emptyIndex % gridSize
            
            // Collect possible legal swappable adjacent indices
            val possibleAdjacentIndices = mutableListOf<Int>()
            if (emptyRow > 0) possibleAdjacentIndices.add(emptyIndex - gridSize) // Top
            if (emptyRow < gridSize - 1) possibleAdjacentIndices.add(emptyIndex + gridSize) // Bottom
            if (emptyCol > 0) possibleAdjacentIndices.add(emptyIndex - 1) // Left
            if (emptyCol < gridSize - 1) possibleAdjacentIndices.add(emptyIndex + 1) // Right
            
            // Randomly Pick an adjacent tile and swap
            val randomIndexToSwap = possibleAdjacentIndices[random.nextInt(possibleAdjacentIndices.size)]
            val mutableList = tileList.toMutableList()
            // Swap
            val temp = mutableList[emptyIndex]
            mutableList[emptyIndex] = mutableList[randomIndexToSwap]
            mutableList[randomIndexToSwap] = temp
            
            tileList = mutableList
            emptyIndex = randomIndexToSwap
        }

        // Just in case the shuffling randomly ended up in solved state, make one or two extra slides
        if (isSolvedState(tileList, totalTiles)) {
            // Swap empty with first neighbor to un-solve it
            val neighbor = if (emptyIndex > 0) emptyIndex - 1 else emptyIndex + 1
            val mutableList = tileList.toMutableList()
            val temp = mutableList[emptyIndex]
            mutableList[emptyIndex] = mutableList[neighbor]
            mutableList[neighbor] = temp
            tileList = mutableList
        }

        return PuzzleState(
            gridSize = gridSize,
            tiles = tileList,
            emptyTileValue = emptyTileValue,
            movesCount = 0,
            secondsElapsed = 0,
            isSolved = false,
            initialShuffleDone = true
        )
    }

    fun makeMove(currentState: PuzzleState, clickedIndex: Int, onSlideAudio: () -> Unit): PuzzleState {
        if (!currentState.canMove(clickedIndex)) return currentState

        val emptyIndex = currentState.getEmptyIndex()
        val mutableList = currentState.tiles.toMutableList()
        
        // Swap values
        val temp = mutableList[emptyIndex]
        mutableList[emptyIndex] = mutableList[clickedIndex]
        mutableList[clickedIndex] = temp

        onSlideAudio() // Trigger the satisfying click slide audio

        val nowSolved = isSolvedState(mutableList, totalTiles)

        return currentState.copy(
            tiles = mutableList,
            movesCount = currentState.movesCount + 1,
            isSolved = nowSolved
        )
    }

    fun swapTiles(currentState: PuzzleState, index1: Int, index2: Int, onSwapAudio: () -> Unit): PuzzleState {
        if (currentState.isSolved) return currentState
        val mutableList = currentState.tiles.toMutableList()
        
        val temp = mutableList[index1]
        mutableList[index1] = mutableList[index2]
        mutableList[index2] = temp

        onSwapAudio()

        val nowSolved = isSolvedState(mutableList, totalTiles)

        return currentState.copy(
            tiles = mutableList,
            movesCount = currentState.movesCount + 1,
            isSolved = nowSolved
        )
    }

    private fun isSolvedState(tilesList: List<Int>, total: Int): Boolean {
        for (i in 0 until total) {
            if (tilesList[i] != i) return false
        }
        return true
    }
}
