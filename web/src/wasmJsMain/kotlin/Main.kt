import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlin.js.JsAny

enum class AuthStep { INITIALIZING, WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY, FATAL_ERROR }
enum class LoadState { IDLE, LOADING, LOADED, ERROR }

data class WebMovie(val title: String, val synopsis: String, val link: String, val posterUrl: String, val fileId: Int)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("Tv Player+") {
        TvPlayerWebTheme { TvPlayerApp() }
    }
}

@Composable
fun TvPlayerWebTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE50914),
            background = Color(0xFF000000),
            surface = Color(0xFF141414),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun TvPlayerApp() {
    val apiId = 8952741
    val apiHash = "693fb2da124662dad85b2b337c53a386"
    val groupLink = "https://t.me/+09n25qE5hCA0Yzdk"

    var currentStep by remember { mutableStateOf(AuthStep.INITIALIZING) }
    var telegramClient by remember { mutableStateOf<JsAny?>(null) }
    var loadState by remember { mutableStateOf(LoadState.IDLE) }
    var loadError by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var fatalErrorMessage by remember { mutableStateOf("") }
    val movies = remember { mutableStateListOf<WebMovie>() }
    var selectedMovie by remember { mutableStateOf<WebMovie?>(null) }

    LaunchedEffect(Unit) {
        try {
            val options = createTdOptions(apiId, apiHash)
            val client = createTdClient(options)
            
            if (client == null) {
                fatalErrorMessage = "No se pudo conectar con el servidor de Telegram (Librería no cargada)."
                currentStep = AuthStep.FATAL_ERROR
                return@LaunchedEffect
            }

            telegramClient = client
            setUpdateHandler(client) { update ->
                val type = getTdType(update)
                if (type == "error") {
                    isProcessing = false
                    errorMessage = getErrorMessage(update)
                    return@setUpdateHandler
                }
                if (type == "updateAuthorizationState") {
                    val state = getAuthState(update)
                    errorMessage = null
                    isProcessing = false
                    hideLoadingScreen() // Ocultar pantalla de carga HTML cuando Telegram responda
                    
                    when (state) {
                        "authorizationStateWaitPhoneNumber" -> currentStep = AuthStep.WAIT_PHONE
                        "authorizationStateWaitCode" -> currentStep = AuthStep.WAIT_CODE
                        "authorizationStateWaitPassword" -> currentStep = AuthStep.WAIT_PASSWORD
                        "authorizationStateReady" -> {
                            currentStep = AuthStep.READY
                            if (loadState == LoadState.IDLE) {
                                loadState = LoadState.LOADING
                                loadGroupVideos(client, groupLink, 80) { result ->
                                    if (getTdType(result) == "loadedGroupVideos") {
                                        movies.clear()
                                        val count = videosCount(result)
                                        for (index in 0 until count) {
                                            movies.add(WebMovie(
                                                title = videoTitle(result, index),
                                                synopsis = "Película cargada desde Telegram.",
                                                link = "",
                                                posterUrl = videoPosterUrl(result, index),
                                                fileId = 0
                                            ))
                                        }
                                        loadState = LoadState.LOADED
                                    } else {
                                        loadError = getErrorMessage(result)
                                        loadState = LoadState.ERROR
                                    }
                                }
                            }
                        }
                        "authorizationStateWaitTdlibParameters" -> {
                            val params = createBaseQuery("setTdlibParameters")
                            addIntParamToQuery(params, "api_id", apiId)
                            addParamToQuery(params, "api_hash", apiHash)
                            addParamToQuery(params, "database_directory", "tdlib")
                            addParamToQuery(params, "files_directory", "tdlib_files")
                            addBooleanParamToQuery(params, "use_file_database", true)
                            addBooleanParamToQuery(params, "use_chat_info_database", true)
                            addBooleanParamToQuery(params, "use_message_database", true)
                            addBooleanParamToQuery(params, "use_secret_chats", false)
                            addParamToQuery(params, "system_language_code", "es")
                            addParamToQuery(params, "device_model", "WebBrowser")
                            addParamToQuery(params, "system_version", "Web")
                            addParamToQuery(params, "application_version", "2.1")
                            sendQuery(client, params)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            fatalErrorMessage = "Error interno: ${e.message}"
            currentStep = AuthStep.FATAL_ERROR
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (currentStep) {
            AuthStep.INITIALIZING -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE50914))
                        Spacer(Modifier.height(16.dp))
                        Text("Conectando...", color = Color.White)
                    }
                }
            }
            AuthStep.FATAL_ERROR -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                        Text("⚠️ ERROR DE CONEXIÓN", color = Color(0xFFE50914), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(fatalErrorMessage, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { /* Recargar página */ }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                            Text("REINTENTAR")
                        }
                    }
                }
            }
            AuthStep.WAIT_PHONE -> WebLoginScreen(errorMessage) { phone ->
                isProcessing = true
                telegramClient?.let {
                    val q = createBaseQuery("setAuthenticationPhoneNumber")
                    addParamToQuery(q, "phone_number", if (phone.startsWith("+")) phone else "+$phone")
                    sendQuery(it, q)
                }
            }
            AuthStep.WAIT_CODE -> WebCodeScreen(errorMessage) { code ->
                isProcessing = true
                telegramClient?.let {
                    val q = createBaseQuery("checkAuthenticationCode")
                    addParamToQuery(q, "code", code)
                    sendQuery(it, q)
                }
            }
            AuthStep.WAIT_PASSWORD -> WebPasswordScreen(errorMessage) { pass ->
                isProcessing = true
                telegramClient?.let {
                    val q = createBaseQuery("checkAuthenticationPassword")
                    addParamToQuery(q, "password", pass)
                    sendQuery(it, q)
                }
            }
            AuthStep.READY -> {
                MainContent(movies, loadState, loadError) { selectedMovie = it }
                selectedMovie?.let { movie ->
                    WebMovieDetail(movie) { selectedMovie = null }
                }
            }
        }
        
        if (isProcessing && currentStep != AuthStep.READY) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        }
    }
}

@Composable
fun MainContent(movies: List<WebMovie>, loadState: LoadState, loadError: String, onMovieClick: (WebMovie) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { WebHero(movies.firstOrNull(), onMovieClick) }
            when {
                loadState == LoadState.LOADING -> item { LoadingRow("Cargando catálogo...") }
                loadState == LoadState.ERROR -> item { ErrorRow(loadError) }
                else -> {
                    item { WebMovieRow("TENDENCIAS", movies, onMovieClick) }
                    item { WebMovieRow("PELÍCULAS", movies.reversed(), onMovieClick) }
                    item { WebMovieRow("SERIES", movies.shuffled(), onMovieClick) }
                }
            }
        }
        WebTopBar()
    }
}

@Composable
fun WebHero(movie: WebMovie?, onClick: (WebMovie) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(550.dp).clickable { movie?.let { onClick(it) } }) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF141414)))
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(60.dp)) {
            Text(movie?.title?.uppercase() ?: "Tv Player+", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { movie?.let { onClick(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("REPRODUCIR", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun WebMovieRow(title: String, movies: List<WebMovie>, onClick: (WebMovie) -> Unit) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 20.dp)) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 60.dp, bottom = 15.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            items(movies) { movie ->
                WebNetflixCard(movie, onClick)
            }
        }
    }
}

@Composable
fun WebNetflixCard(movie: WebMovie, onClick: (WebMovie) -> Unit) {
    Column(modifier = Modifier.width(200.dp).clickable { onClick(movie) }) {
        Box(modifier = Modifier.height(280.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F))) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            Text(
                movie.title, 
                color = Color.White, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WebMovieDetail(movie: WebMovie, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(800.dp).fillMaxHeight(0.8f).clickable(enabled = false) {},
            color = Color(0xFF181818),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(400.dp).background(Color.Black)) {
                    Text("Tv Player+", Modifier.align(Alignment.Center), color = Color.White)
                    Text("X", Modifier.align(Alignment.TopEnd).padding(20.dp).clickable { onDismiss() }, color = Color.White, fontSize = 24.sp)
                }
                Column(Modifier.padding(40.dp)) {
                    Text(movie.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    Text(movie.synopsis, color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = { /* Play logic */ },
                        modifier = Modifier.width(200.dp).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Text("VER AHORA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WebTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))).padding(horizontal = 60.dp, vertical = 25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tv Player+", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Spacer(modifier = Modifier.width(50.dp))
        Text("INICIO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(25.dp))
        Text("PELÍCULAS", color = Color.LightGray, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(25.dp))
        Text("SERIES", color = Color.LightGray, fontSize = 16.sp)
    }
}

@Composable
fun LoadingRow(text: String) {
    Row(Modifier.padding(horizontal = 60.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(15.dp))
        Text(text, color = Color.White)
    }
}

@Composable
fun ErrorRow(message: String) {
    Text("Error: $message", color = Color.Red, modifier = Modifier.padding(60.dp))
}

@Composable
fun WebLoginScreen(error: String?, onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color(0xFF141414)).padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tv Player+", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFE50914))
            Spacer(modifier = Modifier.height(40.dp))
            if (error != null) Text(error, color = Color.Red, modifier = Modifier.padding(bottom = 10.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono (+34...)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(25.dp))
            Button(onClick = { onLogin(phone) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebCodeScreen(error: String?, onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color(0xFF141414)).padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CÓDIGO SMS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(25.dp))
            if (error != null) Text(error, color = Color.Red)
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(25.dp))
            Button(onClick = { onCode(code) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebPasswordScreen(error: String?, onPassword: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color(0xFF141414)).padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CONTRASEÑA 2FA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(25.dp))
            if (error != null) Text(error, color = Color.Red)
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(25.dp))
            Button(onClick = { onPassword(pass) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}
