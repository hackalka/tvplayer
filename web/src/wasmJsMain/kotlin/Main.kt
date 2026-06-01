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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.delay
import kotlin.js.JsAny

enum class AuthStep { INITIALIZING, WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("TV PLAYER PLUS") {
        TvcineWebTheme { CineflixApp() }
    }
}

@Composable
fun TvcineWebTheme(content: @Composable () -> Unit) {
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
fun CineflixApp() {
    var currentStep by remember { mutableStateOf(AuthStep.INITIALIZING) }
    var telegramClient by remember { mutableStateOf<JsAny?>(null) }
    val movies = remember { mutableStateListOf<WebMovie>() }
    
    LaunchedEffect(Unit) {
        delay(2000)
        hideLoadingStatus()
        
        try {
            val options = createTdOptions(8952741, "693fb2da124662dad85b2b337c53a386")
            val client = createTdClient(options)
            if (client != null) {
                telegramClient = client
                setUpdateHandler(client) { update ->
                    val type = getTdType(update)
                    if (type == "updateAuthorizationState") {
                        val state = getAuthState(update)
                        when (state) {
                            "authorizationStateWaitPhoneNumber" -> currentStep = AuthStep.WAIT_PHONE
                            "authorizationStateWaitCode" -> currentStep = AuthStep.WAIT_CODE
                            "authorizationStateWaitPassword" -> currentStep = AuthStep.WAIT_PASSWORD
                            "authorizationStateReady" -> {
                                currentStep = AuthStep.READY
                            }
                            "authorizationStateWaitTdlibParameters" -> {
                                val params = createBaseQuery("setTdlibParameters")
                                addParamToQuery(params, "api_id", "8952741")
                                addParamToQuery(params, "api_hash", "693fb2da124662dad85b2b337c53a386")
                                addParamToQuery(params, "database_directory", "tdlib")
                                addParamToQuery(params, "use_message_database", "true")
                                addParamToQuery(params, "use_secret_chats", "true")
                                addParamToQuery(params, "system_language_code", "es")
                                addParamToQuery(params, "device_model", "Web")
                                addParamToQuery(params, "application_version", "1.0")
                                sendQuery(client, params)
                            }
                        }
                    }
                }
            } else { currentStep = AuthStep.WAIT_PHONE }
        } catch (e: Exception) { currentStep = AuthStep.WAIT_PHONE }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (currentStep) {
            AuthStep.INITIALIZING -> {} 
            AuthStep.WAIT_PHONE -> WebLoginScreen { phone -> 
                telegramClient?.let { 
                    val q = createBaseQuery("setAuthenticationPhoneNumber")
                    addParamToQuery(q, "phone_number", phone)
                    sendQuery(it, q)
                }
            }
            AuthStep.WAIT_CODE -> WebCodeScreen { code -> 
                telegramClient?.let {
                    val q = createBaseQuery("checkAuthenticationCode")
                    addParamToQuery(q, "code", code)
                    sendQuery(it, q)
                }
            }
            AuthStep.WAIT_PASSWORD -> WebPasswordScreen { pass ->
                telegramClient?.let {
                    val q = createBaseQuery("checkAuthenticationPassword")
                    addParamToQuery(q, "password", pass)
                    sendQuery(it, q)
                }
            }
            AuthStep.READY -> {
                LaunchedEffect(Unit) {
                    if (movies.isEmpty()) {
                        movies.addAll(listOf(
                            WebMovie("Misión Imposible", "Acción", ""),
                            WebMovie("Deadpool 3", "Comedia", ""),
                            WebMovie("House of the Dragon", "Serie", "")
                        ))
                    }
                }
                MainContent(movies)
            }
        }
    }
}

@Composable
fun MainContent(movies: List<WebMovie>) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HeroSection() }
            item { MovieRow("Novedades", movies) }
            item { MovieRow("🏆 MUNDIAL 2026", movies.shuffled()) }
            item { MovieRow("FAVORITOS", movies.reversed()) }
            item { MovieRow("PELICULAS", movies) }
            item { MovieRow("SERIES", movies.shuffled()) }
        }
        WebTopBar()
    }
}

@Composable
fun WebTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)))
            .padding(horizontal = 40.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TV PLAYER PLUS", color = Color(0xFFE50914), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.width(40.dp))
        Text("INICIO", color = Color.White, modifier = Modifier.padding(10.dp).clickable {})
        Text("PELICULAS", color = Color.LightGray, modifier = Modifier.padding(10.dp).clickable {})
        Text("SERIES", color = Color.LightGray, modifier = Modifier.padding(10.dp).clickable {})
        Text("DEPORTES", color = Color.LightGray, modifier = Modifier.padding(10.dp).clickable {})
    }
}

@Composable
fun HeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(650.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
        ))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(40.dp)) {
            Text("BIENVENIDO A TV PLAYER PLUS", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold)
            Text("Todo el contenido de Telegram en un solo lugar.", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(4.dp)) {
                Text("REPRODUCIR AHORA", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MovieRow(title: String, movies: List<WebMovie>) {
    Column(modifier = Modifier.padding(vertical = 20.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 40.dp, bottom = 15.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            items(movies) { MovieCard(it) }
        }
    }
}

@Composable
fun MovieCard(movie: WebMovie) {
    Column(modifier = Modifier.width(220.dp).clickable { }) {
        Box(modifier = Modifier.height(300.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1F1F1F))) {
            Text("CARATULA", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WebLoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color(0xFF141414)).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Iniciar sesión", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono (+34...)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onLogin(phone) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("CONTINUAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebCodeScreen(onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color(0xFF141414)).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verificar código", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código de Telegram") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onCode(code) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebPasswordScreen(onPassword: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color(0xFF141414)).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Contraseña 2FA", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onPassword(pass) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class WebMovie(val title: String, val genre: String, val posterUrl: String)
