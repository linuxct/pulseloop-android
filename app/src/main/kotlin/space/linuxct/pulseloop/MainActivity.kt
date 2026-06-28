package space.linuxct.pulseloop

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.ui.navigation.PulseNavGraph
import space.linuxct.pulseloop.ui.theme.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPrefs: AppPreferencesDataStore

    private var navController: NavController? = null
    private var currentUseMaterialYou: Boolean = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            appPrefs.useMaterialYou.collect { currentUseMaterialYou = it }
        }

        setContent {
            val useMaterialYou by appPrefs.useMaterialYou.collectAsState(initial = false)
            AppTheme(useMaterialYou = useMaterialYou) {
                val navCtrl = rememberNavController()
                navController = navCtrl
                PulseNavGraph(navController = navCtrl)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        applyLauncherIcon(currentUseMaterialYou)
    }

    private fun applyLauncherIcon(enabled: Boolean) {
        val pm = packageManager
        val pkg = packageName
        pm.setComponentEnabledSetting(
            ComponentName(pkg, "$pkg.MainActivityIconV1"),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            ComponentName(pkg, "$pkg.MainActivityIconV2"),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Only forward actual deep-link intents; plain notification-tap intents have no
        // nav data and calling handleDeepLink on them causes a NPE inside the nav library.
        if (intent.data != null) {
            navController?.handleDeepLink(intent)
        }
    }
}
