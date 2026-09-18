package com.rajashomoeocare.clinic.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rajashomoeocare.clinic.AppContainer
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.ui.screens.LockScreen
import com.rajashomoeocare.clinic.ui.screens.PatientDetailScreen
import com.rajashomoeocare.clinic.ui.screens.PatientFormScreen
import com.rajashomoeocare.clinic.ui.screens.PatientsScreen
import com.rajashomoeocare.clinic.ui.screens.PhotoCompareScreen
import com.rajashomoeocare.clinic.ui.screens.RecallScreen
import com.rajashomoeocare.clinic.ui.screens.SettingsScreen
import com.rajashomoeocare.clinic.ui.screens.TodayScreen
import com.rajashomoeocare.clinic.ui.screens.VisitEditorScreen
import com.rajashomoeocare.clinic.ui.vm.HomeViewModel
import com.rajashomoeocare.clinic.ui.vm.LockViewModel
import com.rajashomoeocare.clinic.ui.vm.PatientDetailViewModel
import com.rajashomoeocare.clinic.ui.vm.PatientFormViewModel
import com.rajashomoeocare.clinic.ui.vm.PatientsViewModel
import com.rajashomoeocare.clinic.ui.vm.SettingsViewModel
import com.rajashomoeocare.clinic.ui.vm.VisitEditorViewModel
import com.rajashomoeocare.clinic.ui.vm.factoryOf

private object Routes {
    const val TODAY = "today"
    const val RECALL = "recall"
    const val PATIENTS = "patients"
    const val SETTINGS = "settings"
    const val PATIENT_DETAIL = "patient/{patientId}"
    const val PATIENT_FORM = "patientForm?patientId={patientId}"
    const val VISIT = "visit/{patientId}?visitId={visitId}"
    const val COMPARE = "compare/{patientId}"

    fun patientDetail(id: String) = "patient/$id"
    fun patientForm(id: String? = null) = "patientForm?patientId=${id.orEmpty()}"
    fun visit(patientId: String, visitId: String? = null) =
        "visit/$patientId?visitId=${visitId.orEmpty()}"

    fun compare(patientId: String) = "compare/$patientId"
}

private data class Tab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val TABS = listOf(
    Tab(Routes.TODAY, R.string.nav_today, Icons.Filled.Today, Icons.Outlined.Today),
    Tab(
        Routes.RECALL,
        R.string.nav_recall,
        Icons.Filled.NotificationsActive,
        Icons.Outlined.NotificationsActive,
    ),
    Tab(Routes.PATIENTS, R.string.nav_patients, Icons.Filled.Group, Icons.Outlined.Group),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun ClinicRoot(container: AppContainer, activity: FragmentActivity) {
    var unlocked by remember { mutableStateOf(false) }

    if (!unlocked) {
        val lockViewModel: LockViewModel = viewModel(
            factory = factoryOf { LockViewModel(container.settings) },
        )
        LockScreen(
            viewModel = lockViewModel,
            activity = activity,
            onUnlocked = { unlocked = true },
        )
        return
    }

    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(
        factory = factoryOf { HomeViewModel(container.repository) },
    )
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            ClinicBottomBar(
                navController = navController,
                overdueCount = homeState.overdueCount,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    viewModel = homeViewModel,
                    onPatientClick = { navController.navigate(Routes.patientDetail(it)) },
                    onAddPatient = { navController.navigate(Routes.patientForm()) },
                )
            }

            composable(Routes.RECALL) {
                RecallScreen(
                    viewModel = homeViewModel,
                    onPatientClick = { navController.navigate(Routes.patientDetail(it)) },
                )
            }

            composable(Routes.PATIENTS) {
                val vm: PatientsViewModel = viewModel(
                    factory = factoryOf { PatientsViewModel(container.repository) },
                )
                PatientsScreen(
                    viewModel = vm,
                    onPatientClick = { navController.navigate(Routes.patientDetail(it)) },
                    onAddPatient = { navController.navigate(Routes.patientForm()) },
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = factoryOf {
                        SettingsViewModel(container.repository, container.settings)
                    },
                )
                SettingsScreen(viewModel = vm)
            }

            composable(Routes.PATIENT_DETAIL) { entry ->
                val patientId = entry.arguments?.getString("patientId").orEmpty()
                val vm: PatientDetailViewModel = viewModel(
                    factory = factoryOf {
                        PatientDetailViewModel(container.repository, patientId)
                    },
                )
                PatientDetailScreen(
                    viewModel = vm,
                    onBack = navController::popBackStack,
                    onEdit = { navController.navigate(Routes.patientForm(patientId)) },
                    onNewVisit = { navController.navigate(Routes.visit(patientId)) },
                    onEditVisit = { navController.navigate(Routes.visit(patientId, it)) },
                    onCompare = { navController.navigate(Routes.compare(patientId)) },
                )
            }

            composable(Routes.PATIENT_FORM) { entry ->
                val patientId = entry.arguments?.getString("patientId")?.takeIf(String::isNotEmpty)
                val vm: PatientFormViewModel = viewModel(
                    factory = factoryOf {
                        PatientFormViewModel(container.repository, patientId)
                    },
                )
                PatientFormScreen(
                    viewModel = vm,
                    onBack = navController::popBackStack,
                    onSaved = { savedId ->
                        if (patientId == null) {
                            navController.navigate(Routes.patientDetail(savedId)) {
                                popUpTo(Routes.PATIENT_FORM) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

            composable(Routes.VISIT) { entry ->
                val patientId = entry.arguments?.getString("patientId").orEmpty()
                val visitId = entry.arguments?.getString("visitId")?.takeIf(String::isNotEmpty)
                val vm: VisitEditorViewModel = viewModel(
                    factory = factoryOf {
                        VisitEditorViewModel(
                            repo = container.repository,
                            settings = container.settings,
                            patientId = patientId,
                            existingVisitId = visitId,
                        )
                    },
                )
                VisitEditorScreen(
                    viewModel = vm,
                    onBack = navController::popBackStack,
                    onSaved = { navController.popBackStack() },
                    onCompare = { navController.navigate(Routes.compare(patientId)) },
                )
            }

            composable(Routes.COMPARE) { entry ->
                val patientId = entry.arguments?.getString("patientId").orEmpty()
                val vm: PatientDetailViewModel = viewModel(
                    factory = factoryOf {
                        PatientDetailViewModel(container.repository, patientId)
                    },
                )
                PhotoCompareScreen(viewModel = vm, onBack = navController::popBackStack)
            }
        }
    }
}

@Composable
private fun ClinicBottomBar(navController: NavHostController, overdueCount: Int) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The bar is only meaningful on the four top-level tabs.
    val onTab = TABS.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }
    if (!onTab) return

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        TABS.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (tab.route == Routes.RECALL && overdueCount > 0) {
                        BadgedBox(badge = { Badge { Text(overdueCount.toString()) } }) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = null,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                        )
                    }
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
