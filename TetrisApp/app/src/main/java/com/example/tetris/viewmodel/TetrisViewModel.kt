package com.example.tetris.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tetris.model.Board
import com.example.tetris.model.GameState
import com.example.tetris.model.Piece
import com.example.tetris.model.TetrominoType
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TetrisViewUiState(
    val board: Board = Board(),
    val currentPiece: Piece? = null,
    val nextPieceType: TetrominoType = TetrominoType.I,
    val score: Int = 0,
    val linesCleared: Int = 0,
    val gameState: GameState = GameState.Menu,
    val highScore: Int = 0
)

class TetrisViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TetrisViewUiState())
    val uiState: StateFlow<TetrisViewUiState> = _uiState.asStateFlow()

    private var gameJob: Job? = null
    private val prefs = application.getSharedPreferences("tetris_prefs", Context.MODE_PRIVATE)

    init {
        _uiState.update { it.copy(highScore = loadHighScore()) }
    }

    fun startGame() {
        val firstType = getRandomTetrominoType()
        val nextType = getRandomTetrominoType()
        val firstPiece = Piece(
            type = firstType,
            x = (Board.WIDTH - firstType.getShape(0)[0].size) / 2,
            y = 0
        )

        _uiState.update {
            it.copy(
                board = Board(),
                currentPiece = firstPiece,
                nextPieceType = nextType,
                score = 0,
                linesCleared = 0,
                gameState = GameState.Playing,
                highScore = loadHighScore()
            )
        }

        startGameLoop()
    }

    private fun startGameLoop() {
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            while (isActive) {
                delay(getTickDelay())
                if (_uiState.value.gameState == GameState.Playing) {
                    tick()
                }
            }
        }
    }

    private fun getTickDelay(): Long {
        val level = (_uiState.value.linesCleared / 10) + 1
        return maxOf(100L, 1000L - (level - 1) * 100L)
    }

    private fun tick() {
        val current = _uiState.value.currentPiece ?: return
        val nextPos = current.copy(y = current.y + 1)
        if (!_uiState.value.board.hasCollision(nextPos)) {
            _uiState.update { it.copy(currentPiece = nextPos) }
        } else {
            lockPiece()
        }
    }

    private fun lockPiece() {
        val current = _uiState.value.currentPiece ?: return
        val boardAfterMerge = _uiState.value.board.mergePiece(current)
        val (boardAfterClear, linesClearedThisTurn) = boardAfterMerge.clearLines()

        val points = when (linesClearedThisTurn) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800
            else -> 0
        }

        val newScore = _uiState.value.score + points
        val newLinesCleared = _uiState.value.linesCleared + linesClearedThisTurn

        val isNewHighScore = newScore > _uiState.value.highScore
        val updatedHighScore = if (isNewHighScore) newScore else _uiState.value.highScore
        if (isNewHighScore) {
            saveHighScore(newScore)
        }

        val nextType = getRandomTetrominoType()
        val spawnedPiece = Piece(
            type = _uiState.value.nextPieceType,
            x = (Board.WIDTH - _uiState.value.nextPieceType.getShape(0)[0].size) / 2,
            y = 0
        )

        if (boardAfterClear.hasCollision(spawnedPiece)) {
            gameJob?.cancel()
            _uiState.update {
                it.copy(
                    board = boardAfterClear,
                    currentPiece = null,
                    gameState = GameState.GameOver(newScore, isNewHighScore),
                    score = newScore,
                    linesCleared = newLinesCleared,
                    highScore = updatedHighScore
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    board = boardAfterClear,
                    currentPiece = spawnedPiece,
                    nextPieceType = nextType,
                    score = newScore,
                    linesCleared = newLinesCleared,
                    highScore = updatedHighScore
                )
            }
        }
    }

    fun moveLeft() {
        if (_uiState.value.gameState != GameState.Playing) return
        val current = _uiState.value.currentPiece ?: return
        val nextPos = current.copy(x = current.x - 1)
        if (!_uiState.value.board.hasCollision(nextPos)) {
            _uiState.update { it.copy(currentPiece = nextPos) }
        }
    }

    fun moveRight() {
        if (_uiState.value.gameState != GameState.Playing) return
        val current = _uiState.value.currentPiece ?: return
        val nextPos = current.copy(x = current.x + 1)
        if (!_uiState.value.board.hasCollision(nextPos)) {
            _uiState.update { it.copy(currentPiece = nextPos) }
        }
    }

    fun rotate() {
        if (_uiState.value.gameState != GameState.Playing) return
        val current = _uiState.value.currentPiece ?: return
        val nextPos = current.copy(rotation = (current.rotation + 1) % 4)
        if (!_uiState.value.board.hasCollision(nextPos)) {
            _uiState.update { it.copy(currentPiece = nextPos) }
        }
    }

    fun moveDown() {
        if (_uiState.value.gameState != GameState.Playing) return
        val current = _uiState.value.currentPiece ?: return
        val nextPos = current.copy(y = current.y + 1)
        if (!_uiState.value.board.hasCollision(nextPos)) {
            // Soft drop: +1 очко за каждую клетку
            _uiState.update { it.copy(currentPiece = nextPos, score = it.score + 1) }
        } else {
            lockPiece()
        }
    }

    fun hardDrop() {
        if (_uiState.value.gameState != GameState.Playing) return
        var current = _uiState.value.currentPiece ?: return
        var cellsDropped = 0
        while (true) {
            val nextPos = current.copy(y = current.y + 1)
            if (!_uiState.value.board.hasCollision(nextPos)) {
                current = nextPos
                cellsDropped++
            } else {
                break
            }
        }
        // Hard drop: +2 очка за каждую клетку
        _uiState.update { it.copy(currentPiece = current, score = it.score + cellsDropped * 2) }
        lockPiece()
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.Playing) {
            _uiState.update { it.copy(gameState = GameState.Paused) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.Paused) {
            _uiState.update { it.copy(gameState = GameState.Playing) }
            startGameLoop()
        }
    }

    fun quitToMenu() {
        gameJob?.cancel()
        _uiState.update {
            it.copy(
                board = Board(),
                currentPiece = null,
                score = 0,
                linesCleared = 0,
                gameState = GameState.Menu
            )
        }
    }

    private fun getRandomTetrominoType(): TetrominoType {
        val values = TetrominoType.values()
        return values[Random.nextInt(values.size)]
    }

    private fun loadHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    private fun saveHighScore(score: Int) {
        prefs.edit().putInt("high_score", score).apply()
    }

    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
    }
}
