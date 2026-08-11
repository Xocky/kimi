package com.example.tetris.model

import androidx.compose.ui.graphics.Color

/**
 * Классическая фигура тетриса (тетромино).
 *
 * Каждая фигура задаётся матрицей 4×4, где 1 — занятая клетка, 0 — пустая.
 * Центр вращения — центр матрицы 4×4, поворот выполняется простым
 * преобразованием координат (без wall kicks — для MVP это допустимо).
 */
enum class Tetromino(
    val color: Color,
    val shape: Array<IntArray>  // матрица 4×4 начального положения
) {
    /** Палка — светло-синий */
    I(
        color = Color(0xFF00FFFF),
        shape = arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),

    /** Квадрат — жёлтый */
    O(
        color = Color(0xFFFFFF00),
        shape = arrayOf(
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),

    /** Т — фиолетовый */
    T(
        color = Color(0xFF800080),
        shape = arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),

    /** S — зелёный */
    S(
        color = Color(0xFF00FF00),
        shape = arrayOf(
            intArrayOf(0, 1, 1, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),

    /** Z — красный */
    Z(
        color = Color(0xFFFF0000),
        shape = arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),

    /** J — синий */
    J(
        color = Color(0xFF0000FF),
        shape = arrayOf(
            intArrayOf(1, 0, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),

    /** L — оранжевый */
    L(
        color = Color(0xFFFFA500),
        shape = arrayOf(
            intArrayOf(0, 0, 1, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    );

    companion object {
        /**
         * Поворот матрицы 4×4 на 90° по часовой стрелке.
         * Формула: new[r][c] = old[SIZE-1-c][r]
         */
        fun rotateClockwise(shape: Array<IntArray>): Array<IntArray> {
            val n = 4
            val result = Array(n) { IntArray(n) }
            for (r in 0 until n) {
                for (c in 0 until n) {
                    result[r][c] = shape[n - 1 - c][r]
                }
            }
            return result
        }

        /**
         * Поворот матрицы 4×4 на 90° против часовой стрелки.
         * Эквивалентно трём поворотам по часовой.
         */
        fun rotateCounterClockwise(shape: Array<IntArray>): Array<IntArray> {
            return rotateClockwise(rotateClockwise(rotateClockwise(shape)))
        }

        /**
         * Возвращает список координат занятых клеток матрицы в виде Pair(колонка, строка).
         */
        fun occupiedCells(shape: Array<IntArray>): List<Pair<Int, Int>> {
            val cells = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    if (shape[r][c] == 1) cells.add(c to r)
                }
            }
            return cells
        }
    }
}
