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
    val channels = remember { mutableStateListOf<WebChat>() }
    
    // LOGICA REAL DE TELEGRAM (SIMULADA POR PASOS HASTA RECIBIR EVENTOS DE TDWEB)
    LaunchedEffect(Unit) {
        delay(1500)
        currentStep = AuthStep.WAIT_PHONE
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (currentStep) {
            AuthStep.INITIALIZING -> LoadingScreen("Iniciando motor TV PLAYER PLUS...")
            
            AuthStep.WAIT_PHONE -> WebLoginScreen { phone -> 
                // Aquí se llamaría a TdClient.send(createQuery("setAuthenticationPhoneNumber", ...))
                currentStep = AuthStep.WAIT_CODE 
            }
            
            AuthStep.WAIT_CODE -> WebCodeScreen { code -> 
                // Aquí se llamaría a TdClient.send(createQuery("checkAuthenticationCode", ...))
                // Si la cuenta tiene 2FA, pasaría a WAIT_PASSWORD
                currentStep = AuthStep.WAIT_PASSWORD
            }
            
            AuthStep.WAIT_PASSWORD -> WebPasswordScreen { password ->
                // Aquí se llamaría a TdClient.send(createQuery("checkAuthenticationPassword", ...))
                currentStep = AuthStep.READY
            }
            
            AuthStep.READY -> {
                LaunchedEffect(Unit) {
                    if (channels.isEmpty()) {
                        channels.addAll(listOf(
                            WebChat(1, "Estrenos Mundiales"),
                            WebChat(2, "Series TV Cine"),
                            WebChat(3, "Documentales Pro"),
                            WebChat(4, "Acción Total")
                        ))
                    }
                }
                MainContent(channels)
            }
        }
    }
}

@Composable
fun WebPasswordScreen(onPassword: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(450.dp).background(Color.Black.copy(alpha = 0.8f)).padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verificación en dos pasos", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Introduce tu contraseña de Telegram", color = Color.LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onPassword(password) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Entrar", fontWeight = FontWeight.Bold)
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
            Spacer(modifier = Modifier.height(10.dp))
            Text("Ingresa tu teléfono para comenzar", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Número (+34...)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = { onLogin(phone) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Siguiente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun WebCodeScreen(onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(450.dp).background(Color.Black.copy(alpha = 0.8f)).padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Código SMS", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Enviado a tu Telegram o SMS", color = Color.LightGray)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = { onCode(code) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Verificar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 6.dp, modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(25.dp))
            Text(message, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MainContent(channels: List<WebChat>) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HeroSection() }
            item { MovieRow("Continuar viendo en TV PLAYER PLUS", channels) }
            item { MovieRow("Tendencias mundiales", channels.reversed()) }
            item { MovieRow("Películas Recomendadas", channels.shuffled()) }
        }
        TopNavBar()
    }
}

@Composable
fun TopNavBar() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent))).padding(horizontal = 60.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TV PLAYER PLUS", color = Color(0xFFE50914), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(50.dp))
        Text("Inicio", color = Color.White, modifier = Modifier.padding(15.dp).clickable {})
        Text("Series", color = Color.LightGray, modifier = Modifier.padding(15.dp).clickable {})
        Text("Películas", color = Color.LightGray, modifier = Modifier.padding(15.dp).clickable {})
    }
}

@Composable
fun HeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(650.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF141414)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 60.dp, bottom = 120.dp).widthIn(max = 800.dp)) {
            Text("Deadpool & Wolverine", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Text("El épico regreso de los héroes más gamberros. Ya disponible en exclusiva en tu plataforma TV PLAYER PLUS.", color = Color.White, fontSize = 22.sp, lineHeight = 30.sp)
            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(50.dp).width(150.dp)) {
                Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun MovieRow(title: String, channels: List<WebChat>) {
    Column(modifier = Modifier.padding(vertical = 20.dp)) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 60.dp, bottom = 15.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            items(channels) { chat ->
                MovieCard(chat)
            }
        }
    }
}

@Composable
fun MovieCard(chat: WebChat) {
    Box(modifier = Modifier.width(300.dp).height(170.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2F2F2F)).clickable {}) {
        Text(chat.title, modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

data class WebChat(val id: Int, val title: String)
