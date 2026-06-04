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

enum class AuthStep { STARTING, PHONE, CODE, PASS, READY, ERROR }

data class MovieModel(val title: String, val desc: String, val poster: String, val id: Int)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("Tv Player+") {
        WebTheme { AppShell() }
    }
}

@Composable
fun WebTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE50914),
            background = Color.Black,
            onBackground = Color.White
        ),
        content = content
    )
}

@Composable
fun AppShell() {
    val apiId = 8952741
    val apiHash = "693fb2da124662dad85b2b337c53a386"
    val groupLink = "https://t.me/+09n25qE5hCA0Yzdk"

    var step by remember { mutableStateOf(AuthStep.STARTING) }
    var client by remember { mutableStateOf<JsAny?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val movies = remember { mutableStateListOf<MovieModel>() }

    LaunchedEffect(Unit) {
        println("WASM: Iniciando AppShell")
        try {
            val options = createTdOptions(apiId, apiHash)
            val c = createTdClient(options)
            if (c == null) {
                println("WASM: Error al crear cliente")
                step = AuthStep.ERROR
                error = "Fallo al cargar la librería de Telegram."
                return@LaunchedEffect
            }
            client = c
            println("WASM: Cliente creado, registrando onUpdate")
            setUpdateHandler(c) { update ->
                val type = getTdType(update)
                println("WASM: Update recibido: $type")
                
                if (type == "error") {
                    loading = false
                    error = getErrorMessage(update)
                    println("WASM: Error de Telegram: $error")
                } else if (type == "updateAuthorizationState") {
                    val state = getAuthState(update)
                    println("WASM: Auth State: $state")
                    error = null
                    loading = false
                    hideBootScreen() // Función en index.html
                    
                    when (state) {
                        "authorizationStateWaitPhoneNumber" -> step = AuthStep.PHONE
                        "authorizationStateWaitCode" -> step = AuthStep.CODE
                        "authorizationStateWaitPassword" -> step = AuthStep.PASS
                        "authorizationStateReady" -> {
                            step = AuthStep.READY
                            loadGroupVideos(c, groupLink, 50) { res ->
                                if (getTdType(res) == "loadedGroupVideos") {
                                    movies.clear()
                                    val count = videosCount(res)
                                    for (i in 0 until count) {
                                        movies.add(MovieModel(
                                            videoTitle(res, i),
                                            "Disponible en Tv Player+",
                                            videoPosterUrl(res, i),
                                            i
                                        ))
                                    }
                                }
                            }
                        }
                        "authorizationStateWaitTdlibParameters" -> {
                            println("WASM: Enviando setTdlibParameters")
                            val p = createBaseQuery("setTdlibParameters")
                            addIntParamToQuery(p, "api_id", apiId)
                            addParamToQuery(p, "api_hash", apiHash)
                            addParamToQuery(p, "database_directory", "tdlib")
                            addParamToQuery(p, "files_directory", "tdlib_files")
                            addBooleanParamToQuery(p, "use_file_database", true)
                            addBooleanParamToQuery(p, "use_chat_info_database", true)
                            addBooleanParamToQuery(p, "use_message_database", true)
                            addBooleanParamToQuery(p, "use_secret_chats", false)
                            addParamToQuery(p, "system_language_code", "es")
                            addParamToQuery(p, "device_model", "Web")
                            addParamToQuery(p, "system_version", "Web")
                            addParamToQuery(p, "application_version", "2.6")
                            sendQuery(c, p)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("WASM: Excepción en AppShell: $e")
            step = AuthStep.ERROR
            error = e.toString()
        }
    }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        when (step) {
            AuthStep.READY -> MainGrid(movies)
            AuthStep.PHONE -> LoginView(error) { 
                loading = true
                val q = createBaseQuery("setAuthenticationPhoneNumber")
                addParamToQuery(q, "phone_number", if (it.startsWith("+")) it else "+$it")
                client?.let { c -> sendQuery(c, q) }
            }
            AuthStep.CODE -> CodeView(error) {
                loading = true
                val q = createBaseQuery("checkAuthenticationCode")
                addParamToQuery(q, "code", it)
                client?.let { c -> sendQuery(c, q) }
            }
            AuthStep.PASS -> PassView(error) {
                loading = true
                val q = createBaseQuery("checkAuthenticationPassword")
                addParamToQuery(q, "password", it)
                client?.let { c -> sendQuery(c, q) }
            }
            AuthStep.ERROR -> Box(Modifier.fillMaxSize(), Alignment.Center) { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ ERROR", color = Color(0xFFE50914), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(error ?: "Error fatal", color = Color.White) 
                }
            }
            else -> Box(Modifier.fillMaxSize(), Alignment.Center) { 
                CircularProgressIndicator(color = Color(0xFFE50914)) 
            }
        }
        
        if (loading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        }
    }
}

@Composable
fun MainGrid(movies: List<MovieModel>) {
    var selected by remember { mutableStateOf<MovieModel?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { Hero(movies.firstOrNull()) { selected = it } }
            item { RowView("TENDENCIAS", movies) { selected = it } }
            item { RowView("PELÍCULAS", movies.reversed()) { selected = it } }
            item { RowView("SERIES", movies.shuffled()) { selected = it } }
        }
        TopNav()
        selected?.let { Details(it) { selected = null } }
    }
}

@Composable
fun Hero(m: MovieModel?, onClick: (MovieModel) -> Unit) {
    Box(Modifier.fillMaxWidth().height(600.dp).clickable { m?.let { onClick(it) } }) {
        Box(Modifier.fillMaxSize().background(Color(0xFF141414)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        Column(Modifier.align(Alignment.BottomStart).padding(60.dp)) {
            Text(m?.title?.uppercase() ?: "TV PLAYER+", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { m?.let { onClick(it) } }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(50.dp).width(200.dp)) {
                Text("REPRODUCIR", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun RowView(t: String, list: List<MovieModel>, onClick: (MovieModel) -> Unit) {
    if (list.isEmpty()) return
    Column(Modifier.padding(vertical = 20.dp)) {
        Text(t, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 60.dp, bottom = 15.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            items(list) { m ->
                Box(Modifier.width(200.dp).height(300.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F)).clickable { onClick(m) }) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))
                    Text(m.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun Details(m: MovieModel, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)).clickable { onDismiss() }, Alignment.Center) {
        Surface(Modifier.width(800.dp).fillMaxHeight(0.8f).clickable(enabled = false){}, color = Color(0xFF181818), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(450.dp).background(Color.Black)) {
                    Text("Tv Player+", Modifier.align(Alignment.Center), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                    Text("✕", Modifier.align(Alignment.TopEnd).padding(20.dp).clickable { onDismiss() }, color = Color.White, fontSize = 30.sp)
                }
                Column(Modifier.padding(40.dp)) {
                    Text(m.title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(15.dp))
                    Text(m.desc, color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(40.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                        Text("VER AHORA", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TopNav() {
    Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(0.8f), Color.Transparent))).padding(horizontal = 60.dp, vertical = 25.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Tv Player+", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
        Spacer(Modifier.width(50.dp))
        Text("INICIO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {})
        Spacer(Modifier.width(25.dp))
        Text("PELÍCULAS", color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.clickable {})
        Spacer(Modifier.width(25.dp))
        Text("SERIES", color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.clickable {})
    }
}

@Composable
fun LoginView(err: String?, onSub: (String) -> Unit) {
    var p by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.width(400.dp).background(Color(0xFF141414)).padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tv Player+", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFE50914))
            Spacer(Modifier.height(40.dp))
            if (err != null) Text(err, color = Color.Red, modifier = Modifier.padding(bottom = 15.dp))
            OutlinedTextField(p, {p=it}, label = {Text("Teléfono (+34...)")}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(25.dp))
            Button(onClick = {onSub(p)}, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CodeView(err: String?, onSub: (String) -> Unit) {
    var c by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.width(400.dp).background(Color(0xFF141414)).padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CÓDIGO SMS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(25.dp))
            if (err != null) Text(err, color = Color.Red)
            OutlinedTextField(c, {c=it}, label = {Text("Código")}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(25.dp))
            Button(onClick = {onSub(c)}, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("VERIFICAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PassView(err: String?, onSub: (String) -> Unit) {
    var p by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.width(400.dp).background(Color(0xFF141414)).padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CONTRASEÑA 2FA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(25.dp))
            if (err != null) Text(err, color = Color.Red)
            OutlinedTextField(p, {p=it}, label = {Text("Contraseña")}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(25.dp))
            Button(onClick = {onSub(p)}, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(4.dp)) {
                Text("ENTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}
