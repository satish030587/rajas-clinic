package com.rajashomoeocare.clinic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.rajashomoeocare.clinic.ui.ClinicRoot
import com.rajashomoeocare.clinic.ui.theme.ClinicTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as ClinicApp).container
        setContent {
            ClinicTheme {
                ClinicRoot(container = container)
            }
        }
    }
}
