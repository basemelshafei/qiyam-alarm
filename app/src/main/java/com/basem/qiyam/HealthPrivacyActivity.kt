package com.basem.qiyam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Night
import com.basem.qiyam.ui.QiyamTheme

/** Shown by Health Connect when you ask why Istiqamah wants your health data. */
class HealthPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QiyamTheme {
                Box(Modifier.fillMaxSize().background(Night.Ink).statusBarsPadding()) {
                    Page("Your health data") {
                        Card {
                            Text(
                                "Istiqamah reads your steps, sleep, heart rate, weight and workouts so your Body ring " +
                                    "and gym sessions fill in by themselves.",
                                color = Night.Text, fontSize = 16.sp,
                            )
                            Text(
                                "It only reads. Nothing is written back, and nothing leaves your phone: there is no account " +
                                    "and no server. You can turn access off any time in Health Connect.",
                                color = Night.Muted, fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
