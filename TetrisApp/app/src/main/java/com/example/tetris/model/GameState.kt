package com.example.tetris.model

/**
 * Состояния игры — sealed class для строгого контроля переходов.
 */
sealed class GameState {

    /** Главное меню — игра не запущена */
    data object Menu : GameState()

    /** Игра идёт */
    data object Playing : GameState()

    /** Игра на паузе (overlay поверх поля) */
    data object Paused : GameState()

    /** Игра окончена (overlay с итоговым счётом) */
    data class GameOver(val score: Int, val isNewHighScore: Boolean) : GameState()
}
