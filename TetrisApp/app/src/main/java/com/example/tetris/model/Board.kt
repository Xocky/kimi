package com.example.tetris.model

class Board(
    val width: Int = WIDTH,
    val height: Int = HEIGHT,
    val grid: List<List<Long>> = List(height) { List(width) { 0L } }
) {
    companion object {
        const val WIDTH = 10
        const val HEIGHT = 20
    }

    /**
     * Проверяет, выходит ли фигура за границы или пересекается с уже занятыми клетками.
     */
    fun hasCollision(piece: Piece): Boolean {
        val shape = piece.type.getShape(piece.rotation)
        for (r in shape.indices) {
            for (c in shape[r].indices) {
                if (shape[r][c] == 1) {
                    val boardX = piece.x + c
                    val boardY = piece.y + r

                    // Проверяем границы: левая, правая и нижняя
                    if (boardX !in 0 until width || boardY >= height) {
                        return true
                    }

                    // Коллизия с уже занятыми клетками (если y >= 0)
                    if (boardY >= 0 && grid[boardY][boardX] != 0L) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Закрепляет фигуру на поле. Возвращает новую доску.
     */
    fun mergePiece(piece: Piece): Board {
        val shape = piece.type.getShape(piece.rotation)
        val newGrid = grid.map { it.toMutableList() }
        for (r in shape.indices) {
            for (c in shape[r].indices) {
                if (shape[r][c] == 1) {
                    val boardX = piece.x + c
                    val boardY = piece.y + r
                    if (boardY in 0 until height && boardX in 0 until width) {
                        newGrid[boardY][boardX] = piece.type.color
                    }
                }
            }
        }
        return Board(width, height, newGrid.map { it.toList() })
    }

    /**
     * Проверяет и удаляет заполненные линии.
     * Возвращает пару: новая доска и количество удаленных линий.
     */
    fun clearLines(): Pair<Board, Int> {
        val filteredGrid = grid.filter { row -> row.any { it == 0L } }
        val clearedLinesCount = height - filteredGrid.size
        if (clearedLinesCount == 0) {
            return this to 0
        }
        val newGrid = MutableList(clearedLinesCount) { List(width) { 0L } }
        newGrid.addAll(filteredGrid)
        return Board(width, height, newGrid.toList()) to clearedLinesCount
    }
}
