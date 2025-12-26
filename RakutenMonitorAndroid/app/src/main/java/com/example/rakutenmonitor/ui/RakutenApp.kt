package com.example.rakutenmonitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.rakutenmonitor.R
import com.example.rakutenmonitor.data.SecureStorage
import com.example.rakutenmonitor.worker.UpdateWorker
import kotlinx.coroutines.launch
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

@Composable
fun RakutenApp() {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    var isLoggedIn by remember { mutableStateOf(secureStorage.getUserId() != null) }

    if (isLoggedIn) {
        DashboardScreen(
            onLogout = {
                secureStorage.clear()
                isLoggedIn = false
            }
        )
    } else {
        LoginScreen(
            onLoginSuccess = { userId, password ->
                secureStorage.saveCredentials(userId, password)
                isLoggedIn = true
            }
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text(stringResource(R.string.user_id)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onLoginSuccess(userId, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login_button))
        }
    }
}

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    
    // Auto-schedule work on Dashboard load
    LaunchedEffect(Unit) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateWork = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(10L, TimeUnit.MINUTES) // Jitter/Initial delay
             // Note: WorkManager handles backoff automatically.
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "rakuten_monitor_update",
            ExistingPeriodicWorkPolicy.UPDATE, // Update if exists to apply new policy
            updateWork
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ログイン成功！")
        Spacer(modifier = Modifier.height(8.dp))
        Text("データは1時間ごとにバックグラウンドで更新されます。")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogout) {
            Text("ログアウト")
        }
    }
}
