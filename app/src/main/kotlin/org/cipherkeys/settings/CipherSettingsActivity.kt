package org.cipherkeys.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.cipherkeys.cipher.KeyManager
import org.cipherkeys.cipher.KeyStore
import org.cipherkeys.cipher.RecipientManager
import org.cipherkeys.settings.help.HelpDetailScreen
import org.cipherkeys.settings.help.HelpScreen
import org.cipherkeys.settings.help.allHelpEntries
import org.cipherkeys.ui.CipherPrefs

class CipherSettingsActivity : ComponentActivity() {

    lateinit var keyStore: KeyStore
    lateinit var keyManager: KeyManager
    lateinit var recipientManager: RecipientManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CipherPrefs.init(this)

        keyStore = KeyStore(this)
        keyManager = KeyManager(keyStore)
        recipientManager = RecipientManager(this)

        val startScreen = intent.getStringExtra("screen") ?: "settings"

        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
            ) {
                SettingsNav(
                    keyManager = keyManager,
                    recipientManager = recipientManager,
                    startScreen = startScreen,
                    onBack = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNav(
    keyManager: KeyManager,
    recipientManager: RecipientManager,
    startScreen: String = "settings",
    onBack: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getTitle(currentRoute)) },
                navigationIcon = {
                    if (currentRoute == "settings" || currentRoute == null) {
                        IconButton(onClick = { navController.navigate("about") }) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("CK", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startScreen,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable("settings") {
                MainSettingsScreen(
                    onKeysClick = { navController.navigate("keys") },
                    onRecipientsClick = { navController.navigate("recipients") },
                    onHelpClick = { navController.navigate("help") },
                )
            }
            composable("keys") {
                KeyListScreen(
                    keyManager = keyManager,
                    onGenerateClick = { navController.navigate("keys/generate") },
                    onImportClick = { navController.navigate("keys/import") },
                )
            }
            composable("keys/generate") {
                KeyGenScreen(
                    keyManager = keyManager,
                    onDone = { navController.popBackStack() },
                )
            }
            composable("keys/import") {
                ImportKeyScreen(
                    keyManager = keyManager,
                    title = "Import Private Key",
                    onDone = { navController.popBackStack() },
                )
            }
            composable("recipients") {
                RecipientListScreen(
                    recipientManager = recipientManager,
                    onImportClick = { navController.navigate("recipients/import") },
                )
            }
            composable("recipients/import") {
                ImportRecipientScreen(
                    recipientManager = recipientManager,
                    onDone = { navController.popBackStack() },
                )
            }
            composable("about") {
                AboutScreen()
            }
            composable("help") {
                HelpScreen(onElementClick = { entry -> navController.navigate("help/${entry.id}") })
            }
            composable("help/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val entry = allHelpEntries.find { it.id == id }
                if (entry != null) {
                    HelpDetailScreen(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun getTitle(route: String?): String {
    return when {
        route == null -> "CipherKeys Settings"
        route.startsWith("help/") -> "Help"
        route == "help" -> "Help"
        route.startsWith("keys/generate") -> "Generate Key"
        route.startsWith("keys/import") -> "Import Key"
        route.startsWith("keys") -> "Keys"
        route.startsWith("recipients/import") -> "Add Recipient"
        route.startsWith("recipients") -> "Recipients"
        route == "about" -> "About"
        else -> "CipherKeys Settings"
    }
}

@Composable
fun AboutScreen() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("CK", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(16.dp))
        Text("CipherKeys", style = MaterialTheme.typography.headlineMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        Text("PGP encryption keyboard for Android", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
        Text("Version 0.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}
