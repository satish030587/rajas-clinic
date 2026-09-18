package com.rajashomoeocare.clinic

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.OutboxStore
import com.rajashomoeocare.clinic.data.OutboxSync
import com.rajashomoeocare.clinic.data.SessionStore
import com.rajashomoeocare.clinic.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClinicApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Anything written while the network was down goes out on next launch.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.repository.drainOutbox()
        }
    }

    override fun newImageLoader(): ImageLoader = container.imageLoader
}

class AppContainer(app: Application) {
    val session = SessionStore(app)

    private val httpClient = ApiClient.httpClient(session)
    val api = ApiClient.create(httpClient)
    val imageLoader = ApiClient.imageLoader(app, httpClient)

    private val outbox = OutboxStore(app)
    val repository = ClinicRepository(api, outbox, OutboxSync(api, outbox))
}
