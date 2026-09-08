package com.rounds.test.to_dolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.navigation.TodoNavHost
import com.rounds.test.to_dolist.qa.QaMockControls
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity. It owns the theme and the nav graph and nothing else — every screen lives in its
 * own feature module.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug-only, and a no-op in release. It runs here rather than later because the list's
        // first refresh starts with the first composition, so a QA run has to settle the mock
        // source's failure rate before setContent.
        QaMockControls.install(this, intent)
        enableEdgeToEdge()
        setContent {
            TodoListTheme {
                TodoNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
