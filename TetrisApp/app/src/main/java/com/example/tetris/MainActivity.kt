package com.example.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.tetris.ui.screens.TetrisGameScreen
import com.example.tetris.ui.theme.TetrisTheme
import com.example.tetris.viewmodel.TetrisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TetrisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TetrisTheme {
                TetrisGameScreen(viewModel = viewModel)
            }
        }
    }
}
