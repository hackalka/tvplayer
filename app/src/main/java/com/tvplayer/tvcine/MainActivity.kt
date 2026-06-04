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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                            authState = update.authorizationState
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
                                            errorMessage = "Código inválido"
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
                                is TdApi.AuthorizationStateReady -> MainScreen(telegramManager)
                                null -> LoadingScreen("Cargando...")
                                else -> LoadingScreen("Iniciando sistema...")
                            }
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

data class MovieInfo(
    val title: String,
    val synopsis: String,
    val link: String,
    val message: TdApi.Message
)

fun parseCaption(caption: String): MovieInfo? {
    val lines = caption.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return null
    val title = lines.getOrNull(0) ?: "Sin título"
    val link = lines.find { it.contains("http") } ?: ""
    val synopsis = lines.drop(1).filter { it != link }.joinToString("\n")
    return MovieInfo(title, synopsis, link, TdApi.Message()) // placeholder message
}

@Composable
fun MainScreen(manager: TelegramManager) {
    val movies = remember { mutableStateListOf<MovieInfo>() }
    var selectedMovie by remember { mutableStateOf<MovieInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Conectando con el catálogo...") }
    
    LaunchedEffect(Unit) {
        // Usamos el ID directo para máxima velocidad
        manager.getChat(-1003749684388L) { chat ->
            status = "Cargando catálogo de Tv Player+..."
            manager.getChatMessages(chat.id, 0, 100) { result ->
                val videoMessages = result.messages.filter { it.content is TdApi.MessageVideo }
                movies.clear()
                videoMessages.forEach { msg ->
                    val caption = (msg.content as TdApi.MessageVideo).caption.text
                    val info = parseCaption(caption)
                    if (info != null) {
                        movies.add(info.copy(message = msg))
                    }
                }
                isLoading = false
                if (movies.isEmpty()) status = "No hay contenido disponible"
            }
        }
    }

    Scaffold(containerColor = Color.Black) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isLoading && movies.isEmpty()) {
                LoadingScreen(status)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { NetflixHero(movies.firstOrNull(), manager) { selectedMovie = it } }
                    item { MovieRow("TENDENCIAS", movies, manager) { selectedMovie = it } }
                    item { MovieRow("PELÍCULAS", movies.reversed(), manager) { selectedMovie = it } }
                    item { MovieRow("SERIES", movies.shuffled(), manager) { selectedMovie = it } }
                }
            }
            TopBar()
            
            selectedMovie?.let { movie ->
                MovieDetailDialog(movie, manager, onDismiss = { selectedMovie = null })
            }
        }
    }
}

@Composable
fun NetflixHero(movie: MovieInfo?, manager: TelegramManager, onClick: (MovieInfo) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(500.dp).clickable { movie?.let { onClick(it) } }) {
        movie?.let {
            val video = (it.message.content as? TdApi.MessageVideo)?.video
            ThumbnailImage(video?.thumbnail?.file, manager, Modifier.fillMaxSize())
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(movie?.title?.uppercase() ?: "", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Button(
                    onClick = { movie?.let { onClick(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.width(150.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("REPRODUCIR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MovieRow(title: String, movies: List<MovieInfo>, manager: TelegramManager, onClick: (MovieInfo) -> Unit) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 15.dp)) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, bottom = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(movies) { movie ->
                NetflixCard(movie, manager, onClick)
            }
        }
    }
}

@Composable
fun NetflixCard(movie: MovieInfo, manager: TelegramManager, onClick: (MovieInfo) -> Unit) {
    val video = (movie.message.content as? TdApi.MessageVideo)?.video
    Column(modifier = Modifier.width(140.dp).clickable { onClick(movie) }) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F))) {
            ThumbnailImage(video?.thumbnail?.file, manager, Modifier.fillMaxSize())
            // Title inside cover
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            Text(
                movie.title, 
                color = Color.White, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            movie.synopsis, 
            color = Color.Gray, 
            fontSize = 10.sp, 
            maxLines = 2, 
            modifier = Modifier.padding(top = 4.dp),
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MovieDetailDialog(movie: MovieInfo, manager: TelegramManager, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val video = (movie.message.content as? TdApi.MessageVideo)?.video

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() }) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.85f).clickable(enabled = false) {},
                color = Color(0xFF181818),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().height(300.dp)) {
                        ThumbnailImage(video?.thumbnail?.file, manager, Modifier.fillMaxSize())
                        IconButton(onClick = onDismiss, Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50.dp))) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    Column(Modifier.padding(24.dp)) {
                        Text(movie.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(movie.synopsis, color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        
                        // Video List / Play Button
                        Button(
                            onClick = {
                                video?.let { v ->
                                    val url = "http://localhost:8080/stream?fileId=${v.video.id}"
                                    playVideo(context, url)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("VER AHORA", fontWeight = FontWeight.Bold)
                        }
                        
                        if (movie.link.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Enlace externo:", color = Color.Gray, fontSize = 12.sp)
                            Text(movie.link, color = Color(0xFF00A8FF), fontSize = 12.sp, modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(movie.link)))
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tv Player+", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(30.dp))
        Text("INICIO", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(20.dp))
        Text("PELÍCULAS", color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(20.dp))
        Text("SERIES", color = Color.LightGray, fontSize = 14.sp)
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
    if (path != null) AsyncImage(model = path, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    else Box(modifier = modifier.background(Color(0xFF141414)))
}

@Composable
fun LoadingScreen(msg: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE50914))
            Spacer(Modifier.height(16.dp))
            Text(msg, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun LoginScreen(error: String?, onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tv Player+", color = Color(0xFFE50914), fontSize = 40.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(40.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = phone, onValueChange = { phone = it }, 
                label = { Text("Teléfono (+34...)") }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onLogin(phone) }, Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CodeScreen(error: String?, onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CÓDIGO SMS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código de 5 dígitos") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onCode(code) }, Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PasswordScreen(error: String?, onPassword: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CONTRASEÑA 2FA", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onPassword(pass) }, Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun playVideo(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
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
