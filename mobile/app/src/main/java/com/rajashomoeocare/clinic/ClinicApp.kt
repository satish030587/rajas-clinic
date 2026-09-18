package com.rajashomoeocare.clinic

import android.app.Application
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.SessionStore
import com.rajashomoeocare.clinic.data.remote.ApiClient

class ClinicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val session = SessionStore(app)
    val api = ApiClient.create(session)
    val repository = ClinicRepository(api)
}
