package com.tvplayer.tvcine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tvplayer.tvcine.ui.theme.TvcineTheme
import org.drinkless.tdlib.TdApi

class MainActivity : ComponentActivity() {
    private lateinit var telegramManager: TelegramManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        telegramManager = TelegramManager(this)
        TelegramProxyService.telegramManager = telegramManager
        startService(Intent(this, TelegramProxyService::class.java))
        
        enableEdgeToEdge()
        setContent {
            // Forzamos el tema oscuro de Tvcine eliminando dynamicColor para consistencia
            TvcineTheme(dynamicColor = false) {
                var authState by remember { mutableStateOf<TdApi.AuthorizationState?>(null) }
                
                LaunchedEffect(Unit) {
                    telegramManager.start { update ->
                        if (update is TdApi.UpdateAuthorizationState) {
                            authState = update.authorizationState
                        }
                    }
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    when (authState) {
                        is TdApi.AuthorizationStateWaitTdlibParameters -> {
                            LaunchedEffect(Unit) { telegramManager.setupParameters() }
                            LoadingScreen("Configurando...")
                        }
                        is TdApi.AuthorizationStateWaitPhoneNumber -> {
                            LoginScreen { phone -> telegramManager.setAuthenticationPhoneNumber(phone) }
                        }
                        is TdApi.AuthorizationStateWaitCode -> {
                            CodeScreen { code -> telegramManager.checkAuthenticationCode(code) }
                        }
                        is TdApi.AuthorizationStateWaitPassword -> {
                            PasswordScreen { password -> telegramManager.checkAuthenticationPassword(password) }
                        }
                        is TdApi.AuthorizationStateReady -> {
                            MainScreen(telegramManager)
                        }
                        else -> {
                            LoadingScreen("Iniciando Telegram...")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        telegramManager.stop()
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF50A8EB))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ingresa tu número", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono (+34...)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onLogin(phone) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF50A8EB))
            ) {
                Text("Continuar")
            }
        }
    }
}

@Composable
fun CodeScreen(onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Código de Verificación", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onCode(code) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF50A8EB))
            ) {
                Text("Verificar")
            }
        }
    }
}

@Composable
fun PasswordScreen(onPassword: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Contraseña 2FA", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tu cuenta tiene activada la verificación en dos pasos.", color = Color.LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onPassword(password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF50A8EB))
            ) {
                Text("Confirmar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(manager: TelegramManager) {
    val chats = remember { mutableStateListOf<TdApi.Chat>() }
    var showAddGroup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        manager.getChats { result ->
            for (chatId in result.chatIds) {
                manager.getChat(chatId) { chat ->
                    chats.add(chat)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TV Cine Telegram") },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF242F3D),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGroup = true },
                containerColor = Color(0xFF50A8EB),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Group")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            // Ordenar por la posición en la lista principal (order descendente)
            val sortedChats = chats.sortedByDescending { chat: TdApi.Chat -> 
                chat.positions.firstOrNull()?.order ?: 0L 
            }
            
            LazyColumn {
                items(sortedChats) { chat ->
                    ChannelItem(chat, manager) { fileId ->
                        val videoUrl = "http://localhost:8080/stream?fileId=$fileId"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(videoUrl), "video/*")
                            setPackage("org.videolan.vlc")
                        }
                        context.startActivity(intent)
                    }
                    HorizontalDivider(color = Color(0xFF242F3D), thickness = 0.5.dp)
                }
            }
        }

        if (showAddGroup) {
            AddGroupDialog(
                onDismiss = { showAddGroup = false },
                onAdd = { query ->
                    manager.addGroup(query) { newChat ->
                        if (!chats.any { it.id == newChat.id }) {
                            chats.add(0, newChat)
                        }
                        showAddGroup = false
                    }
                }
            )
        }
    }
}

@Composable
fun AddGroupDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Grupo o Canal") },
        text = {
            Column {
                Text("Introduce el enlace o @username")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("https://t.me/... o @usuario") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ChannelItem(chat: TdApi.Chat, manager: TelegramManager, onVideoClick: (Int) -> Unit) {
    var photoPath by remember { mutableStateOf<String?>(null) }
    
    // Intentar obtener la foto
    LaunchedEffect(chat.photo) {
        chat.photo?.small?.let { file ->
            if (file.local.isDownloadingCompleted) {
                photoPath = file.local.path
            } else {
                manager.downloadFile(file.id)
            }
        }
    }

    // Escuchar actualizaciones de archivos para la foto (esto es simplificado)
    // En una app real, usaríamos un Flow de actualizaciones de TDLib

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            color = Color(0xFF50A8EB)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (photoPath != null) {
                    AsyncImage(
                        model = photoPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = chat.title.take(1),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            // Botón para reproducir si es un canal/grupo con video
            // Aquí idealmente buscaríamos el último mensaje con video
            Button(onClick = { 
                // ID de prueba o buscar el video del chat
                // onVideoClick(videoFileId) 
            }) {
                Text("Reproducir en VLC")
            }
        }
    }
}
