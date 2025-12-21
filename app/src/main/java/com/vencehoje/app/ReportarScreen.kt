package com.vencehoje.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val urlIssues = "https://github.com/Crazy-Spy/VenceHoje/issues"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar Bug / Sugestão") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Adicionado para telas menores
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFB71C1C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bora melhorar o VenceHoje?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Se você achou um bug ou tem uma ideia matadora, o melhor lugar para me avisar é no GitHub.",
                textAlign = TextAlign.Center,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QUADRO DE INSTRUÇÕES (O MINI-TUTORIAL)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Passo a passo:",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Acesse o link pelo botão abaixo.", fontSize = 13.sp)
                    Text("2. Faça login (é necessário ter conta no GitHub).", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("3. Clique no botão verde 'New Issue'.", fontSize = 13.sp)
                    Text("4. Dê um título, descreva o que houve e salve.", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlIssues))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ir para o GitHub Issues")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Obrigado por ajudar a manter o app simples e sem bugs!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = Color(0xFF1B5E20)
            )
        }
    }
}