package com.example.rakutenmonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.compose.ui.draw.shadow
import com.example.rakutenmonitor.R
import com.example.rakutenmonitor.data.AppPreferences
import com.example.rakutenmonitor.data.RakutenRepository
import com.example.rakutenmonitor.data.SecureStorage
import com.example.rakutenmonitor.ui.components.CircularChart
import com.example.rakutenmonitor.worker.UpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.rakutenmonitor.ui.theme.DarkGradientEnd
import com.example.rakutenmonitor.ui.theme.DarkGradientStart
import com.example.rakutenmonitor.ui.theme.GlassBorderDark
import com.example.rakutenmonitor.ui.theme.GlassBorderLight
import com.example.rakutenmonitor.ui.theme.GlassSurfaceDark
import com.example.rakutenmonitor.ui.theme.GlassSurfaceLight
import com.example.rakutenmonitor.ui.theme.GradientEnd
import com.example.rakutenmonitor.ui.theme.GradientStart

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val brush = if (isDark) {
        Brush.verticalGradient(listOf(DarkGradientStart, DarkGradientEnd))
    } else {
        Brush.verticalGradient(listOf(GradientStart, GradientEnd))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val borderColor = if (isDark) GlassBorderLight else GlassBorderLight // Always light border looks good for glass
    
    Surface(
        modifier = modifier
            .border(
                BorderStroke(1.dp, borderColor),
                shape
            )
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        color = backgroundColor,
        shape = shape,
        content = content
    )
}

@Composable
fun RakutenApp() {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    var isLoggedIn by remember { mutableStateOf(secureStorage.getUserId() != null) }

    GlassBackground {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rakuten Monitor", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text(stringResource(R.string.user_id)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onLoginSuccess(userId, password) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.login_button))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val usage by appPreferences.usageFlow.collectAsState()
    val lastUpdated by appPreferences.lastUpdatedFlow.collectAsState()
    
    // Theme Settings State
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val currentThemeMode by appPreferences.themeModeFlow.collectAsState()
    
    // Refresh State
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isLoginRequired by remember { mutableStateOf(false) }
    var showLoginWebView by remember { mutableStateOf(false) }

    // Determine circular chart limit and next milestone
    val (chartLimit, nextMilestoneText) = when {
        usage < 3.0f -> 3.0f to "3GB まで あと${String.format("%.1f", 3.0f - usage)} GB"
        usage < 20.0f -> 20.0f to "20GB まで あと${String.format("%.1f", 20.0f - usage)} GB"
        else -> 50.0f to "無制限エリア (20GB超過)"
    }

    // Auto-schedule work on Dashboard load
    LaunchedEffect(Unit) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateWork = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(5L, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "rakuten_monitor_update",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateWork
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "テーマ設定",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ThemeOption(
                        text = "システム設定に従う",
                        selected = currentThemeMode == AppPreferences.ThemeMode.SYSTEM,
                        onClick = { appPreferences.saveThemeMode(AppPreferences.ThemeMode.SYSTEM) }
                    )
                    ThemeOption(
                        text = "ライトモード",
                        selected = currentThemeMode == AppPreferences.ThemeMode.LIGHT,
                        onClick = { appPreferences.saveThemeMode(AppPreferences.ThemeMode.LIGHT) }
                    )
                    ThemeOption(
                        text = "ダークモード",
                        selected = currentThemeMode == AppPreferences.ThemeMode.DARK,
                        onClick = { appPreferences.saveThemeMode(AppPreferences.ThemeMode.DARK) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Logout Option in Settings
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            showLogoutConfirmation = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ログアウト", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("閉じる")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.95f), // Slightly transparent dialog
            shape = RoundedCornerShape(28.dp)
        )
    }
    
    // Logout Confirmation Dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("ログアウト") },
            text = { Text("本当にログアウトしますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ログアウト")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("キャンセル")
                }
            },
             containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.95f)
        )
    }

    // Login WebView Dialog
    if (showLoginWebView) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showLoginWebView = false }) {
             // Full screen dialog-ish
             Surface(modifier = Modifier.fillMaxSize()) {
                 Column {
                     // Close Button
                     IconButton(onClick = { showLoginWebView = false }, modifier = Modifier.align(Alignment.End)) {
                         Icon(Icons.Default.Close, contentDescription = "Close")
                     }
                     LoginWebView(
                         onLoginSuccess = {
                             showLoginWebView = false
                             isLoginRequired = false
                             statusMessage = "Login Success. Refreshing..."
                             // Trigger refresh
                              scope.launch {
                                    isLoading = true
                                    val result = RakutenRepository(context).fetchData { msg -> statusMessage = msg }
                                    if (result.isSuccess) {
                                         val u = result.getOrNull() ?: 0.0
                                         appPreferences.saveUsage(u.toFloat())
                                         appPreferences.saveLastUpdated(SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date()))
                                    }
                                    isLoading = false
                                    statusMessage = "" // Clear status after refresh
                              }
                         }, 
                         onDismiss = { showLoginWebView = false }
                     )
                 }
             }
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // Transparent for gradient
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Rakuten Monitor",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent, // Glass header
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoginRequired) {
                                 Button(
                                    onClick = { showLoginWebView = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(30.dp).padding(end = 4.dp)
                                 ) {
                                     Text("再ログイン", fontSize = 10.sp)
                                 }
                            }
                            IconButton(onClick = {
                                isLoading = true
                                isLoginRequired = false // Reset login required state on manual refresh
                                statusMessage = "Starting..."
                                scope.launch {
                                    try {
                                        val result = RakutenRepository(context).fetchData { msg ->
                                            statusMessage = msg
                                        }
                                        
                                        if (result.isSuccess) {
                                            val u = result.getOrNull() ?: 0.0
                                            val currentTime = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
                                            
                                            appPreferences.saveUsage(u.toFloat())
                                            appPreferences.saveLastUpdated(currentTime)
                                            
                                            // Schedule Background Work
                                            val workRequest = OneTimeWorkRequestBuilder<UpdateWorker>().build()
                                            WorkManager.getInstance(context).enqueue(workRequest)

                                            Toast.makeText(context, "更新完了", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Handle Failure
                                            val ex = result.exceptionOrNull()
                                            if (ex is com.example.rakutenmonitor.data.LoginRequiredException) {
                                                statusMessage = "要ログイン"
                                                isLoginRequired = true
                                                Toast.makeText(context, "再ログインが必要です", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "更新失敗: ${ex?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch(e: Exception) {
                                         if (e is com.example.rakutenmonitor.data.LoginRequiredException) {
                                            statusMessage = "要ログイン"
                                            isLoginRequired = true
                                         } else {
                                            e.printStackTrace()
                                            Toast.makeText(context, "エラー: ${e.message}", Toast.LENGTH_LONG).show()
                                         }
                                    } finally {
                                        isLoading = false
                                        if (!isLoginRequired && statusMessage == "Starting...") statusMessage = "" // Clear generic status if not login required
                                    }
                                }
                            }) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        
                        // Status Text below the button
                        if (statusMessage.isNotEmpty()) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.labelSmall, // Smaller font
                                color = if (isLoginRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Data Usage - GLASS CARD
            GlassCard(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(40.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "今月のデータ使用量",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    CircularChart(
                        usage = usage,
                        limit = chartLimit,
                        radius = 120.dp,
                        strokeWidth = 28.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = nextMilestoneText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Card - GLASS CARD
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       Text(
                           text = "最終更新",
                           style = MaterialTheme.typography.labelMedium,
                           color = MaterialTheme.colorScheme.onSurfaceVariant
                       )
                       Spacer(modifier = Modifier.width(8.dp))
                       Text(
                           text = if (lastUpdated.isNotEmpty()) lastUpdated else "未更新",
                           style = MaterialTheme.typography.bodyLarge,
                           fontWeight = FontWeight.Medium,
                           color = MaterialTheme.colorScheme.onSurface
                       )
                   }
                }
            }
        }
    }
}

@Composable
fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LoginWebView(onLoginSuccess: () -> Unit, onDismiss: () -> Unit) {
    AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url?.contains("portal.mobile.rakuten.co.jp/dashboard") == true) {
                            onLoginSuccess()
                        }
                    }
                }
                loadUrl("https://portal.mobile.rakuten.co.jp/dashboard")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
