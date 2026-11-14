package com.tony3974.budgeterv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tony3974.budgeterv2.ui.BudgetScreen
import com.tony3974.budgeterv2.ui.BudgeterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgeterTheme {
                BudgetScreen()
            }
        }
    }
}
