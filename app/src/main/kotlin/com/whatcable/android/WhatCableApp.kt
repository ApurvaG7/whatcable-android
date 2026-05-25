package com.whatcable.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.whatcable.android.ui.WhatCableNavHost
import com.whatcable.android.ui.theme.WhatCableTheme

@Composable
fun WhatCableApp() {
    WhatCableTheme {
        val navController = rememberNavController()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            WhatCableNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
