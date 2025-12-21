package com.vencehoje.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.vencehoje.app.data.AppDatabase
import com.vencehoje.app.data.BillRepository
import kotlinx.coroutines.launch
import com.vencehoje.app.ui.screens.*
import com.vencehoje.app.ui.theme.VenceHojeTheme
import com.vencehoje.app.logic.saveCsvToUri
import com.vencehoje.app.logic.importFromCSV
import kotlinx.coroutines.flow.first


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val repository = BillRepository(database.billDao())
        setupInitialWorker(this)
        setContent {
            VenceHojeTheme {
                NotificationPermissionHandler()
                MainScreen(repository)
            }
        }
    }

    private fun checkForUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            // Se houver uma atualização disponível e for permitida a atualização imediata
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    999 // Um código qualquer para identificar o retorno
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(info, AppUpdateType.IMMEDIATE, this, 999)
            }
        }
    }
    private fun setupInitialWorker(context: Context) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork("vencehoje_loop", ExistingWorkPolicy.KEEP, request)
    }
}

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: BillRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentScreen by remember { mutableStateOf("home") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importFromCSV(context, repository, scope, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val bills = repository.allBills.first()
                saveCsvToUri(context, it, bills)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "VenceHoje",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color(0xFF1B5E20)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Minhas Contas") },
                    selected = currentScreen == "home",
                    onClick = {
                        if (currentScreen != "home") currentScreen = "home"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = currentScreen == "charts",
                    onClick = { currentScreen = "charts"; scope.launch { drawerState.close() } },
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_piechart_vencehoje), contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    selected = currentScreen == "configs",
                    onClick = { currentScreen = "configs"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Sobre o App") },
                    selected = currentScreen == "about",
                    onClick = { currentScreen = "about"; scope.launch { drawerState.close() } },
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_info_vencehoje), contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Reportar Bug/Sugestão") },
                    selected = currentScreen == "report",
                    onClick = { currentScreen = "report"; scope.launch { drawerState.close() } },
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_bug_vencehoje), contentDescription = null) }
                )
            }
        }
    ) {
        when(currentScreen) {
            "home" -> BillsListScreen(repository, onMenuClick = { scope.launch { drawerState.open() } })
            "charts" -> DashboardScreen(repository, onBack = { currentScreen = "home" })

            "configs" -> SettingsScreen(
                repository = repository,
                onBack = { currentScreen = "home" },
                onExport = {
                    // GERANDO O NOME: backup_vencehoje_20231221_1645.csv
                    val fileName = "backup_vencehoje_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}.csv"
                    exportLauncher.launch(fileName)
                },
                onImport = { importLauncher.launch("*/*") }
            )

            "about" -> SobreScreen(onBack = { currentScreen = "home" })
            "report" -> ReportarScreen(onBack = { currentScreen = "home" })
        }
    }
}