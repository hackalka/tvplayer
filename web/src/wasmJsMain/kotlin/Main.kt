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

enum class AuthStep {
    INITIALIZING,
    WAIT_PHONE,
    WAIT_CODE,
    WAIT_PASSWORD,
    READY
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("TV PLAYER PLUS") {
        TvcineWebTheme {
            CineflixApp()
        }
    }
}

@Composable
fun TvcineWebTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE50914), 
            background = Color(0xFF141414),
            surface = Color(0xFF1C1C1C),
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
    val channels = remember { mutableStateListOf<WebChat>() }
    
    LaunchedEffect(Unit) {
        delay(3000) // Tiempo de seguridad para carga de scripts
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
                            "authorizationStateReady" -> currentStep = AuthStep.READY
                            "authorizationStateWaitTdlibParameters" -> {
                                val params = createBaseQuery("setTdlibParameters")
                                addParamToQuery(params, "api_id", "8952741")
                                addParamToQuery(params, "api_hash", "693fb2da124662dad85b2b337c53a386")
                                addParamToQuery(params, "use_message_database", "true")
                                addParamToQuery(params, "use_chat_info_database", "true")
                                addParamToQuery(params, "use_secret_chats", "true")
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (currentStep) {
            AuthStep.INITIALIZING -> LoadingScreen("Cargando TV PLAYER PLUS...")
            AuthStep.WAIT_PHONE -> WebLoginScreen { phone -> 
                telegramClient?.let { client ->
                    val query = createBaseQuery("setAuthenticationPhoneNumber")
                    addParamToQuery(query, "phone_number", phone)
                    sendQuery(client, query)
                } ?: run { currentStep = AuthStep.WAIT_CODE } // Fallback visual
            }
            AuthStep.WAIT_CODE -> WebCodeScreen { code -> 
                telegramClient?.let { client ->
                    val query = createBaseQuery("checkAuthenticationCode")
                    addParamToQuery(query, "code", code)
                    sendQuery(client, query)
                } ?: run { currentStep = AuthStep.READY } // Fallback visual
            }
            AuthStep.WAIT_PASSWORD -> WebPasswordScreen { password ->
                telegramClient?.let { client ->
                    val query = createBaseQuery("checkAuthenticationPassword")
                    addParamToQuery(query, "password", password)
                    sendQuery(client, query)
                }
            }
            AuthStep.READY -> MainContent(channels)
        }
    }
}

@Composable
fun WebPasswordScreen(onPassword: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(450.dp).background(Color.Black.copy(alpha = 0.8f)).padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verificación 2FA", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña de Telegram") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onPassword(password) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                Text("Entrar")
            }
        }
    }
}

@Composable
fun WebLoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(450.dp).background(Color.Black.copy(alpha = 0.8f)).padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TV PLAYER PLUS", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE50914))
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono (+34...)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = { onLogin(phone) }, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                Text("Enviar Código SMS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebCodeScreen(onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(450.dp).background(Color.Black.copy(alpha = 0.8f)).padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Código SMS", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código recibido") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = { onCode(code) }, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                Text("Verificar")
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE50914))
            Spacer(modifier = Modifier.height(20.dp))
            Text(message, color = Color.White)
        }
    }
}

@Composable
fun MainContent(channels: List<WebChat>) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HeroSection() }
            item { MovieRow("Estrenos TV PLAYER PLUS", channels) }
        }
        TopNavBar()
    }
}

@Composable
fun TopNavBar() {
    Row(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent))).padding(horizontal = 60.dp, vertical = 20.dp)) {
        Text("TV PLAYER PLUS", color = Color(0xFFE50914), fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(600.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF141414)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 60.dp, bottom = 100.dp)) {
            Text("Deadpool & Wolverine", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MovieRow(title: String, channels: List<WebChat>) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

data class WebChat(val id: Int, val title: String)
