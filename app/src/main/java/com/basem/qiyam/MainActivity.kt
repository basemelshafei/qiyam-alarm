package com.basem.qiyam

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Night
import com.basem.qiyam.ui.QiyamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scheduler.createChannels(this)
        Scheduler.scheduleNext(this)
        setContent { QiyamTheme { AppShell() } }
    }
}

enum class Tab(val label: String, val icon: String) {
    TODAY("Today", "☀️"),
    DEEN("Deen", "🕌"),
    BODY("Body", "💪"),
    CAREER("Career", "🚀"),
    WEALTH("Wealth", "📈"),
}

@Composable
private fun AppShell() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var qiyamPage by rememberSaveable { mutableStateOf(false) }

    // Prayer times need your location: ask once, on first launch.
    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) fetchLocation(ctx, prefs) { Scheduler.scheduleNext(ctx) }
    }
    LaunchedEffect(Unit) {
        if (!prefs.hasLocation) locationPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    BackHandler(enabled = qiyamPage || tab != Tab.TODAY) {
        if (qiyamPage) qiyamPage = false else tab = Tab.TODAY
    }

    if (qiyamPage) {
        QiyamScreen(onBack = { qiyamPage = false })
        return
    }

    val content: @Composable () -> Unit = {
        when (tab) {
            Tab.TODAY -> TodayScreen(onOpen = { tab = it })
            Tab.DEEN -> DeenScreen(onOpenQiyam = { qiyamPage = true })
            Tab.BODY -> BodyScreen()
            Tab.CAREER -> ComingSoon(
                Tab.CAREER,
                listOf(
                    "AWS certification: exam countdown, study hours, practice-test scores",
                    "Portfolio projects with milestones",
                    "US job pipeline: applied → interview → offer",
                ),
            )
            Tab.WEALTH -> ComingSoon(
                Tab.WEALTH,
                listOf(
                    "Investment log: buys, sells, realised profit and loss",
                    "Side-hustle income and costs against a monthly target",
                    "Charts of money invested and money made",
                ),
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Night.sky)) {
        if (maxWidth >= 600.dp) {
            // Unfolded: a rail on the left, the page beside it.
            Row(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(
                    Modifier.width(92.dp).fillMaxHeight().padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { Tab.entries.forEach { TabButton(it, it == tab) { tab = it } } }
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                HorizontalDivider(color = Night.CardEdge)
                Row(
                    Modifier.fillMaxWidth().background(Night.Ink).navigationBarsPadding().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) { Tab.entries.forEach { TabButton(it, it == tab) { tab = it } } }
            }
        }
    }
}

@Composable
private fun TabButton(t: Tab, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.testTag("tab_${t.name.lowercase()}")
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(t.icon, fontSize = if (selected) 22.sp else 19.sp)
        Text(t.label, color = if (selected) Night.Gold else Night.Muted, fontSize = 11.sp)
    }
}
