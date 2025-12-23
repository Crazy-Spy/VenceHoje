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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
    // ... (O código do onCreate continua igual, não precisa mexer)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = BillRepository(
            database.billDao(),
            database.categoryDao(),
            database.profileDao()
        )

        val prefs = getSharedPreferences("vencehoje_prefs", Context.MODE_PRIVATE)
        val savedProfileId = prefs.getInt("current_profile_id", 1)

        setupInitialWorker(this)

        setContent {
            VenceHojeTheme {
                NotificationPermissionHandler()

                var currentProfileId by remember { mutableIntStateOf(savedProfileId) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(currentProfileId) {
                    repository.checkAndSeedCategories(currentProfileId)
                }

                val onProfileChange: (Int) -> Unit = { newId ->
                    currentProfileId = newId
                    prefs.edit().putInt("current_profile_id", newId).apply()
                }

                MainScreen(
                    repository = repository,
                    currentProfileId = currentProfileId,
                    onProfileChange = onProfileChange
                )
            }
        }
    }

    // ... (Funções checkForUpdates, onResume, setupInitialWorker continuam iguais)
    private fun checkForUpdates() { /* ... */ }
    override fun onResume() { super.onResume(); /* ... */ }
    private fun setupInitialWorker(context: Context) { /* ... */ }
}

@Composable
fun NotificationPermissionHandler() {
    // ... (igual ao anterior)
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
fun MainScreen(
    repository: BillRepository,
    currentProfileId: Int,
    onProfileChange: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentScreen by remember { mutableStateOf("home") }

    // Coleta os perfis para mostrar no seletor do Drawer
    val profiles by repository.allProfiles.collectAsState(initial = emptyList())

    // Estado para controlar o menuzinho de troca no Drawer
    var isDrawerProfileMenuExpanded by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importFromCSV(context, repository, scope, it, currentProfileId) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val bills = repository.getBillsByProfile(currentProfileId).first()
                saveCsvToUri(context, it, bills)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // CABEÇALHO VERDE (AGORA INTERATIVO!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B5E20))
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                ) {
                    Column {
                        Text(
                            "VenceHoje",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- SELETOR DE PERFIL NO DRAWER ---
                        val activeProfile = profiles.find { it.id == currentProfileId }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                // CIÊNCIA PURA: Tornamos a linha inteira clicável
                                .clickable { isDrawerProfileMenuExpanded = true }
                                .padding(4.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            try { Color(android.graphics.Color.parseColor(activeProfile?.colorHex ?: "#1976D2")) }
                                            catch (e: Exception) { Color.Gray }
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Perfil Ativo:",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        activeProfile?.name ?: "Principal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Setinha indicando que abre um menu
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            // --- O DROPDOWN SIMPLIFICADO (SÓ LISTA) ---
                            DropdownMenu(
                                expanded = isDrawerProfileMenuExpanded,
                                onDismissRequest = { isDrawerProfileMenuExpanded = false }
                            ) {
                                profiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text(profile.name, fontWeight = if(profile.id == currentProfileId) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            onProfileChange(profile.id) // Troca o perfil
                                            isDrawerProfileMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        try { Color(android.graphics.Color.parseColor(profile.colorHex)) }
                                                        catch (e: Exception) { Color.Gray },
                                                        CircleShape
                                                    )
                                            )
                                        },
                                        trailingIcon = {
                                            if (profile.id == currentProfileId) {
                                                Icon(Icons.Default.Check, null, tint = Color(0xFF1B5E20))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // ITENS DE NAVEGAÇÃO
                NavigationDrawerItem(
                    label = { Text("Minhas Contas") },
                    selected = currentScreen == "home",
                    onClick = {
                        if (currentScreen != "home") currentScreen = "home"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Categorias") },
                    selected = currentScreen == "categories",
                    onClick = { currentScreen = "categories"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.List, null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = currentScreen == "charts",
                    onClick = { currentScreen = "charts"; scope.launch { drawerState.close() } },
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_piechart_vencehoje), contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // O botão de Gerenciar Completo fica aqui embaixo, separado
                NavigationDrawerItem(
                    label = { Text("Gerenciar Perfis") },
                    selected = currentScreen == "profiles",
                    onClick = { currentScreen = "profiles"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))

                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    selected = currentScreen == "configs",
                    onClick = { currentScreen = "configs"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Sobre o App") },
                    selected = currentScreen == "about",
                    onClick = { currentScreen = "about"; scope.launch { drawerState.close() } },
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_info_vencehoje), contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Reportar Bug/Sugestão") },
                    selected = currentScreen == "report",
                    onClick = { currentScreen = "report"; scope.launch { drawerState.close() } },
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_bug_vencehoje), contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        // NAVEGAÇÃO DE TELAS
        when(currentScreen) {
            "home" -> BillsListScreen(
                repository = repository,
                profileId = currentProfileId,
                onMenuClick = { scope.launch { drawerState.open() } },
                onProfileChange = onProfileChange,
                onManageProfiles = { currentScreen = "profiles" }
            )
            "charts" -> DashboardScreen(
                repository = repository,
                profileId = currentProfileId,
                onBack = { currentScreen = "home" },
                onProfileChange = onProfileChange
            )
            "categories" -> ManageCategoriesScreen(
                repository = repository,
                profileId = currentProfileId,
                onBack = { currentScreen = "home" },
                onProfileChange = onProfileChange
            )
            "profiles" -> ManageProfilesScreen(
                repository = repository,
                currentProfileId = currentProfileId,
                onBack = { currentScreen = "home" }
            )
            "configs" -> SettingsScreen(
                repository = repository,
                onBack = { currentScreen = "home" },
                onExport = {
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