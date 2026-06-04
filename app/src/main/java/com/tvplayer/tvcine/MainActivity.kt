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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tvplayer.tvcine.ui.theme.TvPlayerTheme
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
            TvPlayerTheme(dynamicColor = false) {
                var authState by remember { mutableStateOf<TdApi.AuthorizationState?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var isProcessing by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    telegramManager.start { update ->
                        if (update is TdApi.UpdateAuthorizationState) {
                            authState = update.authorizationState
                            isProcessing = false
                        }
                    }
                }

                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    when (authState) {
                        is TdApi.AuthorizationStateWaitTdlibParameters -> {
                            LaunchedEffect(Unit) { telegramManager.setupParameters() }
                            LoadingScreen("Iniciando...")
                        }
                        is TdApi.AuthorizationStateWaitPhoneNumber -> LoginScreen(errorMessage) { phone ->
                            isProcessing = true
                            telegramManager.setAuthenticationPhoneNumber(if (phone.startsWith("+")) phone else "+$phone")
                        }
                        is TdApi.AuthorizationStateWaitCode -> CodeScreen(errorMessage) { code ->
                            isProcessing = true
                            telegramManager.checkAuthenticationCode(code)
                        }
                        is TdApi.AuthorizationStateWaitPassword -> PasswordScreen(errorMessage) { pass ->
                            isProcessing = true
                            telegramManager.checkAuthenticationPassword(pass)
                        }
                        is TdApi.AuthorizationStateReady -> MainScreen(telegramManager)
                        null -> LoadingScreen("Cargando...")
                        else -> LoadingScreen("Conectando...")
                    }
                    if (isProcessing) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFE50914))
                        }
                    }
                }
            }
        }
    }
}

data class MovieInfo(val title: String, val synopsis: String, val link: String, val message: TdApi.Message)

fun parseCaption(caption: String, msg: TdApi.Message): MovieInfo {
    val lines = caption.lines().filter { it.isNotBlank() }
    val title = lines.getOrNull(0) ?: "Contenido Multimedia"
    val link = lines.find { it.contains("http") } ?: ""
    val synopsis = lines.drop(1).filter { it != link }.joinToString("\n").ifEmpty { "Sin descripción disponible." }
    return MovieInfo(title, synopsis, link, msg)
}

@Composable
fun MainScreen(manager: TelegramManager) {
    val movies = remember { mutableStateListOf<MovieInfo>() }
    var selectedMovie by remember { mutableStateOf<MovieInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        manager.getChat(-1003749684388L) { chat ->
            manager.getChatMessages(chat.id, 0, 80) { result ->
                movies.clear()
                result.messages.filter { it.content is TdApi.MessageVideo }.forEach { msg ->
                    val caption = (msg.content as TdApi.MessageVideo).caption.text
                    movies.add(parseCaption(caption, msg))
                }
                isLoading = false
            }
        }
    }

    Scaffold(containerColor = Color.Black) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) LoadingScreen("Cargando catálogo...")
            else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item { NetflixHero(movies.firstOrNull(), manager) { selectedMovie = it } }
                    item { MovieRow("TENDENCIAS", movies, manager) { selectedMovie = it } }
                    item { MovieRow("PELÍCULAS", movies.reversed(), manager) { selectedMovie = it } }
                    item { MovieRow("MÁS VISTAS", movies.shuffled(), manager) { selectedMovie = it } }
                }
            }
            TopBar()
            selectedMovie?.let { MovieDetailDialog(it, manager) { selectedMovie = null } }
        }
    }
}

@Composable
fun NetflixHero(movie: MovieInfo?, manager: TelegramManager, onClick: (MovieInfo) -> Unit) {
    movie ?: return
    val video = (movie.message.content as TdApi.MessageVideo).video
    Box(Modifier.fillMaxWidth().height(450.dp).clickable { onClick(movie) }) {
        ThumbnailImage(video.thumbnail?.file, manager, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        Column(Modifier.align(Alignment.BottomCenter).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(movie.title.uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(15.dp))
            Button(onClick = { onClick(movie) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(4.dp)) {
                Icon(Icons.Default.PlayArrow, null)
                Text("REPRODUCIR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MovieRow(title: String, movies: List<MovieInfo>, manager: TelegramManager, onClick: (MovieInfo) -> Unit) {
    if (movies.isEmpty()) return
    Column(Modifier.padding(vertical = 15.dp)) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, bottom = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(movies) { movie ->
                val video = (movie.message.content as TdApi.MessageVideo).video
                Column(Modifier.width(130.dp).clickable { onClick(movie) }) {
                    Box(Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F))) {
                        ThumbnailImage(video.thumbnail?.file, manager, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                        Text(movie.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text(movie.synopsis, color = Color.Gray, fontSize = 9.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp), overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MovieDetailDialog(movie: MovieInfo, manager: TelegramManager, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val video = (movie.message.content as TdApi.MessageVideo).video
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF181818)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(250.dp)) {
                    ThumbnailImage(video.thumbnail?.file, manager, Modifier.fillMaxSize())
                    IconButton(onClick = onDismiss, Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(50.dp))) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                Column(Modifier.padding(20.dp)) {
                    Text(movie.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(movie.synopsis, color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(25.dp))
                    Button(onClick = { playVideo(context, "http://localhost:8080/stream?fileId=${video.video.id}") }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                        Icon(Icons.Default.PlayArrow, null)
                        Text("VER AHORA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbnailImage(file: TdApi.File?, manager: TelegramManager, modifier: Modifier) {
    var path by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        if (file == null) return@LaunchedEffect
        if (file.local.isDownloadingCompleted) path = file.local.path
        else {
            manager.send(TdApi.GetFile(file.id)) { res ->
                if (res is TdApi.File && res.local.isDownloadingCompleted) path = res.local.path
                else manager.downloadFile(file.id)
            }
        }
    }
    if (path != null) AsyncImage(model = path, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFF141414)))
}

@Composable
fun TopBar() {
    Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Tv Player+", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.width(20.dp))
        Text("INICIO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(15.dp))
        Text("PELÍCULAS", color = Color.LightGray, fontSize = 12.sp)
        Spacer(Modifier.width(15.dp))
        Text("SERIES", color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
fun LoadingScreen(msg: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE50914))
            Spacer(Modifier.height(15.dp))
            Text(msg, color = Color.White)
        }
    }
}

@Composable fun LoginScreen(e: String?, onLogin: (String) -> Unit) { /* UI similar a la anterior corregida */ }
@Composable fun CodeScreen(e: String?, onCode: (String) -> Unit) { /* UI similar a la anterior corregida */ }
@Composable fun PasswordScreen(e: String?, onPassword: (String) -> Unit) { /* UI similar a la anterior corregida */ }
fun playVideo(c: android.content.Context, u: String) { /* Lógica de intent VLC */ }
