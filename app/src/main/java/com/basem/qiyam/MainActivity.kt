package com.basem.qiyam

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.basem.qiyam.ui.QiyamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Secrets.migrate(this)
        Scheduler.createChannels(this)
        Scheduler.scheduleNext(this)
        setContent {
            QiyamTheme {
                val ctx = LocalContext.current
                val prefs = remember { Prefs(ctx) }
                // The night's times need your location: ask once, on first launch.
                val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                    if (ok) fetchLocation(ctx, prefs) { Scheduler.scheduleNext(ctx) }
                }
                LaunchedEffect(Unit) {
                    if (!prefs.hasLocation) locationPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                QiyamScreen(onBack = null)
            }
        }
    }
}
