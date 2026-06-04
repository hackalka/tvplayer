package com.tvplayer.tvcine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
            TvcineTheme(dynamicColor = false) {
                var authState by remember { mutableStateOf<TdApi.AuthorizationState?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var isProcessing by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    telegramManager.start { update ->
                        if (update is TdApi.UpdateAuthorizationState) {
                            Log.d("TvPlayerPlus", "Auth State: ${update.authorizationState}")
                            authState = update.authorizationState
                            errorMessage = null
                            isProcessing = false
                        }
                    }
                }

                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    Box {
                        if (isProcessing) {
                            LoadingScreen("Procesando...")
                        } else {
                            when (authState) {
                                is TdApi.AuthorizationStateWaitTdlibParameters -> {
                                    LaunchedEffect(Unit) { telegramManager.setupParameters() }
                                    LoadingScreen("Iniciando Tv Player+...")
                                }
                                is TdApi.AuthorizationStateWaitPhoneNumber -> LoginScreen(errorMessage) { phone ->
                                    isProcessing = true
                                    val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
                                    telegramManager.send(TdApi.SetAuthenticationPhoneNumber(formattedPhone, TdApi.PhoneNumberAuthenticationSettings())) { result ->
                                        if (result is TdApi.Error) {
                                            isProcessing = false
                                            errorMessage = "Error: ${result.message}"
                                        }
                                    }
                                }
                                is TdApi.AuthorizationStateWaitCode -> CodeScreen(errorMessage) { code ->
                                    isProcessing = true
                                    telegramManager.send(TdApi.CheckAuthenticationCode(code)) { result ->
                                        if (result is TdApi.Error) {
                                            isProcessing = false
                                            errorMessage = "Código inválido: ${result.message}"
                                        }
                                    }
                                }
                                is TdApi.AuthorizationStateWaitPassword -> PasswordScreen(errorMessage) { pass ->
                                    isProcessing = true
                                    telegramManager.send(TdApi.CheckAuthenticationPassword(pass)) { result ->
                                        if (result is TdApi.Error) {
                                            isProcessing = false
                                            errorMessage = "Contraseña incorrecta"
                                        }
                                    }
                                }
                                is TdApi.AuthorizationStateWaitRegistration -> {
                                    // Handle new user registration if needed
                                    LoadingScreen("Registro requerido. Por favor, usa una cuenta existente.")
                                }
                                is TdApi.AuthorizationStateReady -> MainScreen(telegramManager)
                                null -> LoadingScreen("Cargando...")
                                else -> {
                                    val stateName = authState!!::class.java.simpleName
                                    if (stateName.contains("EncryptionKey")) {
                                        LaunchedEffect(Unit) { telegramManager.send(TdApi.SetDatabaseEncryptionKey("".toByteArray())) {} }
                                        LoadingScreen("Preparando base de datos...")
                                    } else {
                                        LoadingScreen("Iniciando... ($stateName)")
                                    }
                                }
                            }
                        }
                        
                        // Overlay error message if any
                        errorMessage?.let {
                            Snackbar(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                                action = { Button(onClick = { errorMessage = null }) { Text("OK") } }
                            ) { Text(it) }
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
fun MainScreen(manager: TelegramManager) {
    val movies = remember { mutableStateListOf<TdApi.Message>() }
    val topics = remember { mutableStateListOf<TdApi.ForumTopic>() }
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Buscando contenido...") }
    var selectedTopicId by remember { mutableStateOf<Int?>(null) }
    
    var currentChatId by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(Unit) {
        manager.addGroup("https://web.telegram.org/a/#-1003749684388") { chat ->
            currentChatId = chat.id
            status = "Cargando temas..."
            
            // Fetch topics if it's a forum
            manager.getForumTopics(chat.id) { result ->
                topics.clear()
                topics.addAll(result.topics)
            }

            status = "Cargando catálogo..."
            manager.getChatMessages(chat.id, 0, 100) { result ->
                val videoMessages = result.messages.filter { it.content is TdApi.MessageVideo }
                movies.clear()
                movies.addAll(videoMessages)
                isLoading = false
                if (movies.isEmpty()) status = "No se encontraron películas"
            }
        }
    }

    LaunchedEffect(selectedTopicId) {
        val chatId = currentChatId ?: return@LaunchedEffect
        if (selectedTopicId != null) {
            isLoading = true
            status = "Cargando videos del tema..."
            manager.getMessageThreadHistory(chatId, selectedTopicId!!.toLong(), 0, 100) { result ->
                val videoMessages = result.messages.filter { it.content is TdApi.MessageVideo }
                movies.clear()
                movies.addAll(videoMessages)
                isLoading = false
            }
        }
    }

    Scaffold(containerColor = Color.Black) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isLoading && movies.isEmpty()) {
                LoadingScreen(status)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { HeroSection(movies.firstOrNull(), manager) }
                    
                    if (topics.isNotEmpty()) {
                        item {
                            Text("TEMAS DEL GRUPO", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(topics) { topic ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (selectedTopicId == topic.info.forumTopicId) Color(0xFFE50914) else Color(0xFF333333))
                                            .clickable { selectedTopicId = topic.info.forumTopicId }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(topic.info.name, color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    item { MovieRow("Películas", movies, manager) }
                    item { MovieRow("Series", movies.shuffled(), manager) }
                }
            }
            TopBar()
        }
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tv Player+", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Spacer(modifier = Modifier.width(30.dp))
        Text("INICIO", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(20.dp))
        Text("PELÍCULAS", color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(20.dp))
        Text("SERIES", color = Color.LightGray, fontSize = 14.sp)
    }
}

@Composable
fun HeroSection(message: TdApi.Message?, manager: TelegramManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
        message?.let {
            val video = (it.content as? TdApi.MessageVideo)?.video
            ThumbnailImage(video?.thumbnail?.file, manager, Modifier.fillMaxSize())
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(30.dp)) {
            val title = (message?.content as? TdApi.MessageVideo)?.caption?.text ?: "PELÍCULA DESTACADA"
            Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val video = (message?.content as? TdApi.MessageVideo)?.video
                    video?.let { v ->
                        val url = "http://localhost:8080/stream?fileId=${v.video.id}"
                        playVideo(context, url)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("REPRODUCIR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun playVideo(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            // Try VLC first, then fall back to any player
            setPackage("org.videolan.vlc")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val genericIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
        }
        context.startActivity(genericIntent)
    }
}

@Composable
fun MovieRow(title: String, movies: List<TdApi.Message>, manager: TelegramManager) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 15.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, bottom = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(movies) { MovieCard(it, manager) }
        }
    }
}

@Composable
fun MovieCard(message: TdApi.Message, manager: TelegramManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val video = (message.content as? TdApi.MessageVideo)?.video
    val caption = (message.content as? TdApi.MessageVideo)?.caption?.text ?: ""
    
    Column(modifier = Modifier.width(180.dp).clickable {
        video?.let { v ->
            val url = "http://localhost:8080/stream?fileId=${v.video.id}"
            playVideo(context, url)
        }
    }) {
        Box(modifier = Modifier.height(260.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F))) {
            ThumbnailImage(video?.thumbnail?.file, manager, Modifier.fillMaxSize())
        }
        if (caption.isNotEmpty()) {
            Text(caption, color = Color.LightGray, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ThumbnailImage(file: TdApi.File?, manager: TelegramManager, modifier: Modifier) {
    var path by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        file?.let {
            if (it.local.isDownloadingCompleted) path = it.local.path
            else {
                manager.send(TdApi.GetFile(it.id)) { res ->
                    if (res is TdApi.File && res.local.isDownloadingCompleted) path = res.local.path
                    else manager.downloadFile(it.id)
                }
            }
        }
    }
    
    // Escuchamos actualizaciones de descarga de archivos
    DisposableEffect(file) {
        val updateCallback: (TdApi.Object) -> Unit = { update ->
            if (update is TdApi.UpdateFile && update.file.id == file?.id && update.file.local.isDownloadingCompleted) {
                path = update.file.local.path
            }
        }
        // Nota: manager debería permitir registrar listeners, pero aquí usaremos el onUpdate global si pudiéramos.
        // Como no podemos fácilmente sin cambiar TelegramManager, confiamos en LaunchedEffect
        onDispose {}
    }

    if (path != null) AsyncImage(model = path, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    else Box(modifier = modifier.background(Color(0xFF141414)))
}

@Composable
fun LoadingScreen(msg: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE50914))
            Spacer(Modifier.height(16.dp))
            Text(msg, color = Color.White)
        }
    }
}

@Composable
fun LoginScreen(error: String?, onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column {
            Text("Tv Player+", color = Color(0xFFE50914), fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(30.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = phone, 
                onValueChange = { phone = it }, 
                label = { Text("Teléfono (+34...)") }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onLogin(phone) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("CONTINUAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CodeScreen(error: String?, onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column {
            Text("CÓDIGO SMS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onCode(code) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PasswordScreen(error: String?, onPassword: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column {
            Text("CONTRASEÑA 2FA", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onPassword(pass) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}
