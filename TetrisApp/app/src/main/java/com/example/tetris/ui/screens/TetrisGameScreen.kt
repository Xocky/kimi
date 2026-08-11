package com.example.tetris.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tetris.model.Board
import com.example.tetris.model.GameState
import com.example.tetris.model.TetrominoType
import com.example.tetris.viewmodel.TetrisViewModel
import kotlin.math.abs

// Минимальное расстояние (px) для срабатывания свайпа
private const val SWIPE_THRESHOLD = 30f

@Composable
fun TetrisGameScreen(viewModel: TetrisViewModel) {
    // Баг 1: collectAsStateWithLifecycle — не жрёт батарею в фоне
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.gameState) {
        if (uiState.gameState == GameState.Playing) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> { viewModel.moveLeft(); true }
                        Key.DirectionRight -> { viewModel.moveRight(); true }
                        Key.DirectionUp -> { viewModel.rotate(); true }
                        Key.DirectionDown -> { viewModel.moveDown(); true }
                        Key.Spacebar -> { viewModel.hardDrop(); true }
                        Key.P -> {
                            if (uiState.gameState == GameState.Playing) viewModel.pauseGame()
                            else if (uiState.gameState == GameState.Paused) viewModel.resumeGame()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KIMI TETRIS",
                    color = Color(0xFF00E5FF),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.gameState == GameState.Playing) {
                    IconButton(
                        onClick = { viewModel.pauseGame() },
                        modifier = Modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                    ) {
                        Text("‖", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Игровое поле с жестами (Баг 3)
                Box(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight()
                        .border(2.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        // Баг 3: swipe-жесты — влево/вправо = move, вниз = soft drop
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                if (uiState.gameState != GameState.Playing) return@detectDragGestures
                                val (dx, dy) = dragAmount
                                when {
                                    abs(dx) > abs(dy) && dx < -SWIPE_THRESHOLD -> viewModel.moveLeft()
                                    abs(dx) > abs(dy) && dx > SWIPE_THRESHOLD  -> viewModel.moveRight()
                                    abs(dy) > abs(dx) && dy > SWIPE_THRESHOLD  -> viewModel.moveDown()
                                }
                            }
                        }
                        // Баг 3: tap = rotate, double tap = hard drop
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (uiState.gameState == GameState.Playing) viewModel.rotate()
                                },
                                onDoubleTap = {
                                    if (uiState.gameState == GameState.Playing) viewModel.hardDrop()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        val width = Board.WIDTH
                        val height = Board.HEIGHT
                        val blockSize = minOf(size.width / width, size.height / height)
                        val offsetX = (size.width - width * blockSize) / 2
                        val offsetY = (size.height - height * blockSize) / 2

                        // Баг 2: вычисляем один раз за кадр, не в каждом drawBlock()
                        val cornerRadiusBlock = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        val cornerRadiusHighlight = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        val blockStroke = Stroke(width = 1.5f)

                        // Сетка
                        for (r in 0 until height) {
                            for (c in 0 until width) {
                                drawRect(
                                    color = Color(0xFF222222),
                                    topLeft = Offset(offsetX + c * blockSize + 0.5f, offsetY + r * blockSize + 0.5f),
                                    size = Size(blockSize - 1f, blockSize - 1f)
                                )
                            }
                        }

                        // Зафиксированные блоки
                        for (r in 0 until height) {
                            for (c in 0 until width) {
                                val colorVal = uiState.board.grid[r][c]
                                if (colorVal != 0L) {
                                    drawBlock(
                                        c, r, Color(colorVal), blockSize, offsetX, offsetY,
                                        cornerRadiusBlock, cornerRadiusHighlight, blockStroke
                                    )
                                }
                            }
                        }

                        // Активная фигура
                        uiState.currentPiece?.let { piece ->
                            val shape = piece.type.getShape(piece.rotation)
                            for (r in shape.indices) {
                                for (c in shape[r].indices) {
                                    if (shape[r][c] == 1) {
                                        val boardX = piece.x + c
                                        val boardY = piece.y + r
                                        if (boardY in 0 until height && boardX in 0 until width) {
                                            drawBlock(
                                                boardX, boardY, Color(piece.type.color),
                                                blockSize, offsetX, offsetY,
                                                cornerRadiusBlock, cornerRadiusHighlight, blockStroke
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "NEXT",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NextPiecePreview(type = uiState.nextPieceType)
                    }

                    StatBox(label = "SCORE", value = "${uiState.score}")
                    StatBox(label = "LINES", value = "${uiState.linesCleared}")
                    StatBox(label = "RECORD", value = "${uiState.highScore}", highlight = true)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(text = "◀", onClick = { viewModel.moveLeft() })
                    ControlButton(
                        text = "↻",
                        onClick = { viewModel.rotate() },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                    ControlButton(text = "▶", onClick = { viewModel.moveRight() })
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.moveDown() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("▼ DROP", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.hardDrop() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("HARD DROP ⚡", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.gameState is GameState.Menu) {
            OverlayContainer {
                Text(
                    text = "TETRIS",
                    color = Color(0xFF00E5FF),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Kimi VS Code Edition",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                if (uiState.highScore > 0) {
                    Text(
                        text = "High Score: ${uiState.highScore}",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                Button(
                    onClick = { viewModel.startGame() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("START GAME", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (uiState.gameState is GameState.Paused) {
            OverlayContainer {
                Text(
                    text = "PAUSED",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(
                    onClick = { viewModel.resumeGame() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.quitToMenu() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("QUIT TO MENU", color = Color.White)
                }
            }
        }

        if (uiState.gameState is GameState.GameOver) {
            val gameOverState = uiState.gameState as GameState.GameOver
            OverlayContainer {
                Text(
                    text = "GAME OVER",
                    color = Color(0xFFFF1744),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Final Score: ${gameOverState.score}",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (gameOverState.isNewHighScore) {
                    Text(
                        text = "NEW HIGH SCORE! 🎉",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Button(
                    onClick = { viewModel.startGame() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("TRY AGAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.quitToMenu() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("QUIT TO MENU", color = Color.White)
                }
            }
        }
    }
}

// Баг 2: CornerRadius и Stroke передаются снаружи — созданы один раз за кадр
private fun DrawScope.drawBlock(
    x: Int, y: Int,
    color: Color,
    blockSize: Float,
    offsetX: Float,
    offsetY: Float,
    cornerRadiusBlock: CornerRadius,
    cornerRadiusHighlight: CornerRadius,
    blockStroke: Stroke
) {
    val left = offsetX + x * blockSize
    val top = offsetY + y * blockSize
    val pad = 1f

    drawRoundRect(
        color = color,
        topLeft = Offset(left + pad, top + pad),
        size = Size(blockSize - 2 * pad, blockSize - 2 * pad),
        cornerRadius = cornerRadiusBlock
    )

    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(left + pad + 2f, top + pad + 2f),
        size = Size(blockSize - 2 * pad - 4f, (blockSize - 2 * pad) / 3f),
        cornerRadius = cornerRadiusHighlight
    )

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(left + pad, top + pad),
        size = Size(blockSize - 2 * pad, blockSize - 2 * pad),
        cornerRadius = cornerRadiusBlock,
        style = blockStroke
    )
}

@Composable
fun NextPiecePreview(type: TetrominoType) {
    val shape = type.getShape(0)
    Box(
        modifier = Modifier
            .size(70.dp)
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val cellCount = maxOf(shape.size, shape[0].size)
            val blockSize = size.width / cellCount
            val offsetX = (size.width - shape[0].size * blockSize) / 2
            val offsetY = (size.height - shape.size * blockSize) / 2
            val cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            val highlightCorner = CornerRadius(1.dp.toPx(), 1.dp.toPx())

            for (r in shape.indices) {
                for (c in shape[r].indices) {
                    if (shape[r][c] == 1) {
                        drawRoundRect(
                            color = Color(type.color),
                            topLeft = Offset(offsetX + c * blockSize + 1f, offsetY + r * blockSize + 1f),
                            size = Size(blockSize - 2f, blockSize - 2f),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.25f),
                            topLeft = Offset(offsetX + c * blockSize + 2.5f, offsetY + r * blockSize + 2.5f),
                            size = Size(blockSize - 5f, blockSize / 3f),
                            cornerRadius = highlightCorner
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, highlight: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = if (highlight) Color(0xFFFFD700) else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun ControlButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = Color(0xFF2A2A2A)
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OverlayContainer(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                .padding(24.dp),
            content = content
        )
    }
}
