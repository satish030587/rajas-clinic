package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Small helper so each screen can build its ViewModel from the AppContainer. */
inline fun <reified VM : ViewModel> factoryOf(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create() }
}
