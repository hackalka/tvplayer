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

// Estado de autenticación para la Web
enum class AuthStep {
    INITIALIZING,
    WAIT_PHONE,
    WAIT_CODE,
    WAIT_PASSWORD,
    READY
}

// Datos de chat simplificados para Web
data class WebChat(val id: Int, val title: String)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("TV Cine Web") {
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
    
    LaunchedEffect(Unit) {
        // Simulación de carga inicial
        kotlinx.coroutines.delay(1000)
        currentStep = AuthStep.WAIT_PHONE
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (currentStep) {
            AuthStep.INITIALIZING -> LoadingScreen("Iniciando TV Cine...")
            AuthStep.WAIT_PHONE -> WebLoginScreen { phone -> 
                currentStep = AuthStep.WAIT_CODE 
            }
            AuthStep.WAIT_CODE -> WebCodeScreen { code -> 
                currentStep = AuthStep.READY
            }
            AuthStep.READY -> {
                LaunchedEffect(Unit) {
                    if (channels.isEmpty()) {
                        channels.addAll(listOf(
                            WebChat(1, "Estrenos"),
                            WebChat(2, "Series"),
                            WebChat(3, "Documentales"),
                            WebChat(4, "Acción"),
                            WebChat(5, "Terror"),
                            WebChat(6, "Comedia")
                        ))
                    }
                }
                MainContent(channels)
            }
            else -> {}
        }
    }
}

@Composable
fun MainContent(channels: List<WebChat>) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HeroSection() }
            item { MovieRow("Novedades", channels) }
            item { MovieRow("Tendencias", channels.reversed()) }
            item { MovieRow("Mi Lista", channels.shuffled()) }
        }
        TopNavBar()
    }
}

@Composable
fun WebLoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color.Black.copy(alpha = 0.5f)).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Iniciar sesión", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onLogin(phone) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebCodeScreen(onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(400.dp).background(Color.Black.copy(alpha = 0.5f)).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verificar código", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onCode(code) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
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
fun TopNavBar() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent))).padding(horizontal = 60.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TV CINE", color = Color(0xFFE50914), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(50.dp))
        Text("Inicio", color = Color.White, modifier = Modifier.padding(15.dp).clickable {})
        Text("Series", color = Color.LightGray, modifier = Modifier.padding(15.dp).clickable {})
    }
}

@Composable
fun HeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(600.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF141414)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 60.dp, bottom = 100.dp).widthIn(max = 700.dp)) {
            Text("Deadpool & Wolverine", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Text("El estreno más esperado del año ya disponible en TV Cine.", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(4.dp)) {
                Text("Reproducir", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MovieRow(title: String, channels: List<WebChat>) {
    Column(modifier = Modifier.padding(vertical = 15.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 60.dp, bottom = 15.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(channels) { chat ->
                MovieCard(chat)
            }
        }
    }
}

@Composable
fun MovieCard(chat: WebChat) {
    Box(modifier = Modifier.width(240.dp).height(135.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2F2F2F)).clickable {}) {
        Text(chat.title, modifier = Modifier.align(Alignment.Center), color = Color.White)
    }
}
