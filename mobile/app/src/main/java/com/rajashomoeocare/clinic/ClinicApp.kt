package com.rajashomoeocare.clinic

import android.app.Application
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.SettingsStore
import com.rajashomoeocare.clinic.data.local.ClinicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClinicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.repository.ensureTemplatesSeeded()
        }
    }
}

class AppContainer(app: Application) {
    private val database = ClinicDatabase.build(app)
    val repository = ClinicRepository(database)
    val settings = SettingsStore(app)
}
