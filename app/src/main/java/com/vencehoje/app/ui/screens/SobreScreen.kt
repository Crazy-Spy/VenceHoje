package com.vencehoje.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share // Substituto para o ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SobreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val chavePix = "c16cba4e-ce2b-4a63-b553-75dbdccb3188"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre o App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar"
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "VenceHoje",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Este app nasceu de uma frustração pessoal: eu só queria um lugar simples para não esquecer meus boletos, mas só encontrava apps financeiros complexos, cheios de propaganda e que pediam acesso a tudo.",
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "O foco aqui é a SIMPLICIDADE. Sem anúncios, sem mensalidades e 100% offline. Seus dados são seus e de mais ninguém.",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Gostou da iniciativa?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Incentive o desenvolvimento enviando um Pix de qualquer valor.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(chavePix))
                            Toast.makeText(context, "Chave Pix copiada!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // TROCADO: Usando o ícone Share que está no pacote core
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copiar Chave Pix")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "VenceHoje: Porque ninguém merece perder dinheiro com esquecimento.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                textAlign = TextAlign.Center
            )
        }
    }
}