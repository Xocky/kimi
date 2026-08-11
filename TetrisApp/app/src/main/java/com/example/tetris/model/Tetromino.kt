package com.example.tetris.model

/**
 * Типы тетромино. ВСЕ 4 состояния rotation хранятся заранее.
 * Цвет как Long (ARGB) — модель не зависит от Compose.
 */
enum class TetrominoType(
    val color: Long,
    private val states: List<List<List<Int>>>
) {
    I(
        color = 0xFF00FFFF,
        states = listOf(
            listOf(listOf(0,0,0,0), listOf(1,1,1,1), listOf(0,0,0,0), listOf(0,0,0,0)),
            listOf(listOf(0,0,1,0), listOf(0,0,1,0), listOf(0,0,1,0), listOf(0,0,1,0)),
            listOf(listOf(0,0,0,0), listOf(0,0,0,0), listOf(1,1,1,1), listOf(0,0,0,0)),
            listOf(listOf(0,1,0,0), listOf(0,1,0,0), listOf(0,1,0,0), listOf(0,1,0,0))
        )
    ),
    O(
        color = 0xFFFFFF00,
        states = List(4) { listOf(listOf(1,1), listOf(1,1)) }
    ),
    T(
        color = 0xFF800080,
        states = listOf(
            listOf(listOf(0,1,0), listOf(1,1,1), listOf(0,0,0)),
            listOf(listOf(0,1,0), listOf(0,1,1), listOf(0,1,0)),
            listOf(listOf(0,0,0), listOf(1,1,1), listOf(0,1,0)),
            listOf(listOf(0,1,0), listOf(1,1,0), listOf(0,1,0))
        )
    ),
    S(
        color = 0xFF00FF00,
        states = listOf(
            listOf(listOf(0,1,1), listOf(1,1,0), listOf(0,0,0)),
            listOf(listOf(0,1,0), listOf(0,1,1), listOf(0,0,1)),
            listOf(listOf(0,0,0), listOf(0,1,1), listOf(1,1,0)),
            listOf(listOf(1,0,0), listOf(1,1,0), listOf(0,1,0))
        )
    ),
    Z(
        color = 0xFFFF0000,
        states = listOf(
            listOf(listOf(1,1,0), listOf(0,1,1), listOf(0,0,0)),
            listOf(listOf(0,0,1), listOf(0,1,1), listOf(0,1,0)),
            listOf(listOf(0,0,0), listOf(1,1,0), listOf(0,1,1)),
            listOf(listOf(0,1,0), listOf(1,1,0), listOf(1,0,0))
        )
    ),
    J(
        color = 0xFF0000FF,
        states = listOf(
            listOf(listOf(1,0,0), listOf(1,1,1), listOf(0,0,0)),
            listOf(listOf(0,1,1), listOf(0,1,0), listOf(0,1,0)),
            listOf(listOf(0,0,0), listOf(1,1,1), listOf(0,0,1)),
            listOf(listOf(0,1,0), listOf(0,1,0), listOf(1,1,0))
        )
    ),
    L(
        color = 0xFFFFA500,
        states = listOf(
            listOf(listOf(0,0,1), listOf(1,1,1), listOf(0,0,0)),
            listOf(listOf(0,1,0), listOf(0,1,0), listOf(0,1,1)),
            listOf(listOf(0,0,0), listOf(1,1,1), listOf(1,0,0)),
            listOf(listOf(1,1,0), listOf(0,1,0), listOf(0,1,0))
        )
    );

    companion object {
        const val MATRIX_SIZE = 4
    }

    fun getShape(rotation: Int): List<List<Int>> = states[rotation % 4]
}

/** Активная фигура на поле */
data class Piece(
    val type: TetrominoType,
    val x: Int,
    val y: Int,
    val rotation: Int = 0
)
