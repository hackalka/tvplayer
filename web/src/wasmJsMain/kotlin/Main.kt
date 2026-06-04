import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlin.js.JsAny

enum class AuthStep { INITIALIZING, WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY }
enum class LoadState { IDLE, LOADING, LOADED, ERROR }

data class WebMovie(val title: String, val genre: String, val posterUrl: String)

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
    val groupLink = "https://web.telegram.org/a/#-1003749684388"

    var currentStep by remember { mutableStateOf(AuthStep.INITIALIZING) }
    var telegramClient by remember { mutableStateOf<JsAny?>(null) }
    var loadState by remember { mutableStateOf(LoadState.IDLE) }
    var loadError by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val movies = remember { mutableStateListOf<WebMovie>() }

    LaunchedEffect(Unit) {
        hideLoadingScreen()

        try {
            val options = createTdOptions(apiId, apiHash)
            val client = createTdClient(options)
            if (client != null) {
                telegramClient = client
                setUpdateHandler(client) { update ->
                    val type = getTdType(update)
                    if (type == "updateAuthorizationState") {
                        val state = getAuthState(update)
                        errorMessage = null
                        isProcessing = false
                        when (state) {
                            "authorizationStateWaitPhoneNumber" -> currentStep = AuthStep.WAIT_PHONE
                            "authorizationStateWaitCode" -> currentStep = AuthStep.WAIT_CODE
                            "authorizationStateWaitPassword" -> currentStep = AuthStep.WAIT_PASSWORD
                            "authorizationStateReady" -> {
                                currentStep = AuthStep.READY
                                if (loadState == LoadState.IDLE) {
                                    loadState = LoadState.LOADING
                                    loadGroupVideos(client, groupLink, 80) { result ->
                                        when (getTdType(result)) {
                                            "loadedGroupVideos" -> {
                                                movies.clear()
                                                val count = videosCount(result)
                                                for (index in 0 until count) {
                                                    movies.add(
                                                        WebMovie(
                                                            title = videoTitle(result, index),
                                                            genre = videoGenre(result, index),
                                                            posterUrl = videoPosterUrl(result, index)
                                                        )
                                                    )
                                                }
                                                loadState = LoadState.LOADED
                                            }
                                            "loadGroupVideosError" -> {
                                                loadError = getErrorMessage(result)
                                                loadState = LoadState.ERROR
                                            }
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
                                addParamToQuery(params, "application_version", "1.0")
                                sendQuery(client, params)
                            }
                        }
                    }
                }
            } else {
                currentStep = AuthStep.WAIT_PHONE
            }
        } catch (e: Exception) {
            currentStep = AuthStep.WAIT_PHONE
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isProcessing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else {
            when (currentStep) {
                AuthStep.INITIALIZING -> {}
                AuthStep.WAIT_PHONE -> WebLoginScreen(errorMessage) { phone ->
                    isProcessing = true
                    telegramClient?.let {
                        val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
                        val q = createBaseQuery("setAuthenticationPhoneNumber")
                        addParamToQuery(q, "phone_number", formattedPhone)
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
                AuthStep.READY -> MainContent(movies, loadState, loadError)
            }
        }
    }
}

@Composable
fun MainContent(movies: List<WebMovie>, loadState: LoadState, loadError: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HeroSection() }
            when {
                loadState == LoadState.LOADING -> item { LoadingRow("Cargando catálogo...") }
                loadState == LoadState.ERROR -> item { ErrorRow(loadError) }
                movies.isEmpty() -> item { LoadingRow("Buscando contenido...") }
                else -> {
                    item { MovieRow("TENDENCIAS", movies) }
                    item { MovieRow("RECIÉN AÑADIDAS", movies.reversed()) }
                    item { MovieRow("MI LISTA", movies.shuffled()) }
                }
            }
        }
        WebTopBar()
    }
}

@Composable
fun WebTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)))
            .padding(horizontal = 40.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tv Player+", color = Color(0xFFE50914), fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.width(40.dp))
        Text("INICIO", color = Color.White, modifier = Modifier.padding(10.dp).clickable {}, fontWeight = FontWeight.Bold)
        Text("PELÍCULAS", color = Color.LightGray, modifier = Modifier.padding(10.dp).clickable {})
        Text("SERIES", color = Color.LightGray, modifier = Modifier.padding(10.dp).clickable {})
    }
}

@Composable
fun HeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(600.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(40.dp)) {
            Text("Tv Player+", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("Tu videoclub personal en streaming", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun LoadingRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun ErrorRow(message: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp)) {
        Text("Error al cargar contenido", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.LightGray, fontSize = 14.sp)
    }
}

@Composable
fun MovieRow(title: String, movies: List<WebMovie>) {
    Column(modifier = Modifier.padding(vertical = 15.dp)) {
        Text(
            title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 40.dp, bottom = 15.dp)
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            items(movies) { MovieCard(it) }
        }
    }
}

@Composable
fun MovieCard(movie: WebMovie) {
    Box(
        modifier = Modifier.width(220.dp).height(310.dp).clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1F1F1F)).clickable { }
    ) {
        Text(movie.title, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WebLoginScreen(error: String?, onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(420.dp).background(Color(0xFF141414)).padding(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tv Player+", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFE50914))
            Spacer(modifier = Modifier.height(32.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono (+34...)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onLogin(phone) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun WebCodeScreen(error: String?, onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(420.dp).background(Color(0xFF141414)).padding(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CÓDIGO SMS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código de Telegram") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onCode(code) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebPasswordScreen(error: String?, onPassword: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(420.dp).background(Color(0xFF141414)).padding(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CONTRASEÑA 2FA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            if (error != null) {
                Text(error, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onPassword(pass) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}
