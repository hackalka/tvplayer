package com.tvplayer.tvcine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
                LaunchedEffect(Unit) {
                    telegramManager.start { update ->
                        if (update is TdApi.UpdateAuthorizationState) authState = update.authorizationState
                    }
                }

                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    when (authState) {
                        is TdApi.AuthorizationStateWaitTdlibParameters -> {
                            LaunchedEffect(Unit) { telegramManager.setupParameters() }
                            LoadingScreen("Configurando...")
                        }
                        is TdApi.AuthorizationStateWaitPhoneNumber -> LoginScreen { telegramManager.setAuthenticationPhoneNumber(it) }
                        is TdApi.AuthorizationStateWaitCode -> CodeScreen { telegramManager.checkAuthenticationCode(it) }
                        is TdApi.AuthorizationStateWaitPassword -> PasswordScreen { telegramManager.checkAuthenticationPassword(it) }
                        is TdApi.AuthorizationStateReady -> MainScreen(telegramManager)
                        else -> LoadingScreen("Cargando TV PLAYER PLUS...")
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
    
    LaunchedEffect(Unit) {
        // Enlace directo a tu grupo
        manager.addGroup("https://t.me/+09n25qE5hCA0Yzdk") { chat ->
            manager.getChatMessages(chat.id) { result ->
                movies.clear()
                movies.addAll(result.messages.filter { it.content is TdApi.MessageVideo })
            }
        }
    }

    Scaffold(containerColor = Color.Black) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { HeroSection(movies.firstOrNull(), manager) }
                item { MovieRow("🏆 MUNDIAL 2026", movies, manager) }
                item { MovieRow("Novedades", movies.reversed(), manager) }
                item { MovieRow("FAVORITOS", movies.shuffled(), manager) }
                item { MovieRow("PELICULAS", movies, manager) }
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
        Text("TV PLAYER PLUS", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Spacer(modifier = Modifier.width(30.dp))
        Text("INICIO", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(20.dp))
        Text("DEPORTES", color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(20.dp))
        Text("PELICULAS", color = Color.LightGray, fontSize = 14.sp)
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
            Text("PELÍCULA DESTACADA", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val video = (message?.content as? TdApi.MessageVideo)?.video
                    video?.let { v ->
                        val url = "http://localhost:8080/stream?fileId=${v.video.id}"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(url), "video/*")
                            setPackage("org.videolan.vlc")
                        }
                        context.startActivity(intent)
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

@Composable
fun MovieRow(title: String, movies: List<TdApi.Message>, manager: TelegramManager) {
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
    Column(modifier = Modifier.width(180.dp).clickable {
        video?.let { v ->
            val url = "http://localhost:8080/stream?fileId=${v.video.id}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "video/*")
                setPackage("org.videolan.vlc")
            }
            context.startActivity(intent)
        }
    }) {
        Box(modifier = Modifier.height(260.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F))) {
            ThumbnailImage(video?.thumbnail?.file, manager, Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ThumbnailImage(file: TdApi.File?, manager: TelegramManager, modifier: Modifier) {
    var path by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        file?.let {
            if (it.local.isDownloadingCompleted) path = it.local.path
            else manager.downloadFile(it.id)
        }
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
fun LoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column {
            Text("TV PLAYER PLUS", color = Color(0xFFE50914), fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono (+34...)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onLogin(phone) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("CONTINUAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CodeScreen(onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column {
            Text("CÓDIGO SMS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onCode(code) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PasswordScreen(onPassword: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column {
            Text("CONTRASEÑA 2FA", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onPassword(pass) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}
